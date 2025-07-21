package com.example.hectoclash

import android.app.Activity
import android.app.Application
import android.os.Bundle

object AppVisibilityListener : Application.ActivityLifecycleCallbacks {
    private var activityCount = 0
    private var isAppInForeground = false

    override fun onActivityStarted(activity: Activity) {
        activityCount++
        if (!isAppInForeground && activityCount > 0) {
            isAppInForeground = true
            MusicManager.resumeMusic()
        }
    }

    override fun onActivityStopped(activity: Activity) {
        activityCount--
        if (activityCount <= 0) {
            isAppInForeground = false
            MusicManager.pauseMusic()
        }
    }

    // Other lifecycle methods (not used, but must be implemented)
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {}
    override fun onActivityResumed(activity: Activity) {}
    override fun onActivityPaused(activity: Activity) {}
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    override fun onActivityDestroyed(activity: Activity) {}
}
