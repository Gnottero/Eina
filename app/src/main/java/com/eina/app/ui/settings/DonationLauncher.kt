package com.eina.app.ui.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.graphics.toColorInt

// Ko-fi invece di Buy Me a Coffee: BMC accetta solo Stripe per i nuovi account,
// Ko-fi permette di incassare direttamente su PayPal.
const val DONATION_URL = "https://ko-fi.com/gnottero"

/**
 * Apre la pagina donazioni in una Custom Tab, cosi' la si legge senza uscire dall'app.
 * Se nessun browser supporta le Custom Tab si ripiega su ACTION_VIEW; se manca anche un
 * browser (device senza, o disabilitato) non si fa nulla di piu' che non far crashare l'app.
 */
fun launchDonationPage(context: Context) {
    val uri = Uri.parse(DONATION_URL)
    val customTabs = CustomTabsIntent.Builder()
        .setShowTitle(true)
        .setDefaultColorSchemeParams(
            androidx.browser.customtabs.CustomTabColorSchemeParams.Builder()
                .setToolbarColor("#FBF6F2".toColorInt())
                .build()
        )
        .build()

    try {
        customTabs.launchUrl(context, uri)
    } catch (e: ActivityNotFoundException) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri))
        } catch (e: ActivityNotFoundException) {
            // Nessun browser installato: non c'e' un fallback sensato.
        }
    }
}
