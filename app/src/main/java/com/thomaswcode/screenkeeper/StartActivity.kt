package com.thomaswcode.screenkeeper

import android.app.Activity
import android.os.Bundle

// Invisible activity: starting the service from here counts as a foreground start
class StartActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        KeepAwakeService.start(this)
        finish()
    }
}
