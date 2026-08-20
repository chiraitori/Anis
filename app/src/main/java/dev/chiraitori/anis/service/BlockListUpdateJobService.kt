package dev.chiraitori.anis.service

import android.Manifest
import android.content.pm.PackageManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import dev.chiraitori.anis.data.BlockListRepository
import dev.chiraitori.anis.data.SettingsRepository
import dev.chiraitori.anis.data.model.AutoUpdateFrequency
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BlockListUpdateJobService : JobService() {

    private val jobScope = CoroutineScope(Dispatchers.IO)

    override fun onStartJob(params: JobParameters?): Boolean {
        jobScope.launch {
            try {
                Log.i(TAG, "Executing scheduled blocklist auto-update")
                val blockListRepo = BlockListRepository.getInstance(applicationContext)
                val settingsRepo = SettingsRepository.getInstance(applicationContext)

                blockListRepo.updateAllLists()

                if (settingsRepo.autoUpdateNotificationFlow.value) {
                    showUpdateCompletedNotification(blockListRepo.getActiveBlockedDomains().size)
                }

                jobFinished(params, false)
            } catch (e: Exception) {
                Log.e(TAG, "Error in auto-update job", e)
                jobFinished(params, true)
            }
        }
        return true
    }

    override fun onStopJob(params: JobParameters?): Boolean {
        return true
    }

    private fun showUpdateCompletedNotification(rulesCount: Int) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        val manager = getSystemService(NotificationManager::class.java) ?: return
        val channelId = "anis_updates_channel"

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Blocklist Updates",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Notifications for automated blocklist rule updates"
            }
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setContentTitle("Anis Blocklists Updated")
            .setContentText("Active rules refreshed ($rulesCount domains loaded)")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setAutoCancel(true)
            .build()

        manager.notify(UPDATE_NOTIFICATION_ID, notification)
    }

    companion object {
        private const val TAG = "BlockListUpdateJob"
        private const val UPDATE_NOTIFICATION_ID = 3001
    }
}

object BlockListUpdateScheduler {

    private const val JOB_ID = 9001
    private const val TAG = "BlockListScheduler"

    fun schedule(context: Context, frequency: AutoUpdateFrequency, wifiOnly: Boolean) {
        val scheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as? JobScheduler ?: return

        if (frequency == AutoUpdateFrequency.MANUAL) {
            scheduler.cancel(JOB_ID)
            Log.i(TAG, "Auto-update cancelled (Manual only)")
            return
        }

        val intervalMs = when (frequency) {
            AutoUpdateFrequency.DAILY -> 24L * 60 * 60 * 1000L
            AutoUpdateFrequency.THREE_DAYS -> 3L * 24 * 60 * 60 * 1000L
            AutoUpdateFrequency.WEEKLY -> 7L * 24 * 60 * 60 * 1000L
            AutoUpdateFrequency.MANUAL -> return
        }

        val component = ComponentName(context, BlockListUpdateJobService::class.java)
        val builder = JobInfo.Builder(JOB_ID, component)
            .setPeriodic(intervalMs)
            .setPersisted(true)

        if (wifiOnly) {
            builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED)
        } else {
            builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
        }

        val result = scheduler.schedule(builder.build())
        if (result == JobScheduler.RESULT_SUCCESS) {
            Log.i(TAG, "Scheduled blocklist auto-update: ${frequency.title} (wifiOnly=$wifiOnly)")
        } else {
            Log.e(TAG, "Failed scheduling blocklist auto-update job")
        }
    }
}
