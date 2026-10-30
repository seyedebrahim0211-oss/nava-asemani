package ir.navaasemani.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.SearchOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import ir.navaasemani.app.data.Persian
import ir.navaasemani.app.ui.EmptyState
import ir.navaasemani.app.ui.LocalActions
import ir.navaasemani.app.ui.LocalSnapshot
import ir.navaasemani.app.ui.MaddahAvatar
import ir.navaasemani.app.ui.MihrabPattern
import ir.navaasemani.app.ui.Pill
import ir.navaasemani.app.ui.SectionHeader
import ir.navaasemani.app.ui.TrackRow
import ir.navaasemani.app.ui.theme.Cream
import ir.navaasemani.app.ui.theme.Gold
import ir.navaasemani.app.ui.theme.Muted
import ir.navaasemani.app.ui.theme.Surface2
import ir.navaasemani.app.ui.theme.paletteFor

@Composable
fun SearchScreen(
    contentPadding: PaddingValues,
    initialCategory: String?,
    onSubmit: (String) -> Unit,
    onClearHistory: () -> Unit
) {
    val snap = LocalSnapshot.current
    val actions = LocalActions.current
    val focus = LocalFocusManager.current
    val catalog = snap.catalog
    var query by rememberSaveable { mutableStateOf("") }
    var category by rememberSaveable { mutableStateOf<String?>(initialCategory) }
    var occasion by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(initialCategory) { if (initialCategory != null) category = initialCategory }

    val normalized = Persian.normalize(query)
    val words = normalized.split(' ').filter { it.isNotBlank() }
    val occasions = remember(catalog) { catalog.tracks.map { it.occasion }.filter { it.isNotBlank() }.distinct() }
    val searching = words.isNotEmpty() || category != null || occasion != null

    val maddahMatches = if (words.isEmpty()) emptyList() else catalog.maddahs.filter { m ->
        !m.hidden && Persian.normalize(m.displayName + " " + m.city).let { hay -> words.all { hay.contains(it) } }
    }
    val trackMatches = if (!searching) emptyList() else catalog.tracks.filter { t ->
        (category == null || t.category == category) &&
            (occasion == null || t.occasion.contains(occasion!!)) &&
            (words.isEmpty() || Persian.normalize(
                listOf(t.title, catalog.maddah(t.maddahId)?.displayName.orEmpty(), t.featuring, t.occasion, t.category, t.tags, t.year)
                    .joinToString(" ")
            ).let { hay -> words.all { hay.contains(it) } })
    }

    LazyColumn(Modifier.fillMaxSize(), contentPadding = contentPadding) {
        item {
            Column(Modifier.statusBarsPadding().padding(start = 20.dp, end = 20.dp, top = 14.dp)) {
                Text("جست‌وجو", style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(12.dp))
                TextField(
                    value = query, onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("نام مداح، عنوان، مناسبت…") },
                    leadingIcon = { Icon(Icons.Rounded.Search, null) },
                    trailingIcon = {
                        if (query.isNotEmpty()) IconButton(onClick = { query = "" }) { Icon(Icons.Rounded.Close, "پاک‌کردن") }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { onSubmit(query); focus.clearFocus() }),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Surface2, unfocusedContainerColor = Surface2,
                        focusedIndicatorColor = Color.Transparent, unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = Gold
                    )
                )
            }
        }
        item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item { Pill("همه", selected = category == null, onClick = { category = null }) }
                items(catalog.categories) { c -> Pill(c, selected = category == c, onClick = { category = if (category == c) null else c }) }
            }
        }
        if (occasions.isNotEmpty()) item {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(occasions) { o -> Pill(o, selected = occasion == o, onClick = { occasion = if (occasion == o) null else o }) }
            }
        }

        if (!searching) {
            if (snap.library.searches.isNotEmpty()) {
                item { SectionHeader("جست‌وجوهای اخیر", action = "پاک‌کردن", onAction = onClearHistory) }
                items(snap.library.searches) { s ->
                    Row(
                        Modifier.fillMaxWidth().clickable { query = s }.padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Rounded.History, null, tint = Muted)
                        Spacer(Modifier.width(12.dp))
                        Text(s)
                    }
                }
            }
            item { SectionHeader("مرور مناسبت‌ها") }
            item {
                Column(Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    occasions.chunked(2).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            row.forEach { o ->
                                val (a, b) = paletteFor("occ-$o")
                                Box(
                                    Modifier.weight(1f).height(92.dp).clip(RoundedCornerShape(16.dp))
                                        .background(Brush.linearGradient(listOf(a, b))).clickable { occasion = o }
                                ) {
                                    MihrabPattern(Modifier.fillMaxSize(), alpha = 0.22f)
                                    Text(o, style = MaterialTheme.typography.titleMedium, color = Cream,
                                        modifier = Modifier.align(Alignment.TopStart).padding(14.dp))
                                    Text("${catalog.tracks.count { it.occasion.contains(o) }} اثر",
                                        style = MaterialTheme.typography.labelSmall, color = Cream.copy(alpha = 0.7f),
                                        modifier = Modifier.align(Alignment.BottomStart).padding(14.dp))
                                }
                            }
                            if (row.size == 1) Spacer(Modifier.weight(1f))
                        }
                    }
                }
            }
        } else {
            if (maddahMatches.isNotEmpty()) {
                item { SectionHeader("مداحان") }
                item {
                    LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                        items(maddahMatches, key = { it.id }) { m ->
                            Column(
                                Modifier.width(84.dp).clip(RoundedCornerShape(12.dp)).clickable { actions.openMaddah(m.id) },
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                MaddahAvatar(m, 76.dp)
                                Spacer(Modifier.height(6.dp))
                                Text(m.name, style = MaterialTheme.typography.labelMedium, maxLines = 2,
                                    textAlign = TextAlign.Center, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            }
            item { SectionHeader("آثار (${trackMatches.size})") }
            if (trackMatches.isEmpty()) item {
                EmptyState(Icons.Rounded.SearchOff, "نتیجه‌ای پیدا نشد", "املای دیگری امتحان کنید یا فیلترها را بردارید.")
            }
            items(trackMatches, key = { it.id }) { TrackRow(it, trackMatches) }
        }
    }
}
