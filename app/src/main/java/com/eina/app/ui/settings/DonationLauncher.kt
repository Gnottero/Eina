package com.eina.app.ui.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.graphics.toColorInt

// TODO: sostituire con l'URL Buy Me a Coffee definitivo (asset ancora non fornito).
const val DONATION_URL = "https://buymeacoffee.com/eina"

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
