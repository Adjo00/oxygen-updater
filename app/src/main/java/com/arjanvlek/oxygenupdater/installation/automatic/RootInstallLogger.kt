package com.arjanvlek.oxygenupdater.installation.automatic

import android.app.job.JobParameters
import android.app.job.JobService
import com.arjanvlek.oxygenupdater.ApplicationData
import com.arjanvlek.oxygenupdater.internal.OxygenUpdaterException
import com.arjanvlek.oxygenupdater.internal.logger.Logger.logError
import com.arjanvlek.oxygenupdater.internal.server.NetworkException
import com.arjanvlek.oxygenupdater.settings.SettingsManager
import org.joda.time.DateTimeZone
import org.joda.time.LocalDateTime

class RootInstallLogger : JobService() {
    override fun onStartJob(params: JobParameters?): Boolean {
        if (params == null || application !is ApplicationData) {
            // Retrying wont fix this issue. This is a lost cause.
            return true
        }

        val applicationData = application as ApplicationData
        val serverConnector = applicationData.serverConnector
        val settingsManager = SettingsManager(applicationData)
        val deviceId = settingsManager.getPreference(SettingsManager.PROPERTY_DEVICE_ID, -1L)
        val updateMethodId = settingsManager.getPreference(SettingsManager.PROPERTY_UPDATE_METHOD_ID, -1L)

        if (deviceId == -1L || updateMethodId == -1L) {
            logError(TAG, OxygenUpdaterException("Failed to log update installation action: Device and / or update method not selected."))

            // Retrying wont fix this issue. This is a lost cause.
            return true
        }

        params.extras.apply {
            val status = InstallationStatus.valueOf(getString(DATA_STATUS, ""))
            val installationId = getString(DATA_INSTALL_ID, "<INVALID>")
            val startOSVersion = getString(DATA_START_OS, "<UNKNOWN>")
            val destinationOSVersion = getString(DATA_DESTINATION_OS, "<UNKNOWN>")
            val currentOsVersion = getString(DATA_CURR_OS, "<UNKNOWN>")
            val timestamp = LocalDateTime.now(DateTimeZone.forID("Europe/Amsterdam")).toString()
            val failureReason = getString(DATA_FAILURE_REASON, "")

            val installation = RootInstall(deviceId, updateMethodId, status, installationId, timestamp, startOSVersion, destinationOSVersion, currentOsVersion, failureReason)

            serverConnector!!.logRootInstall(installation) { result ->
                if (result == null) {
                    logError(TAG, NetworkException("Failed to log update installation action on server: No response from server"))
                    jobFinished(params, true)
                } else if (!result.success) {
                    logError(TAG, OxygenUpdaterException("Failed to log update installation action on server: " + result.errorMessage))
                    jobFinished(params, true)
                } else if (result.success
                    && installation.installationStatus == InstallationStatus.FAILED
                    || installation.installationStatus == InstallationStatus.FINISHED
                ) {
                    settingsManager.deletePreference(SettingsManager.PROPERTY_INSTALLATION_ID)
                    jobFinished(params, false)
                } else {
                    jobFinished(params, false)
                }
            }
        }

        return true
    }

    override fun onStopJob(params: JobParameters): Boolean {
        return true
    }

    companion object {
        const val DATA_STATUS = "STATUS"
        const val DATA_INSTALL_ID = "INSTALLATION_ID"
        const val DATA_START_OS = "START_OS"
        const val DATA_DESTINATION_OS = "DEST_OS"
        const val DATA_CURR_OS = "CURR_OS"
        const val DATA_FAILURE_REASON = "FAILURE_REASON"

        private const val TAG = "RootInstallLogger"
    }
}
