package pi.pocket.edition.util

import android.os.Build

object ArchDetector {
    fun getArch(): String {
        val abis = Build.SUPPORTED_ABIS
        return if (abis.any { it.contains("arm64") || it.contains("aarch64") }) {
            "arm64"
        } else {
            "arm32"
        }
    }

    fun is64Bit(): Boolean = getArch() == "arm64"

    fun getDefaultDownloadUrl(): String {
        return if (is64Bit()) {
            "https://github.com/DesktopECHO/Pi-hole-for-Android/releases/latest/download/raspbian.tgz"
        } else {
            "https://github.com/DesktopECHO/Pi-hole-for-Android/releases/latest/download/raspbian32.tgz"
        }
    }

    fun getArchLabel(): String {
        return if (is64Bit()) "ARM64 (64-bit)" else "ARM32 (32-bit)"
    }
}
