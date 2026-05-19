package pi.pocket.edition.root

import android.os.Build

object NetworkCommands {

    fun disableUsbTethering(): Boolean {
        val result = RootManager.exec("service call connectivity 33 i32 0")
        return result.isSuccess
    }

    fun enableUsbTethering(): Boolean {
        val result = RootManager.exec("service call connectivity 33 i32 1")
        if (!result.isSuccess) {
            // Fallback
            return RootManager.exec("setprop sys.usb.config rndis,adb").isSuccess
        }
        return true
    }

    fun disableEthTethering(): Boolean {
        if (Build.VERSION.SDK_INT < 30) return true
        val result = RootManager.exec("cmd tethering stop-tethering 5")
        return result.isSuccess
    }

    fun enableEthTethering(): Boolean {
        if (Build.VERSION.SDK_INT < 30) return false
        val result = RootManager.exec("cmd tethering start-tethering 5")
        return result.isSuccess
    }

    fun addIpAddress(ip: String): Boolean {
        val result = RootManager.exec("ip addr add $ip dev wlan0 2>/dev/null")
        return result.isSuccess
    }

    fun removeIpAddress(ip: String): Boolean {
        val result = RootManager.exec("ip addr del $ip dev wlan0 2>/dev/null")
        return result.isSuccess
    }

    /**
     * Pre-start safety check: disable all tethering before Pi-hole start
     * to prevent port 53 conflict
     */
    fun disableAllTethering(): Boolean {
        var success = true
        if (RootManager.isUsbTetheringActive()) {
            success = disableUsbTethering() && success
        }
        if (Build.VERSION.SDK_INT >= 30 && RootManager.isEthTetheringActive()) {
            success = disableEthTethering() && success
        }
        return success
    }
}
