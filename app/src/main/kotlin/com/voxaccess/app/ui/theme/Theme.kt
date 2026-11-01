package com.voxaccess.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val EsquemaColoresOscuro = darkColorScheme(
    primary = NaranjaVox,
    onPrimary = TextoOscuroSobreNaranja,
    secondary = VerdeVox,
    onSecondary = TextoBlanco,
    background = FondoOscuro,
    onBackground = TextoBlanco,
    surface = SuperficieOscura,
    onSurface = TextoBlanco,
    surfaceVariant = VarianteSuperficieOscura,
    onSurfaceVariant = TextoGris
)

@Composable
fun VoxAccessTheme(
    contenido: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = EsquemaColoresOscuro,
        content = contenido
    )
}
