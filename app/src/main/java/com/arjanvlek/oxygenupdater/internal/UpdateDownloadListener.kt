package com.arjanvlek.oxygenupdater.internal

import com.arjanvlek.oxygenupdater.models.DownloadProgressData

interface UpdateDownloadListener {
    fun onInitialStatusUpdate()
    fun onDownloadStarted()
    fun onDownloadProgressUpdate(downloadProgressData: DownloadProgressData)
    fun onDownloadPaused(queued: Boolean, downloadProgressData: DownloadProgressData)
    fun onDownloadComplete()
    fun onDownloadCancelled()
    fun onDownloadError(isInternalError: Boolean, isStorageSpaceError: Boolean, isServerError: Boolean)
    fun onVerifyStarted()
    fun onVerifyError()
    fun onVerifyComplete(launchInstallation: Boolean)
}
