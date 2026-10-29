package ir.navaasemani.app.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.Typography
import androidx.compose.ui.unit.sp

val Ink = Color(0xFF08131F)
val Panel = Color(0xFF142B3B)
val PanelLight = Color(0xFF213F4D)
val Jade = Color(0xFF27D1C3)
val Gold = Color(0xFFE3BC76)
val Cream = Color(0xFFF4F1E8)
val Muted = Color(0xFFA6B7C5)

private val palette = darkColorScheme(
    primary = Jade, onPrimary = Ink, secondary = Gold, onSecondary = Ink,
    background = Ink, onBackground = Cream,
    surface = Panel, onSurface = Cream,
    surfaceVariant = PanelLight, onSurfaceVariant = Muted,
    outline = Color(0xFF38505B), error = Color(0xFFF29A8A)
)

@Composable fun NavaTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = palette, typography = Typography(
        titleLarge = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.SansSerif),
        titleMedium = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.SansSerif),
        bodyLarge = TextStyle(fontSize = 16.sp, fontFamily = FontFamily.SansSerif),
        bodyMedium = TextStyle(fontSize = 14.sp, fontFamily = FontFamily.SansSerif),
        labelMedium = TextStyle(fontSize = 12.sp, fontFamily = FontFamily.SansSerif)
    ), content = content)
}
