package com.eina.app.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.eina.app.R

/**
 * Marchio dell'app in Compose. Punta allo stesso vettoriale dell'icona di sistema e
 * dell'immagine condivisibile: il logo esiste in un file solo (res/drawable/ic_eina_logo.xml).
 */
@Composable
fun EinaLogo(modifier: Modifier = Modifier, size: Dp = 40.dp) {
    Image(
        painter = painterResource(R.drawable.ic_eina_logo),
        contentDescription = "Eina",
        modifier = modifier.size(size)
    )
}
