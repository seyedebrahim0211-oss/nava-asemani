package ir.navaasemani.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import ir.navaasemani.app.R

// هویت بصری «نوای آسمانی»: شب لاجوردی، طلای محراب، فیروزه‌ای کاشی
val Night = Color(0xFF0B0F14)
val Surface1 = Color(0xFF141A22)
val Surface2 = Color(0xFF1C2430)
val Surface3 = Color(0xFF263040)
val Gold = Color(0xFFE3BC76)
val GoldDeep = Color(0xFFB8893F)
val Turquoise = Color(0xFF27D1C3)
val Cream = Color(0xFFF4F1E8)
val Muted = Color(0xFF9AA7B4)
val Faint = Color(0xFF5E6B78)
val Danger = Color(0xFFF29A8A)

val GoldGradient = Brush.linearGradient(listOf(Color(0xFFF3D9A4), Gold, GoldDeep))

/** جفت‌رنگ‌های کاور برای مداحانی که هنوز عکس ندارند */
val CoverPalettes: List<Pair<Color, Color>> = listOf(
    Color(0xFF7A2E3A) to Color(0xFF1A0E14),
    Color(0xFF2E4F7A) to Color(0xFF0D1520),
    Color(0xFF2E7A6B) to Color(0xFF0B1A17),
    Color(0xFF7A5A2E) to Color(0xFF1A130A),
    Color(0xFF5A2E7A) to Color(0xFF140D1C),
    Color(0xFF7A3E2E) to Color(0xFF1C0F0B),
    Color(0xFF2E6A7A) to Color(0xFF0A171B),
    Color(0xFF4A5A2E) to Color(0xFF12160B),
)

fun paletteFor(seed: String): Pair<Color, Color> {
    var h = 0
    for (c in seed) h = h * 31 + c.code
    return CoverPalettes[Math.floorMod(h, CoverPalettes.size)]
}

val Vazirmatn = FontFamily(
    Font(R.font.vazirmatn_regular, FontWeight.Normal),
    Font(R.font.vazirmatn_medium, FontWeight.Medium),
    Font(R.font.vazirmatn_bold, FontWeight.Bold),
    Font(R.font.vazirmatn_black, FontWeight.Black)
)

private fun TextStyle.v(): TextStyle = copy(fontFamily = Vazirmatn)

private val base = Typography()
private val NavaTypography = Typography(
    displayLarge = base.displayLarge.v(),
    displayMedium = base.displayMedium.v(),
    displaySmall = base.displaySmall.v(),
    headlineLarge = base.headlineLarge.v().copy(fontWeight = FontWeight.Black),
    headlineMedium = base.headlineMedium.v().copy(fontWeight = FontWeight.Black),
    headlineSmall = base.headlineSmall.v().copy(fontWeight = FontWeight.Bold),
    titleLarge = base.titleLarge.v().copy(fontWeight = FontWeight.Bold, fontSize = 21.sp),
    titleMedium = base.titleMedium.v().copy(fontWeight = FontWeight.Bold),
    titleSmall = base.titleSmall.v().copy(fontWeight = FontWeight.Medium),
    bodyLarge = base.bodyLarge.v(),
    bodyMedium = base.bodyMedium.v(),
    bodySmall = base.bodySmall.v(),
    labelLarge = base.labelLarge.v().copy(fontWeight = FontWeight.Bold),
    labelMedium = base.labelMedium.v(),
    labelSmall = base.labelSmall.v()
)

private val NavaColors = darkColorScheme(
    primary = Gold, onPrimary = Night,
    secondary = Turquoise, onSecondary = Night,
    tertiary = Turquoise,
    background = Night, onBackground = Cream,
    surface = Surface1, onSurface = Cream,
    surfaceVariant = Surface2, onSurfaceVariant = Muted,
    surfaceContainer = Surface1, surfaceContainerHigh = Surface2, surfaceContainerHighest = Surface3,
    outline = Color(0xFF34404D), outlineVariant = Color(0xFF242E3A),
    error = Danger
)

@Composable
fun NavaTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = NavaColors, typography = NavaTypography, content = content)
}
