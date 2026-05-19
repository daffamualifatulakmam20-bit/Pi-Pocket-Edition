package pi.pocket.edition.root

import com.topjohnwu.superuser.Shell

object RootManager {
    fun isRooted(): Boolean = Shell.isAppGrantedRoot() == true

    fun exec(vararg commands: String): Shell.Result {
        return Shell.cmd(*commands).exec()
    }

    fun execOut(vararg commands: String): List<String> {
        return Shell.cmd(*commands).exec().out
    }

    fun isPort53InUse(): Boolean {
        val result = exec("ss -tlnp | grep ':53 '")
        return result.out.isNotEmpty()
    }

    fun isUsbTetheringActive(): Boolean {
        val result = exec("cat /sys/class/net/rndis0/operstate 2>/dev/null || cat /sys/class/net/usb0/operstate 2>/dev/null")
        return result.out.any { it.trim() == "up" }
    }

    fun isEthTetheringActive(): Boolean {
        val result = exec("ip link show eth0 2>/dev/null | grep 'state UP'")
        return result.out.isNotEmpty()
    }
}
