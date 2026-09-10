package com.dockeeper.app.ui.theme

import androidx.compose.ui.graphics.Color

/**
 * Paleta predefinida de colores para categorías. El índice se guarda en
 * CategoryEntity.colorTag. Pensada para identificar categorías de un vistazo
 * (médico, legal, finanzas, etc.).
 */
object CategoryColors {
    val palette: List<Color> = listOf(
        Color(0xFF3F51B5), // Índigo (por defecto)
        Color(0xFFE53935), // Rojo (médico)
        Color(0xFF1E88E5), // Azul (legal)
        Color(0xFF43A047), // Verde (finanzas)
        Color(0xFF8E24AA), // Púrpura
        Color(0xFFFB8C00), // Naranja
        Color(0xFF00897B), // Teal
        Color(0xFF6D4C41)  // Marrón
    )

    fun colorFor(tag: Int): Color = palette.getOrElse(tag) { palette.first() }

    val size: Int get() = palette.size
}
