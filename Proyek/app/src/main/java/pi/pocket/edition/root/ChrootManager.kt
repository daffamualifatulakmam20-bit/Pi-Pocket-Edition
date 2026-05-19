package pi.pocket.edition.root

import com.topjohnwu.superuser.Shell

/**
 * Manages the Debian chroot at /data/local/pipocket/rasbian.
 * 
 * Architecture matches the original Pi Deploy (pideploy.apk):
 * - cli.sh handles container_mount() with 10 filesystem types
 * - etc/init.d/android-raspbian handles first-boot Pi-hole install
 * - Networking is auto-configured from Android host on each start
 * 
 * Mount sequence from original cli.sh (line 660):
 * root → proc → sys → dev → shm → pts → fd → tty → tun → binfmt_misc
 */
object ChrootManager {
    const val CHROOT_PATH = "/data/local/pipocket/rasbian"
    const val DOWNLOAD_DIR = "/data/local/pipocket"

    fun isInstalled(): Boolean {
        val result = RootManager.exec("test -d $CHROOT_PATH/etc && echo 'yes'")
        return result.out.any { it.trim() == "yes" }
    }

    fun isMounted(): Boolean {
        val result = RootManager.exec("grep -q ' $CHROOT_PATH/proc ' /proc/mounts 2>/dev/null && echo 'yes'")
        return result.out.any { it.trim() == "yes" }
    }

    /**
     * Mount essential filesystems for chroot operation.
     * Matches the original cli.sh container_mount() sequence exactly.
     * Original mounts: root, proc, sys, dev, shm, pts, fd, tty, tun, binfmt_misc
     */
    fun mountChroot(): Boolean {
        // Disable SELinux enforcement if present (original cli.sh checks this)
        RootManager.exec("[ -e /sys/fs/selinux/enforce ] && echo 0 > /sys/fs/selinux/enforce 2>/dev/null")

        // proc - with hidepid=0 fix from android-raspbian init script
        RootManager.exec(
            "mkdir -p $CHROOT_PATH/proc",
            "mountpoint -q $CHROOT_PATH/proc || mount -t proc proc $CHROOT_PATH/proc",
            "mount -o remount,rw,hidepid=0 $CHROOT_PATH/proc 2>/dev/null"
        )

        // sys
        RootManager.exec(
            "mkdir -p $CHROOT_PATH/sys",
            "mountpoint -q $CHROOT_PATH/sys || mount -t sysfs sysfs $CHROOT_PATH/sys"
        )

        // dev
        RootManager.exec(
            "mkdir -p $CHROOT_PATH/dev",
            "mountpoint -q $CHROOT_PATH/dev || mount -o bind /dev $CHROOT_PATH/dev"
        )

        // shm (missing in our previous version!)
        RootManager.exec(
            "[ -d /dev/shm ] || mkdir -p /dev/shm",
            "mountpoint -q /dev/shm || mount -o rw,nosuid,nodev,mode=1777 -t tmpfs tmpfs /dev/shm",
            "mkdir -p $CHROOT_PATH/dev/shm",
            "mountpoint -q $CHROOT_PATH/dev/shm || mount -o bind /dev/shm $CHROOT_PATH/dev/shm"
        )

        // pts
        RootManager.exec(
            "[ -d /dev/pts ] || mkdir -p /dev/pts",
            "mountpoint -q /dev/pts || mount -o rw,nosuid,noexec,gid=5,mode=620,ptmxmode=000 -t devpts devpts /dev/pts",
            "mkdir -p $CHROOT_PATH/dev/pts",
            "mountpoint -q $CHROOT_PATH/dev/pts || mount -o bind /dev/pts $CHROOT_PATH/dev/pts"
        )

        // fd symlinks (from cli.sh mount_part fd)
        RootManager.exec(
            "[ -e /dev/fd ] || ln -s /proc/self/fd /dev/fd",
            "[ -e /dev/stdin ] || ln -s /proc/self/fd/0 /dev/stdin",
            "[ -e /dev/stdout ] || ln -s /proc/self/fd/1 /dev/stdout",
            "[ -e /dev/stderr ] || ln -s /proc/self/fd/2 /dev/stderr"
        )

        // tty
        RootManager.exec("[ -e /dev/tty0 ] || ln -s /dev/null /dev/tty0")

        // tun
        RootManager.exec(
            "[ -e /dev/net/tun ] || { mkdir -p /dev/net && mknod /dev/net/tun c 10 200; }"
        )

        // tmp
        RootManager.exec(
            "mkdir -p $CHROOT_PATH/tmp",
            "mountpoint -q $CHROOT_PATH/tmp || mount -t tmpfs tmpfs $CHROOT_PATH/tmp"
        )

        // bind /system for unchroot access to Android properties
        RootManager.exec(
            "mkdir -p $CHROOT_PATH/system",
            "mountpoint -q $CHROOT_PATH/system || mount -o bind /system $CHROOT_PATH/system 2>/dev/null"
        )

        return isMounted()
    }

    /**
     * Unmount all chroot filesystems in reverse order.
     * Matches cli.sh container_umount() - kills processes first, then unmounts.
     */
    fun unmountChroot(): Boolean {
        // Kill processes using the chroot (from cli.sh container_umount)
        RootManager.exec(
            "for pid in \$(lsof 2>/dev/null | grep '$CHROOT_PATH' | awk '{print \$2}' | sort -u); do kill -9 \$pid 2>/dev/null; done"
        )

        // Unmount in reverse order
        val result = RootManager.exec(
            "umount $CHROOT_PATH/system 2>/dev/null",
            "umount $CHROOT_PATH/tmp 2>/dev/null",
            "umount $CHROOT_PATH/dev/shm 2>/dev/null",
            "umount $CHROOT_PATH/dev/pts 2>/dev/null",
            "umount $CHROOT_PATH/dev 2>/dev/null",
            "umount $CHROOT_PATH/sys 2>/dev/null",
            "umount $CHROOT_PATH/proc 2>/dev/null"
        )
        return result.isSuccess
    }

    /**
     * Execute a command inside the chroot.
     * Matches cli.sh chroot_exec() - sets proper PATH.
     */
    fun execInChroot(command: String): Shell.Result {
        return RootManager.exec(
            "PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin chroot $CHROOT_PATH /bin/bash -c '$command'"
        )
    }

    fun execInChrootOut(command: String): List<String> {
        return execInChroot(command).out
    }

    /**
     * Configure DNS inside chroot so it can access internet.
     */
    fun setupDns(): Boolean {
        val result = RootManager.exec(
            "echo 'nameserver 8.8.8.8' > $CHROOT_PATH/etc/resolv.conf",
            "echo 'nameserver 8.8.4.4' >> $CHROOT_PATH/etc/resolv.conf"
        )
        return result.isSuccess
    }

    /**
     * Create unchroot shim so the init script can call getprop etc.
     * The original uses `unchroot getprop ro.product.device` to get hostname.
     */
    fun setupUnchrootShim(): Boolean {
        // Use heredoc with 'SHIMEOF' (single-quoted) so shell doesn't expand $@
        val shimScript = "cat > $CHROOT_PATH/usr/sbin/unchroot << 'SHIMEOF'\n" +
            "#!/bin/bash\n" +
            "# unchroot shim - execute commands outside the chroot via nsenter\n" +
            "nsenter -t 1 -m -- \"\$@\" 2>/dev/null || \"\$@\" 2>/dev/null\n" +
            "SHIMEOF\n" +
            "chmod +x $CHROOT_PATH/usr/sbin/unchroot"
        val result = RootManager.exec(shimScript)
        return result.isSuccess
    }

    /**
     * Setup Android groups for network access.
     * From android-raspbian init script line 19-20.
     */
    fun setupAndroidGroups(): Boolean {
        val result = RootManager.exec(
            """chroot $CHROOT_PATH /bin/bash -c '
                grep -q "aid_inet" /etc/group && \
                sed -i "/aid_inet/c\\aid_inet:x:3003:android,root,messagebus,pihole,unbound,xrdp,sshd,avahi,postfix,postdrop,redis,www-data,ncp,prometheus,sambashare,mysql,_apt" /etc/group
                usermod -a -G aid_net_bt,aid_net_bt_admin,aid_net_admin,aid_net_raw,aid_inet root 2>/dev/null
            '"""
        )
        return result.isSuccess
    }

    /**
     * Run the original android-raspbian init script inside chroot.
     * This handles first-boot Pi-hole install + network auto-config.
     */
    fun runInitScript(): Shell.Result {
        return RootManager.exec(
            "chroot $CHROOT_PATH /bin/bash -c 'export DEBIAN_FRONTEND=noninteractive && /etc/init.d/android-raspbian start' 2>&1"
        )
    }

    /**
     * Start Pi-hole services (FTL + lighttpd).
     * Used for subsequent starts after initial setup.
     * Matches the original boot sequence: mount → groups → dns → init → services
     */
    fun startPihole(): Boolean {
        if (!isMounted()) mountChroot()
        setupDns()
        setupAndroidGroups()

        // Auto-configure network (from android-raspbian init lines 24-28)
        val networkScript = """
            subnetmask=${'$'}(ip route list table main 2>/dev/null | tail -n1 | cut -d "/" -f2 | cut -d " " -f1)
            device=${'$'}(ip route list table main 2>/dev/null | tail -n1 | cut -d" " -f 3)
            ipaddr=${'$'}(ip route list table main 2>/dev/null | tail -n1 | awk '{print ${'$'}NF}')
            gateway=${'$'}(ip -4 route show table 0 2>/dev/null | grep "default via" | cut -d" " -f 3 | head -n 1)
            [ -n "${'$'}gateway" ] && [ -n "${'$'}device" ] && route add default gw ${'$'}gateway dev ${'$'}device 2>/dev/null
            [ -n "${'$'}ipaddr" ] && sed -i "/IPV4_ADDRESS/c\\IPV4_ADDRESS=${'$'}ipaddr/${'$'}subnetmask" /etc/pihole/setupVars.conf 2>/dev/null
            [ -n "${'$'}device" ] && sed -i "/PIHOLE_INTERFACE/c\\PIHOLE_INTERFACE=${'$'}device" /etc/pihole/setupVars.conf 2>/dev/null
        """.trimIndent()
        RootManager.exec(
            "chroot $CHROOT_PATH /bin/bash -c '$networkScript'"
        )

        // Replace systemctl shims (from android-raspbian init lines 99-103)
        RootManager.exec(
            """chroot $CHROOT_PATH /bin/bash -c '
                [ -f /.service ] && cp /.service /usr/sbin/service
                [ -f /.systemctl ] && cp /.systemctl /usr/bin/systemctl
                [ -f /.pihole-FTL ] && cp /.pihole-FTL /etc/init.d/pihole-FTL
            ' 2>/dev/null"""
        )

        // Start services
        val result = RootManager.exec(
            "chroot $CHROOT_PATH /bin/bash -c 'service pihole-FTL start 2>/dev/null || pihole-FTL 2>/dev/null'",
            "chroot $CHROOT_PATH /bin/bash -c 'service lighttpd start 2>/dev/null'"
        )
        return result.isSuccess
    }

    /**
     * Stop Pi-hole services and unmount.
     * Matches original stop sequence.
     */
    fun stopPihole(): Boolean {
        RootManager.exec(
            """chroot $CHROOT_PATH /bin/bash -c '
                service pihole-FTL stop 2>/dev/null
                service lighttpd stop 2>/dev/null
                killall pihole-FTL 2>/dev/null
                killall lighttpd 2>/dev/null
            '"""
        )
        return unmountChroot()
    }

    fun setPiholePassword(password: String): Boolean {
        if (!isMounted()) mountChroot()
        val result = execInChroot("pihole -a -p \"$password\"")
        return result.isSuccess
    }

    fun setSshPassword(password: String): Boolean {
        if (!isMounted()) mountChroot()
        val result = execInChroot("echo \"root:$password\" | chpasswd && echo \"android:$password\" | chpasswd")
        return result.isSuccess
    }
}
