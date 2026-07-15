package com.smouldering_durtles.wk.activities

import android.os.Bundle
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.smouldering_durtles.wk.Constants
import com.smouldering_durtles.wk.R
import com.smouldering_durtles.wk.proxy.ViewProxy
import jp.wasabeef.glide.transformations.RoundedCornersTransformation

/**
 * Simple activity that only shows a big TextView. Shows the app's 'about' information.
 */
class AboutActivity : AbstractActivity(R.layout.activity_about, R.menu.generic_options_menu) {
    override fun onCreateLocal(savedInstanceState: Bundle?) {
        val document = ViewProxy(this, R.id.document)
        document.setTextHtml(Constants.ABOUT_DOCUMENT)
        document.setLinkMovementMethod()

        val imageViewGif = findViewById<ImageView>(R.id.imageViewGif)
        Glide.with(this)
            .asGif()
            .load(R.drawable.welcome) // replace with your actual gif resource
            .apply(
                RequestOptions.bitmapTransform(
                    RoundedCornersTransformation(
                        30,
                        0
                    )
                )
            ) // here is where the rounding is applied
            .into(imageViewGif)
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
