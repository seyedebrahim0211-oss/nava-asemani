package ir.navaasemani.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddCircle
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.SystemUpdate
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import ir.navaasemani.app.BuildConfig
import ir.navaasemani.app.ui.GoldButton
import ir.navaasemani.app.ui.LocalActions
import ir.navaasemani.app.ui.LocalSnapshot
import ir.navaasemani.app.ui.MihrabPattern
import ir.navaasemani.app.ui.Pill
import ir.navaasemani.app.ui.theme.Gold
import ir.navaasemani.app.ui.theme.GoldGradient
import ir.navaasemani.app.ui.theme.Muted
import ir.navaasemani.app.ui.theme.Night
import ir.navaasemani.app.ui.theme.Surface1

@Composable
fun AboutScreen(contentPadding: PaddingValues, onBack: () -> Unit, onRefresh: () -> Unit) {
    val snap = LocalSnapshot.current
    val actions = LocalActions.current
    val repo = BuildConfig.REPO_URL
    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).statusBarsPadding()
                .padding(start = 24.dp, end = 24.dp, top = 56.dp, bottom = contentPadding.calculateBottomPadding() + 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Box(Modifier.size(110.dp).clip(RoundedCornerShape(30.dp)).background(GoldGradient), contentAlignment = Alignment.Center) {
                MihrabPattern(Modifier.fillMaxSize().padding(10.dp), color = Night, alpha = 0.85f)
            }
            Text("نوای آسمانی", style = MaterialTheme.typography.headlineLarge)
            Text("«هر نوا، راهی به نور»", color = Gold, style = MaterialTheme.typography.titleMedium)
            Text("نسخه ${BuildConfig.VERSION_NAME}", color = Muted, style = MaterialTheme.typography.labelMedium)
            Text(
                "آرشیو زنده مداحی مداحان ایران: ${snap.catalog.tracks.size} اثر از ${snap.catalog.maddahs.count { !it.hidden }} مداح." +
                    (if (snap.catalog.updated.isNotBlank()) "\nآخرین به‌روزرسانی آرشیو: ${snap.catalog.updated}" else ""),
                textAlign = TextAlign.Center, style = MaterialTheme.typography.bodyMedium
            )
            GoldButton(if (snap.refreshing) "در حال به‌روزرسانی…" else "به‌روزرسانی آرشیو", Icons.Rounded.Refresh, onClick = onRefresh)
            if (repo.isNotBlank()) {
                Pill("دریافت آخرین نسخه اپ", selected = false, icon = Icons.Rounded.SystemUpdate,
                    onClick = { actions.openUrl("$repo/releases/latest") })
                Pill("پیشنهاد مداح یا اثر تازه", selected = false, icon = Icons.Rounded.AddCircle,
                    onClick = { actions.openUrl("$repo/issues/new") })
            }
            Spacer(Modifier.height(4.dp))
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Surface1).padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("حقوق صاحبان اثر", style = MaterialTheme.typography.titleMedium, color = Gold)
                Text(
                    "این اپ فایل صوتی را بازنشر نمی‌کند؛ پخش و دانلود فقط از لینک مستقیمی انجام می‌شود که خود مداح یا ناشر رسمی منتشر کرده است. " +
                        "برای آثاری که لینک مستقیم ندارند، صفحه رسمی ناشر باز می‌شود. نام منبع و مجوز هر اثر در پخش‌کننده نمایش داده می‌شود. " +
                        "صاحبان اثر برای اصلاح یا حذف می‌توانند از بخش «پیشنهاد» در گیت‌هاب پیام دهند.",
                    style = MaterialTheme.typography.bodySmall, color = Muted
                )
                Text("قلم: وزیرمتن (مجوز OFL) · کدنویسی Kotlin و Jetpack Compose · پخش با Media3", style = MaterialTheme.typography.labelSmall, color = Muted)
            }
        }
        BackButton(onBack)
    }
}
