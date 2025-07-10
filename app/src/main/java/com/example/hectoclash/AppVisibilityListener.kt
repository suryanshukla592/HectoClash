package com.example.hectoclash

import android.app.Activity
import android.app.Application
import android.os.Bundle

object AppVisibilityListener : Application.ActivityLifecycleCallbacks {
    private var activityCount = 0

    override fun onActivityStarted(activity: Activity) {
        activityCount++
        if (activityCount == 1) {
            // App comes to foreground
            MusicManager.resumeMusic()
        }
    }

    override fun onActivityStopped(activity: Activity) {
        activityCount--
        if (activityCount == 0) {
            // App goes to background
            MusicManager.pauseMusic()
        }
    }

    // Unused but required to implement
    override fun onActivityCreated(a: Activity, b: Bundle?) {}
    override fun onActivityResumed(a: Activity) {}
    override fun onActivityPaused(a: Activity) {}
    override fun onActivitySaveInstanceState(a: Activity, b: Bundle) {}
    override fun onActivityDestroyed(a: Activity) {}
}
