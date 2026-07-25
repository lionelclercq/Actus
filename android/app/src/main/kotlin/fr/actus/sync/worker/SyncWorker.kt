package fr.actus.sync.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import fr.actus.sync.R
import fr.actus.sync.data.CookieRepositoryHolder
import fr.actus.sync.data.SettingsRepository
import fr.actus.sync.sync.SyncRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class SyncWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        CookieRepositoryHolder.init(applicationContext)
        val settings = SettingsRepository(applicationContext)
        val sync = SyncRepository(settings)
        try {
            updateForeground("Démarrage…")
            sync.run { progress ->
                when (progress.phase) {
                    SyncRepository.SyncProgress.Phase.FETCH,
                    SyncRepository.SyncProgress.Phase.ENRICH,
                    SyncRepository.SyncProgress.Phase.ANALYZE,
                    SyncRepository.SyncProgress.Phase.WIKI,
                    SyncRepository.SyncProgress.Phase.PUSH,
                    -> updateForeground(progress.message)
                    SyncRepository.SyncProgress.Phase.DONE -> Unit
                    SyncRepository.SyncProgress.Phase.ERROR -> Unit
                }
            }
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "Sync failed", e)
            val detail = e.message?.take(500).orEmpty()
            settings.lastSyncMessage = "❌ $detail"
            Result.failure()
        }
    }

    private suspend fun updateForeground(text: String) {
        try {
            setForeground(createForegroundInfo(text))
        } catch (e: Exception) {
            Log.w(TAG, "Notification de synchronisation indisponible: ${e.message}")
        }
    }

    private fun createForegroundInfo(text: String): ForegroundInfo {
        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                applicationContext.getString(R.string.notification_channel),
                NotificationManager.IMPORTANCE_LOW,
            )
            manager.createNotificationChannel(channel)
        }
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher)
            .setContentTitle(applicationContext.getString(R.string.notification_sync_title))
            .setContentText(text)
            .setOngoing(true)
            .setSilent(true)
            .build()
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ForegroundInfo(
                NOTIFICATION_ID,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC,
            )
        } else {
            ForegroundInfo(NOTIFICATION_ID, notification)
        }
    }

    companion object {
        const val WORK_NAME = "actus_sync"
        private const val TAG = "SyncWorker"
        private const val CHANNEL_ID = "actus_sync"
        private const val NOTIFICATION_ID = 42
    }
}
