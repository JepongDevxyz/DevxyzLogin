package com.devxyz.login

import android.app.Activity
import android.content.Intent
import android.os.Bundle

/**
 * Branded launch entry point. The activity theme
 * (@style/Theme.DevxyzLogin.Splash) paints the branded splash — white
 * logo mark centered on a green gradient — as the window background
 * from the very first frame, so there is no white flash. We hand off
 * to MainActivity immediately (first frame), then finish so Back never
 * returns here.
 */
class SplashActivity : Activity() {

    @Suppress("DEPRECATION") // overridePendingTransition(0,0): no-anim handoff
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        startActivity(Intent(this, MainActivity::class.java))
        finish()
        overridePendingTransition(0, 0)
    }
}
