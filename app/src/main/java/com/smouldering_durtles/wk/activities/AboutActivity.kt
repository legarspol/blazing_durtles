package com.smouldering_durtles.wk.activities

import android.os.Bundle
import com.smouldering_durtles.wk.Constants
import com.smouldering_durtles.wk.R
import com.smouldering_durtles.wk.proxy.ViewProxy

/**
 * Simple activity that only shows a big TextView. Shows the app's 'about' information.
 */
class AboutActivity : AbstractActivity(R.layout.activity_about, R.menu.generic_options_menu) {
    override fun onCreateLocal(savedInstanceState: Bundle?) {
        val document = ViewProxy(this, R.id.document)
        document.setTextHtml(Constants.ABOUT_DOCUMENT)
        document.setLinkMovementMethod()
    }

    override fun onResumeLocal() {
        //
    }

    override fun onPauseLocal() {
        //
    }

    override fun enableInteractionLocal() {
        //
    }

    override fun disableInteractionLocal() {
        //
    }

    override fun showWithoutApiKey(): Boolean {
        return true
    }
}
