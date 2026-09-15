package com.example.workers

import android.content.Context
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

object OverdueWorkScheduler {

    private const val PERIODIC_WORK_TAG = "periodic_overdue_check_work"
    private const val ONE_TIME_WORK_TAG = "immediate_overdue_check_work"

    /**
     * Schedule a periodic daily check for overdue members.
     */
    fun schedulePeriodicOverdueCheck(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiresBatteryNotLow(false)
            .build()

        val periodicWorkRequest = PeriodicWorkRequestBuilder<OverdueCheckWorker>(
            24, TimeUnit.HOURS,
            1, TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .addTag(PERIODIC_WORK_TAG)
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            PERIODIC_WORK_TAG,
            ExistingPeriodicWorkPolicy.KEEP,
            periodicWorkRequest
        )
    }

    /**
     * Trigger an immediate check now (for manual refresh / testing).
     */
    fun triggerImmediateCheck(context: Context) {
        val oneTimeRequest = OneTimeWorkRequestBuilder<OverdueCheckWorker>()
            .addTag(ONE_TIME_WORK_TAG)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            ONE_TIME_WORK_TAG,
            ExistingWorkPolicy.REPLACE,
            oneTimeRequest
        )
    }

    /**
     * Cancel periodic worker if user disables automatic checks.
     */
    fun cancelPeriodicCheck(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(PERIODIC_WORK_TAG)
    }
}
