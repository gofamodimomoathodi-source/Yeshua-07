package com.yhwh.bible

import android.content.Context
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.DropdownMenu
import android.database.sqlite.SQLiteDatabase
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException

private enum class AppTab(val title: String) {
    Home("Home"), Bible("Bible"), Study("Word Study"), Pdf("PDF"), Profile("Profile")
}

private data class Verse(val bookId: Int, val chapter: Int, val number: Int, val text: String) {
    val reference: String get() = "${BIBLE_BOOKS[bookId - 1]} $chapter:$number"
}

private enum class BibleTranslation(val label: String, val assetName: String, val availabilityNote: String) {
    KJV("KJV — King James Version", "kjv", "Included in the standard build"),
    NKJV("NKJV — New King James Version", "nkjv", "Requires an authorized text/database license"),
    LSB("LSB — Legacy Standard Bible", "lsb", "Requires an authorized text/database license"),
    AMP("AMP — Amplified Bible", "amp", "Requires an authorized text/database license")
}

private val BIBLE_BOOKS = listOf(
    "Genesis", "Exodus", "Leviticus", "Numbers", "Deuteronomy", "Joshua", "Judges", "Ruth",
    "1 Samuel", "2 Samuel", "1 Kings", "2 Kings", "1 Chronicles", "2 Chronicles", "Ezra",
    "Nehemiah", "Esther", "Job", "Psalms", "Proverbs", "Ecclesiastes", "Song of Solomon",
    "Isaiah", "Jeremiah", "Lamentations", "Ezekiel", "Daniel", "Hosea", "Joel", "Amos",
    "Obadiah", "Jonah", "Micah", "Nahum", "Habakkuk", "Zephaniah", "Haggai", "Zechariah",
    "Malachi", "Matthew", "Mark", "Luke", "John", "Acts", "Romans", "1 Corinthians",
    "2 Corinthians", "Galatians", "Ephesians", "Philippians", "Colossians", "1 Thessalonians",
    "2 Thessalonians", "1 Timothy", "2 Timothy", "Titus", "Philemon", "Hebrews", "James",
    "1 Peter", "2 Peter", "1 John", "2 John", "3 John", "Jude", "Revelation"
)

private class BibleDatabase(private val context: Context, private val translation: BibleTranslation) {
    private fun databaseFile(): java.io.File {
        val assetName = "${translation.assetName}.sqlite"
        val file = context.getDatabasePath(assetName)
        if (!file.exists()) {
            val availableAssets = context.assets.list("")?.toSet().orEmpty()
            if (assetName !in availableAssets) {
                throw IOException("${translation.label} text is not installed. ${translation.availabilityNote}. Add the authorized ${translation.assetName}.sqlite file to app/src/main/assets/.")
            }
            file.parentFile?.mkdirs()
            context.assets.open(assetName).use { input ->
                file.outputStream().use { output -> input.copyTo(output) }
            }
        }
        return file
    }

    fun loadChapter(bookId: Int, chapter: Int): List<Verse> {
        val db = SQLiteDatabase.openDatabase(databaseFile().absolutePath, null, SQLiteDatabase.OPEN_READONLY)
        return try {
            db.rawQuery(
                "SELECT book_id, chapter, number, text FROM verses WHERE book_id = ? AND chapter = ? ORDER BY number",
                arrayOf(bookId.toString(), chapter.toString())
            ).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) {
                        add(Verse(cursor.getInt(0), cursor.getInt(1), cursor.getInt(2), cursor.getString(3)))
                    }
                }
            }
        } finally { db.close() }
    }

    fun chapters(bookId: Int): List<Int> {
        val db = SQLiteDatabase.openDatabase(databaseFile().absolutePath, null, SQLiteDatabase.OPEN_READONLY)
        return try {
            db.rawQuery("SELECT DISTINCT chapter FROM verses WHERE book_id = ? ORDER BY chapter", arrayOf(bookId.toString())).use { cursor ->
                buildList { while (cursor.moveToNext()) add(cursor.getInt(0)) }
            }
        } finally { db.close() }
    }

    fun search(query: String): List<Verse> {
        val db = SQLiteDatabase.openDatabase(databaseFile().absolutePath, null, SQLiteDatabase.OPEN_READONLY)
        return try {
            db.rawQuery(
                "SELECT book_id, chapter, number, text FROM verses WHERE text LIKE ? ORDER BY book_id, chapter, number LIMIT 150",
                arrayOf("%$query%")
            ).use { cursor ->
                buildList {
                    while (cursor.moveToNext()) add(Verse(cursor.getInt(0), cursor.getInt(1), cursor.getInt(2), cursor.getString(3)))
                }
            }
        } finally { db.close() }
    }
}

private val sampleLexicon = listOf(
    LexiconEntry("λόγος", "Greek", "logos", "Word, message, or account. Example entry only."),
    LexiconEntry("ἀγάπη", "Greek", "agapē", "Love or goodwill. Example entry only."),
    LexiconEntry("שָׁלוֹם", "Hebrew", "shalom", "Peace, well-being, or wholeness. Example entry only."),
    LexiconEntry("חֶסֶד", "Hebrew", "chesed", "Kindness or steadfast love. Example entry only.")
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            var darkMode by remember { mutableStateOf(false) }
            MaterialTheme(colorScheme = if (darkMode) darkColorScheme() else lightColorScheme()) {
                YhwhApp(onToggleTheme = { darkMode = !darkMode })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun YhwhApp(onToggleTheme: () -> Unit) {
    var selectedTab by remember { mutableStateOf(AppTab.Home) }
    var search by remember { mutableStateOf("") }
    var pdfUri by remember { mutableStateOf<Uri?>(null) }
    val pdfPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) pdfUri = uri
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("YHWH", fontWeight = FontWeight.Bold)
                        Text(selectedTab.title, style = MaterialTheme.typography.labelSmall)
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                listOf(
                    AppTab.Home to Icons.Default.Home,
                    AppTab.Bible to Icons.Default.Book,
                    AppTab.Study to Icons.Default.MenuBook,
                    AppTab.Pdf to Icons.Default.PictureAsPdf,
                    AppTab.Profile to Icons.Default.Person
                ).forEach { (tab, icon) ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Icon(icon, contentDescription = tab.title) },
                        label = { Text(tab.title) }
                    )
                }
            }
        }
    ) { padding ->
        Surface(Modifier.fillMaxSize().padding(padding)) {
            when (selectedTab) {
                AppTab.Home -> HomeScreen(
                    onOpenBible = { selectedTab = AppTab.Bible },
                    onOpenStudy = { selectedTab = AppTab.Study },
                    onOpenPdf = { selectedTab = AppTab.Pdf; pdfPicker.launch(arrayOf("application/pdf")) },
                    onToggleTheme = onToggleTheme
                )
                AppTab.Bible -> BibleScreen(search = search, onSearchChange = { search = it })
                AppTab.Study -> WordStudyScreen()
                AppTab.Pdf -> PdfScreen(
                    uri = pdfUri,
                    onPickPdf = { pdfPicker.launch(arrayOf("application/pdf")) }
                )
                AppTab.Profile -> ProfileScreen()
            }
        }
    }
}

@Composable
private fun HomeScreen(
    onOpenBible: () -> Unit,
    onOpenStudy: () -> Unit,
    onOpenPdf: () -> Unit,
    onToggleTheme: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
                Column(Modifier.padding(20.dp)) {
                    Text("Welcome to YHWH", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    Text("Read Scripture, explore original-language word studies, and open Bible PDFs.")
                    Spacer(Modifier.height(12.dp))
                    Button(onClick = onOpenBible) { Text("Open the Bible") }
                }
            }
        }
        item { FeatureCard("Daily Scripture", "Psalm 119:105", "Thy word is a lamp unto my feet, and a light unto my path.", onOpenBible) }
        item { FeatureCard("Greek & Hebrew", "Explore sample word entries", "Original-language examples are illustrative and not a complete lexicon.", onOpenStudy) }
        item { FeatureCard("PDF Reader", "Open a PDF from your phone", "Import a PDF using Android's file picker.", onOpenPdf) }
        item { OutlinedButton(onClick = onToggleTheme, modifier = Modifier.fillMaxWidth()) { Text("Toggle light / dark theme") } }
        item {
            Text(
                "Prototype notice: this version contains sample Scripture and word-study data. It is not yet a complete Bible or production account system.",
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}

@Composable
private fun FeatureCard(title: String, subtitle: String, body: String, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text(subtitle, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(6.dp))
            Text(body, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BibleScreen(search: String, onSearchChange: (String) -> Unit) {
    val context = LocalContext.current
    var selectedBook by remember { mutableIntStateOf(1) }
    var selectedChapter by remember { mutableIntStateOf(1) }
    var selectedTranslation by remember { mutableStateOf(BibleTranslation.KJV) }
    var translationsExpanded by remember { mutableStateOf(false) }
    var booksExpanded by remember { mutableStateOf(false) }
    var chaptersExpanded by remember { mutableStateOf(false) }
    var verses by remember { mutableStateOf<List<Verse>>(emptyList()) }
    var searchResults by remember { mutableStateOf<List<Verse>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }

    LaunchedEffect(selectedTranslation, selectedBook, selectedChapter, search) {
        loading = true
        error = null
        try {
            val db = BibleDatabase(context, selectedTranslation)
            if (search.isNotBlank()) {
                searchResults = withContext(Dispatchers.IO) { db.search(search.trim()) }
            } else {
                verses = withContext(Dispatchers.IO) { db.loadChapter(selectedBook, selectedChapter) }
            }
        } catch (e: Exception) {
            error = e.localizedMessage ?: "${selectedTranslation.label} database unavailable."
            verses = emptyList()
            searchResults = emptyList()
        } finally {
            loading = false
        }
    }

    Column(Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Spacer(Modifier.height(8.dp))
        Box {
            OutlinedButton(onClick = { translationsExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                Text("Translation: ${selectedTranslation.name}")
            }
            DropdownMenu(expanded = translationsExpanded, onDismissRequest = { translationsExpanded = false }) {
                BibleTranslation.values().forEach { translation ->
                    DropdownMenuItem(
                        text = { Column {
                            Text(translation.label)
                            if (translation != BibleTranslation.KJV) {
                                Text("Licensed database required", style = MaterialTheme.typography.labelSmall)
                            }
                        } },
                        onClick = {
                            selectedTranslation = translation
                            translationsExpanded = false
                            searchResults = emptyList()
                            verses = emptyList()
                        }
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = search,
            onValueChange = onSearchChange,
            modifier = Modifier.fillMaxWidth(),
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            label = { Text("Search ${selectedTranslation.name} verses") },
            singleLine = true
        )
        Spacer(Modifier.height(8.dp))
        if (search.isBlank()) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Box {
                    OutlinedButton(onClick = { booksExpanded = true }) { Text(BIBLE_BOOKS[selectedBook - 1]) }
                    DropdownMenu(expanded = booksExpanded, onDismissRequest = { booksExpanded = false }) {
                        BIBLE_BOOKS.forEachIndexed { index, name ->
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = {
                                    selectedBook = index + 1
                                    selectedChapter = 1
                                    booksExpanded = false
                                }
                            )
                        }
                    }
                }
                Box {
                    OutlinedButton(onClick = { chaptersExpanded = true }) { Text("Chapter $selectedChapter") }
                    DropdownMenu(expanded = chaptersExpanded, onDismissRequest = { chaptersExpanded = false }) {
                        (1..chapterCounts[selectedBook - 1]).forEach { chapter ->
                            DropdownMenuItem(
                                text = { Text(chapter.toString()) },
                                onClick = { selectedChapter = chapter; chaptersExpanded = false }
                            )
                        }
                    }
                }
            }
        } else {
            Text("Search results are limited to the first 150 matches.", style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(8.dp))
        if (loading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { Text("Loading ${selectedTranslation.name}…") }
        } else if (error != null) {
            Text(error!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodyMedium)
        } else {
            val displayVerses = if (search.isBlank()) verses else searchResults
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(displayVerses) { verse ->
                    Card(Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(14.dp)) {
                            Text(
                                verse.reference,
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(verse.text, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
                if (displayVerses.isEmpty()) item { Text("No verses found for this selection.") }
            }
        }
    }
}

private val chapterCounts = listOf(
    50, 40, 27, 36, 34, 24, 21, 4, 31, 24, 22, 25, 29, 36, 10, 13, 10, 42, 150, 31, 12, 8,
    66, 52, 5, 48, 12, 14, 3, 9, 1, 4, 7, 3, 3, 3, 2, 14, 4, 28, 16, 24, 21, 28, 16, 16, 13,
    6, 6, 4, 4, 5, 3, 6, 4, 3, 1, 13, 5, 5, 3, 5, 1, 1, 1, 22
)

@Composable
private fun WordStudyScreen() {
    LazyColumn(Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        item {
            Text("Greek & Hebrew word study", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("These are example entries only. Verified lexicon data and verse-level word alignment must be added separately.")
        }
        items(sampleLexicon) { entry ->
            Card(Modifier.fillMaxWidth()) {
                Column(Modifier.padding(16.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(entry.word, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                        Spacer(Modifier.width(12.dp))
                        Text(entry.language, style = MaterialTheme.typography.labelMedium)
                    }
                    Text("Transliteration: ${entry.transliteration}")
                    Spacer(Modifier.height(6.dp))
                    Text(entry.meaning)
                }
            }
        }
    }
}

@Composable
private fun ProfileScreen() {
    var email by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("Demo only — no account is created.") }
    Column(Modifier.fillMaxSize().padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Profile", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text("Account creation and secure authentication are not implemented in this prototype.")
        androidx.compose.material3.OutlinedTextField(
            value = email, onValueChange = { email = it },
            modifier = Modifier.fillMaxWidth(), label = { Text("Email (demo field)") }, singleLine = true
        )
        Button(onClick = { message = "Demo only. No account was created." }) { Text("Continue (demo)") }
        Text(message, style = MaterialTheme.typography.bodySmall)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PdfScreen(uri: Uri?, onPickPdf: () -> Unit) {
    var pageIndex by remember(uri) { mutableIntStateOf(0) }
    var pageCount by remember(uri) { mutableIntStateOf(0) }
    var bitmap by remember(uri) { mutableStateOf<Bitmap?>(null) }
    var error by remember(uri) { mutableStateOf<String?>(null) }
    var scale by remember(uri) { mutableFloatStateOf(1f) }
    var offsetX by remember(uri) { mutableFloatStateOf(0f) }
    var offsetY by remember(uri) { mutableFloatStateOf(0f) }
    val context = LocalContext.current

    LaunchedEffect(uri, pageIndex) {
        bitmap = null
        error = null
        if (uri != null) {
            try {
                val result = withContext(Dispatchers.IO) { renderPdfPage(context, uri, pageIndex) }
                pageCount = result.first
                bitmap = result.second
                if (pageIndex >= pageCount && pageCount > 0) pageIndex = pageCount - 1
            } catch (e: Exception) {
                error = "Could not open this PDF: ${e.localizedMessage ?: "unknown error"}"
            }
        }
    }

    val transformState = rememberTransformableState { zoomChange, panChange, _ ->
        scale = (scale * zoomChange).coerceIn(1f, 5f)
        offsetX += panChange.x
        offsetY += panChange.y
    }

    Column(Modifier.fillMaxSize().padding(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            Button(onClick = onPickPdf) { Text("Choose PDF") }
            if (uri != null) Text("Page ${pageIndex + 1} / ${pageCount.coerceAtLeast(1)}")
        }
        Spacer(Modifier.height(8.dp))
        if (uri == null) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Choose a PDF file from your device to begin.")
            }
        } else {
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            bitmap?.let { pageBitmap ->
                Box(
                    Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(8.dp)).background(MaterialTheme.colorScheme.surfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Image(
                        bitmap = pageBitmap.asImageBitmap(),
                        contentDescription = "PDF page ${pageIndex + 1}",
                        modifier = Modifier.fillMaxSize()
                            .graphicsLayer(scaleX = scale, scaleY = scale, translationX = offsetX, translationY = offsetY)
                            .transformable(transformState)
                    )
                }
            } ?: Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(if (error == null) "Rendering PDF page…" else "PDF could not be rendered.")
            }
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                OutlinedButton(onClick = {
                    if (pageIndex > 0) { pageIndex--; scale = 1f; offsetX = 0f; offsetY = 0f }
                }, enabled = pageIndex > 0) { Text("Previous") }
                OutlinedButton(onClick = {
                    if (pageCount > 0 && pageIndex < pageCount - 1) { pageIndex++; scale = 1f; offsetX = 0f; offsetY = 0f }
                }, enabled = pageCount > 0 && pageIndex < pageCount - 1) { Text("Next") }
            }
            Text("Pinch to zoom. Text search, selection, and annotations are not included yet.", style = MaterialTheme.typography.bodySmall)
        }
    }
}

private fun renderPdfPage(context: Context, uri: Uri, pageIndex: Int): Pair<Int, Bitmap> {
    val descriptor: ParcelFileDescriptor = context.contentResolver.openFileDescriptor(uri, "r")
        ?: throw IOException("Could not access selected PDF")
    descriptor.use { pfd ->
        PdfRenderer(pfd).use { renderer ->
            if (renderer.pageCount <= 0) throw IOException("PDF contains no pages")
            val safeIndex = pageIndex.coerceIn(0, renderer.pageCount - 1)
            renderer.openPage(safeIndex).use { page ->
                val width = (page.width * 1.5f).toInt().coerceAtLeast(1)
                val height = (page.height * 1.5f).toInt().coerceAtLeast(1)
                val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                return renderer.pageCount to bmp
            }
        }
    }
}

