package com.dxnd.viper4android

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.util.Property
import com.dxnd.viper4android.utils.FileLogger
import dagger.hilt.android.HiltAndroidApp
import java.lang.reflect.Method

const val SERVICE_CHANNEL_ID = "viper4android_service"
const val BULK_OP_CHANNEL_ID = "viper4android_bulk_op"

@HiltAndroidApp
class ViPER4AndroidApp : Application() {
    override fun onCreate() {
        super.onCreate()
        createNotificationChannels()
        FileLogger.init(this)
    }

    private fun createNotificationChannels() {
        val nm = getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(
            NotificationChannel(
                SERVICE_CHANNEL_ID,
                getString(R.string.notification_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = getString(R.string.notification_channel_description)
                setShowBadge(false)
            },
        )
        nm.createNotificationChannel(
            NotificationChannel(
                BULK_OP_CHANNEL_ID,
                getString(R.string.notification_bulk_op_channel_name),
                NotificationManager.IMPORTANCE_LOW,
            ).apply {
                description = getString(R.string.notification_bulk_op_channel_description)
                setShowBadge(false)
            },
        )
    }

    companion object {
        init {
            try {
                val methods =
                    Property
                        .of(
                            Class::class.java,
                            Array<Method>::class.java,
                            "Methods",
                        ).get(Class.forName("dalvik.system.VMRuntime"))
                var runtime: Any? = null
                var exemptionMethod: Method? = null
                for (method in methods) {
                    when (method.name) {
                        "getRuntime" -> runtime = method.invoke(null)
                        "setHiddenApiExemptions" -> exemptionMethod = method
                    }
                }
                if (runtime != null && exemptionMethod != null) {
                    exemptionMethod.invoke(runtime, arrayOf("L"))
                    FileLogger.d("App", "unseal: success")
                } else {
                    FileLogger.e(
                        "App",
                        "unseal: methods not found (runtime=$runtime, exemption=$exemptionMethod)",
                    )
                }
            } catch (e: Exception) {
                FileLogger.e("App", "unseal: failed", e)
            }
        }
    }
}
