package pi.pocket.edition

import android.app.Application
import com.topjohnwu.superuser.Shell

class PiPocketApp : Application() {

    companion object {
        init {
            Shell.enableVerboseLogging = false
            Shell.setDefaultBuilder(
                Shell.Builder.create()
                    .setFlags(Shell.FLAG_MOUNT_MASTER or Shell.FLAG_REDIRECT_STDERR)
                    .setTimeout(30)
            )
        }
    }

    override fun onCreate() {
        super.onCreate()
    }
}
