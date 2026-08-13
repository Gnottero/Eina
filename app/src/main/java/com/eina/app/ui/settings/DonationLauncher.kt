package com.eina.app.ui.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.graphics.toColorInt

// Ko-fi instead of Buy Me a Coffee: BMC only accepts Stripe for new accounts, while Ko-fi pays out
// directly to PayPal.
const val DONATION_URL = "https://ko-fi.com/gnottero"

/**
 * Opens the donation page in a Custom Tab, so it is read without leaving the app. If no browser
 * supports Custom Tabs it falls back to ACTION_VIEW; with no browser at all it does nothing beyond
 * not crashing.
 */
fun launchDonationPage(context: Context) {
    val uri = Uri.parse(DONATION_URL)
    val customTabs = CustomTabsIntent.Builder()
        .setShowTitle(true)
        .setDefaultColorSchemeParams(
            androidx.browser.customtabs.CustomTabColorSchemeParams.Builder()
                .setToolbarColor("#F4F3F1".toColorInt())
                .build()
        )
        .build()

    try {
        customTabs.launchUrl(context, uri)
    } catch (e: ActivityNotFoundException) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (e: ActivityNotFoundException) {
            // No browser installed: there is no sensible fallback.
        }
    }
}
