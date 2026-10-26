package com.jarvis.premium

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.speech.RecognizerIntent
import android.speech.tts.TextToSpeech
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val Ink = Color(0xFF090B10)
private val Panel = Color(0xFF121720)
private val Panel2 = Color(0xFF19212D)
private val Violet = Color(0xFF9B8CFF)
private val Mint = Color(0xFF9CF2D2)
private val TextMain = Color(0xFFF3F5FA)
private val TextMuted = Color(0xFF929BAA)
private data class Message(val user: Boolean, val text: String)

class MainActivity : ComponentActivity() {
    private var tts: TextToSpeech? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this) { status -> if (status == TextToSpeech.SUCCESS) tts?.language = Locale("uz", "UZ") }
        setContent { JarvisApp(onSpeak = { tts?.speak(it, TextToSpeech.QUEUE_FLUSH, null, "jarvis_reply") }) }
    }
    override fun onDestroy() { tts?.shutdown(); super.onDestroy() }
}

@Composable
private fun JarvisApp(onSpeak: (String) -> Unit) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("jarvis_premium", 0) }
    var tab by remember { mutableStateOf("Home") }
    var input by remember { mutableStateOf("") }
    var notes by remember { mutableStateOf(prefs.getString("notes", "") ?: "") }
    var history by remember { mutableStateOf(prefs.getString("history", "") ?: "") }
    var messages by remember { mutableStateOf(listOf(Message(false, "Assalomu alaykum. Men JARVIS. Sizga qanday yordam beray?"))) }
    var language by remember { mutableStateOf(prefs.getString("language", "O‘zbekcha") ?: "O‘zbekcha") }
    var voiceReplies by remember { mutableStateOf(prefs.getBoolean("voice", true)) }
    var status by remember { mutableStateOf("TIZIM TAYYOR") }
    var googleQuery by remember { mutableStateOf("") }

    fun act(command: String) {
        val q = command.trim()
        if (q.isEmpty()) return
        val lower = q.lowercase(Locale.ROOT)
        var reply = when {
            listOf("salom", "assalomu", "hello", "hi").any { lower.contains(it) } -> "Va alaykum assalom. Nima qilamiz?"
            "vaqt" in lower || "soat" in lower -> "Hozirgi vaqt: ${SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())}."
            "sana" in lower || "bugun" in lower -> "Bugungi sana: ${SimpleDateFormat("dd MMMM yyyy", Locale("uz")).format(Date())}."
            "eslatma" in lower || "eslab qol" in lower || "note:" in lower -> {
                val note = q.substringAfter(":", q).removePrefix("eslatma").trim().ifBlank { q }
                notes = (notes + if (notes.isBlank()) "" else "\n\n" + "• " + note)
                prefs.edit().putString("notes", notes).apply()
                "Eslatmani xotiraga saqladim."
            }
            "youtube" in lower || "ютуб" in lower -> { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://m.youtube.com"))); "YouTube ochilyapti." }
            "google" in lower || "qidir" in lower || "internet" in lower -> {
                val search = q.replace(Regex("(?i)google|internetda|internet|qidir|qidirib ber"), "").trim().ifBlank { q }
                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + Uri.encode(search))))
                "Qidiruvni brauzerda ochdim: $search"
            }
            "telefon" in lower && ("qo‘ng‘iroq" in lower || "qongiroq" in lower || "ter" in lower) -> { context.startActivity(Intent(Intent.ACTION_DIAL)); "Telefon terish oynasini ochdim." }
            "sozlama" in lower -> { context.startActivity(Intent(android.provider.Settings.ACTION_SETTINGS)); "Telefon sozlamalarini ochdim." }
            "wifi" in lower || "wi-fi" in lower -> { context.startActivity(Intent(android.provider.Settings.ACTION_WIFI_SETTINGS)); "Wi-Fi sozlamalarini ochdim. Tarmoqni o‘zingiz tanlang." }
            "bluetooth" in lower -> { context.startActivity(Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS)); "Bluetooth sozlamalarini ochdim." }
            "signal" in lower || "budilnik" in lower || "alarm" in lower -> { context.startActivity(Intent(android.provider.AlarmClock.ACTION_SHOW_ALARMS)); "Budilnik ilovasini ochdim." }
            "yordam" in lower || "nima qila" in lower || "help" in lower -> "Men vaqt va sanani aytaman, eslatma saqlayman, internetdan qidiraman, YouTube, telefon terish oynasi, Wi-Fi, Bluetooth va sozlamalarni ochaman. Masalan: ‘Eslatma: kitob olish’, ‘Bugun sana’, ‘YouTube och’."
            else -> "Bu buyruq uchun hozircha tayyor offline javobim yo‘q. ‘Yordam’ deb yozing yoki internetdan qidirishni so‘rang. To‘liq AI javoblari uchun keyin xavfsiz backend ulash kerak bo‘ladi."
        }
        messages = messages + Message(true, q) + Message(false, reply)
        history = (history + if (history.isBlank()) "" else "\n") + "Siz: $q\nJARVIS: $reply"
        prefs.edit().putString("history", history).apply()
        status = "BUYRUQ BAJARILDI"
        if (voiceReplies) onSpeak(reply)
        input = ""
        if (tab != "Chat") tab = "Chat"
    }

    val imageLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            try {
                val share = Intent(Intent.ACTION_SEND).apply {
                    type = "image/*"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(Intent.createChooser(share, "Rasmni Google Lens yoki boshqa ilovada ochish"))
                status = "RASM ULASHISH OCHILDI"
            } catch (_: Exception) { status = "RASMNI OCHIB BO‘LMADI" }
        }
    }

    fun openGoogle(url: String) {
        try { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }
        catch (_: Exception) { status = "BRAUZER TOPILMADI" }
    }

    val voiceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (!spoken.isNullOrBlank()) act(spoken) else status = "OVOZ ANIQLANMADI"
    }
    fun listen() {
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, when(language) { "English" -> "en-US"; "Русский" -> "ru-RU"; else -> "uz-UZ" })
                putExtra(RecognizerIntent.EXTRA_PROMPT, "JARVIS ga buyruq bering")
            }
            voiceLauncher.launch(intent)
            status = "TINGLAYAPMAN…"
        } catch (_: Exception) { status = "OVOZ XIZMATI MAVJUD EMAS" }
    }

    MaterialTheme(colorScheme = darkColorScheme(background = Ink, surface = Panel, primary = Violet, onBackground = TextMain, onSurface = TextMain)) {
        Column(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color(0xFF10131B), Ink, Color(0xFF0D1017))))) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 22.dp, vertical = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(40.dp).clip(RoundedCornerShape(13.dp)).background(Brush.linearGradient(listOf(Violet, Color(0xFF545F83)))), contentAlignment = Alignment.Center) {
                    Icon(Icons.Filled.Bolt, null, tint = Color.White, modifier = Modifier.size(24.dp))
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("JARVIS", fontSize = 19.sp, fontWeight = FontWeight.Bold, letterSpacing = 3.sp, color = TextMain)
                    Text("PERSONAL INTELLIGENCE", fontSize = 9.sp, letterSpacing = 1.8.sp, color = TextMuted)
                }
                Box(Modifier.clip(CircleShape).background(Color(0xFF17251F)).border(1.dp, Color(0xFF315C4B), CircleShape).padding(horizontal = 10.dp, vertical = 7.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) { Box(Modifier.size(6.dp).clip(CircleShape).background(Mint)); Spacer(Modifier.width(6.dp)); Text("ONLINE", fontSize = 9.sp, letterSpacing = 1.sp, color = Mint) }
                }
            }
            when (tab) {
                "Home" -> HomeScreen(status, onListen = { listen() }, onQuick = { act(it) }, onChat = { tab = "Chat" })
                "Chat" -> ChatScreen(messages, input, { input = it }, { act(input) }, { listen() }, { messages = listOf(Message(false, "Suhbat tozalandi. Yangi buyruqni boshlaymiz.")) })
                "Memory" -> MemoryScreen(notes, history, onSave = { notes = it; prefs.edit().putString("notes", notes).apply() }, onClear = { history = ""; prefs.edit().putString("history", "").apply() })
                "Google" -> GoogleScreen(
                    query = googleQuery,
                    onQuery = { googleQuery = it },
                    onSearch = { q -> openGoogle("https://www.google.com/search?q=" + Uri.encode(q.ifBlank { "Google" })) },
                    onImages = { q -> openGoogle("https://www.google.com/search?tbm=isch&q=" + Uri.encode(q.ifBlank { "rasmlar" })) },
                    onLens = { imageLauncher.launch("image/*") },
                    onTranslate = { q -> openGoogle("https://translate.google.com/?sl=auto&tl=uz&text=" + Uri.encode(q) + "&op=translate") },
                    onMaps = { q -> openGoogle("https://www.google.com/maps/search/" + Uri.encode(q.ifBlank { "yaqin joylar" })) },
                    onGemini = { openGoogle("https://gemini.google.com/") },
                    onYoutube = { openGoogle("https://www.youtube.com/") }
                )
                else -> SettingsScreen(language, { language = it; prefs.edit().putString("language", it).apply() }, voiceReplies, { voiceReplies = it; prefs.edit().putBoolean("voice", it).apply() }, { context.startActivity(Intent(android.provider.Settings.ACTION_SETTINGS)) })
            }
            Spacer(Modifier.weight(1f, fill = false))
            NavigationBar(containerColor = Color(0xFF0C0F15), contentColor = TextMain, tonalElevation = 0.dp) {
                listOf(Triple("Home", Icons.Filled.GridView, "Bosh sahifa"), Triple("Chat", Icons.Filled.Forum, "Suhbat"), Triple("Google", Icons.Filled.Search, "Google"), Triple("Memory", Icons.Filled.BookmarkBorder, "Xotira"), Triple("Settings", Icons.Filled.Tune, "Sozlama")).forEach { item ->
                    NavigationBarItem(selected = tab == item.first, onClick = { tab = item.first }, icon = { Icon(item.second, null, modifier = Modifier.size(21.dp)) }, label = { Text(item.third, fontSize = 10.sp) }, colors = NavigationBarItemDefaults.colors(selectedIconColor = Violet, selectedTextColor = Violet, indicatorColor = Color(0xFF242236), unselectedIconColor = TextMuted, unselectedTextColor = TextMuted))
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(status: String, onListen: () -> Unit, onQuick: (String) -> Unit, onChat: () -> Unit) {
    val transition = rememberInfiniteTransition(label = "orb")
    val pulse by transition.animateFloat(initialValue = 0.96f, targetValue = 1.04f, animationSpec = infiniteRepeatable(tween(1900), RepeatMode.Reverse), label = "pulse")
    Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp).padding(top = 8.dp), horizontalAlignment = Alignment.Start) {
        Text("YAXSHI KUN.", fontSize = 11.sp, letterSpacing = 2.sp, color = TextMuted)
        Text("Nima rejalashtiramiz?", fontSize = 26.sp, fontWeight = FontWeight.SemiBold, color = TextMain, modifier = Modifier.padding(top = 6.dp))
        Text("Buyruq bering yoki suhbatni boshlang.", fontSize = 13.sp, color = TextMuted, modifier = Modifier.padding(top = 7.dp))
        Box(Modifier.fillMaxWidth().padding(top = 24.dp).height(270.dp).clip(RoundedCornerShape(28.dp)).background(Brush.linearGradient(listOf(Color(0xFF1B1B2D), Color(0xFF111923), Color(0xFF151622)))).border(1.dp, Color(0xFF303344), RoundedCornerShape(28.dp)), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(154.dp).scale(pulse).clip(CircleShape).background(Brush.radialGradient(listOf(Color(0xFF5D5A8F), Color(0xFF28273E), Color(0xFF171A25)))).border(1.dp, Violet.copy(alpha = .7f), CircleShape), contentAlignment = Alignment.Center) {
                    Box(Modifier.size(112.dp).clip(CircleShape).border(1.dp, Color(0xFF8D8AB8).copy(alpha = .8f), CircleShape), contentAlignment = Alignment.Center) {
                        Box(Modifier.size(72.dp).clip(CircleShape).background(Brush.linearGradient(listOf(Violet, Color(0xFF667A9E)))), contentAlignment = Alignment.Center) { Icon(Icons.Filled.GraphicEq, null, tint = Color.White, modifier = Modifier.size(37.dp)) }
                    }
                }
                Text("J A R V I S", fontSize = 13.sp, letterSpacing = 4.sp, color = TextMain, modifier = Modifier.padding(top = 17.dp))
                Text(status, fontSize = 9.sp, letterSpacing = 1.8.sp, color = Mint, modifier = Modifier.padding(top = 6.dp))
            }
        }
        Row(Modifier.fillMaxWidth().padding(top = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Button(onClick = onListen, modifier = Modifier.weight(1f).height(54.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Violet, contentColor = Color(0xFF10111A))) { Icon(Icons.Filled.Mic, null); Spacer(Modifier.width(8.dp)); Text("Ovozli buyruq", fontWeight = FontWeight.SemiBold) }
            OutlinedButton(onClick = onChat, modifier = Modifier.weight(.72f).height(54.dp), shape = RoundedCornerShape(16.dp), border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF3A4051)), colors = ButtonDefaults.outlinedButtonColors(contentColor = TextMain)) { Icon(Icons.Filled.MessageCircle, null); Spacer(Modifier.width(7.dp)); Text("Suhbat") }
        }
        Text("TEZKOR BUYRUQLAR", fontSize = 10.sp, letterSpacing = 2.sp, color = TextMuted, modifier = Modifier.padding(top = 25.dp, bottom = 11.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuickTile("Vaqt va sana", "Hozirgi vaqt", Icons.Filled.Schedule, Modifier.weight(1f)) { onQuick("Hozirgi vaqt") }
            QuickTile("YouTube", "Media ochish", Icons.Filled.PlayCircle, Modifier.weight(1f)) { onQuick("YouTube och") }
            QuickTile("Qidiruv", "Internet", Icons.Filled.Search, Modifier.weight(1f)) { onQuick("Google qidiruv") }
        }
        Row(Modifier.fillMaxWidth().padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            QuickTile("Wi-Fi", "Tarmoq", Icons.Filled.Wifi, Modifier.weight(1f)) { onQuick("Wi-Fi sozlamalari") }
            QuickTile("Budilnik", "Signal", Icons.Filled.Alarm, Modifier.weight(1f)) { onQuick("Budilnik och") }
            QuickTile("Yordam", "Buyruqlar", Icons.Filled.AutoAwesome, Modifier.weight(1f)) { onQuick("Yordam") }
        }
    }
}

@Composable
private fun QuickTile(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier, onClick: () -> Unit) {
    Column(modifier.height(104.dp).clip(RoundedCornerShape(17.dp)).background(Panel).border(1.dp, Color(0xFF252D3A), RoundedCornerShape(17.dp)).clickable(onClick = onClick).padding(12.dp), verticalArrangement = Arrangement.SpaceBetween) {
        Icon(icon, null, tint = Violet, modifier = Modifier.size(21.dp))
        Column { Text(title, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = TextMain, maxLines = 1, overflow = TextOverflow.Ellipsis); Text(subtitle, fontSize = 9.sp, color = TextMuted, maxLines = 1) }
    }
}

@Composable
private fun ChatScreen(messages: List<Message>, input: String, onInput: (String) -> Unit, onSend: () -> Unit, onMic: () -> Unit, onClear: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp)) {
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) { Text("Suhbat", fontSize = 24.sp, fontWeight = FontWeight.SemiBold); Text("JARVIS bilan muloqot", fontSize = 12.sp, color = TextMuted) }
            IconButton(onClick = onClear) { Icon(Icons.Filled.DeleteOutline, "Suhbatni tozalash", tint = TextMuted) }
        }
        LazyColumn(Modifier.fillMaxWidth().heightIn(min = 280.dp, max = 470.dp), verticalArrangement = Arrangement.spacedBy(12.dp), contentPadding = PaddingValues(vertical = 12.dp)) {
            items(messages) { msg ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = if (msg.user) Arrangement.End else Arrangement.Start) {
                    Column(Modifier.widthIn(max = 300.dp).clip(RoundedCornerShape(18.dp)).background(if (msg.user) Color(0xFF29263F) else Panel).border(1.dp, if (msg.user) Color(0xFF514A76) else Color(0xFF252D3A), RoundedCornerShape(18.dp)).padding(14.dp)) {
                        Text(if (msg.user) "SIZ" else "JARVIS", fontSize = 9.sp, letterSpacing = 1.5.sp, color = if (msg.user) Violet else Mint, fontWeight = FontWeight.Bold)
                        Text(msg.text, fontSize = 14.sp, lineHeight = 20.sp, color = TextMain, modifier = Modifier.padding(top = 5.dp))
                    }
                }
            }
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 12.dp).clip(RoundedCornerShape(18.dp)).background(Panel).border(1.dp, Color(0xFF303746), RoundedCornerShape(18.dp)).padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            BasicTextField(value = input, onValueChange = onInput, modifier = Modifier.weight(1f).padding(horizontal = 10.dp, vertical = 12.dp), textStyle = androidx.compose.ui.text.TextStyle(color = TextMain, fontSize = 14.sp), cursorBrush = androidx.compose.ui.graphics.SolidColor(Violet), decorationBox = { inner -> if (input.isEmpty()) Text("JARVIS ga yozing…", color = TextMuted, fontSize = 14.sp); inner() })
            IconButton(onClick = onMic) { Icon(Icons.Filled.Mic, "Ovoz", tint = Violet) }
            IconButton(onClick = onSend, modifier = Modifier.clip(RoundedCornerShape(12.dp)).background(Violet)) { Icon(Icons.Filled.ArrowUpward, "Yuborish", tint = Ink) }
        }
    }
}

@Composable
private fun GoogleScreen(
    query: String,
    onQuery: (String) -> Unit,
    onSearch: (String) -> Unit,
    onImages: (String) -> Unit,
    onLens: () -> Unit,
    onTranslate: (String) -> Unit,
    onMaps: (String) -> Unit,
    onGemini: () -> Unit,
    onYoutube: () -> Unit
) {
    Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 20.dp).padding(top = 12.dp, bottom = 16.dp)) {
        Text("Google vositalari", fontSize = 25.sp, fontWeight = FontWeight.SemiBold, color = TextMain)
        Text("Qidiruv, rasm, tarjima va AI — bitta joyda", fontSize = 12.sp, color = TextMuted, modifier = Modifier.padding(top = 6.dp, bottom = 18.dp))
        Row(Modifier.fillMaxWidth().clip(RoundedCornerShape(17.dp)).background(Panel).border(1.dp, Color(0xFF35394A), RoundedCornerShape(17.dp)).padding(start = 14.dp, end = 6.dp, top = 5.dp, bottom = 5.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.Search, null, tint = Violet)
            BasicTextField(value = query, onValueChange = onQuery, modifier = Modifier.weight(1f).padding(horizontal = 10.dp, vertical = 13.dp), singleLine = true, textStyle = androidx.compose.ui.text.TextStyle(color = TextMain, fontSize = 14.sp), cursorBrush = androidx.compose.ui.graphics.SolidColor(Violet), decorationBox = { inner -> if (query.isEmpty()) Text("Nimani qidiramiz?", color = TextMuted, fontSize = 14.sp); inner() })
            IconButton(onClick = { onSearch(query) }) { Icon(Icons.Filled.ArrowForward, "Qidirish", tint = Violet) }
        }
        Text("QIDIRUV TURLARI", fontSize = 10.sp, letterSpacing = 1.8.sp, color = TextMuted, modifier = Modifier.padding(top = 22.dp, bottom = 10.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GoogleTile("Web qidiruv", "Sayt va javoblar", Icons.Filled.Language, Modifier.weight(1f)) { onSearch(query) }
            GoogleTile("Rasmlar", "Rasm topish", Icons.Filled.ImageSearch, Modifier.weight(1f)) { onImages(query) }
        }
        Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            GoogleTile("Lens / rasm", "Rasmni tanlash", Icons.Filled.CenterFocusStrong, Modifier.weight(1f)) { onLens() }
            GoogleTile("Tarjimon", "Matn tarjimasi", Icons.Filled.Translate, Modifier.weight(1f)) { onTranslate(query) }
        }
        Text("AI VA XARITALAR", fontSize = 10.sp, letterSpacing = 1.8.sp, color = TextMuted, modifier = Modifier.padding(top = 22.dp, bottom = 10.dp))
        GoogleTile("Google Gemini", "AI bilan suhbatni ochish", Icons.Filled.AutoAwesome, Modifier.fillMaxWidth()) { onGemini() }
        GoogleTile("Google Maps", "Joy va yo‘nalish qidirish", Icons.Filled.Map, Modifier.fillMaxWidth().padding(top = 10.dp)) { onMaps(query) }
        GoogleTile("YouTube", "Video qidirish va ko‘rish", Icons.Filled.PlayCircle, Modifier.fillMaxWidth().padding(top = 10.dp)) { onYoutube() }
        Text("Eslatma: Google xizmatlari internet va tegishli ilova/hisob mavjudligiga bog‘liq. Rasm tanlanganda Android ulashish oynasi ochiladi; Google Lens ko‘rinishi qurilmaga qarab farq qilishi mumkin.", fontSize = 10.sp, lineHeight = 15.sp, color = TextMuted, modifier = Modifier.padding(top = 16.dp, bottom = 12.dp))
    }
}

@Composable
private fun GoogleTile(title: String, subtitle: String, icon: androidx.compose.ui.graphics.vector.ImageVector, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Row(modifier.clip(RoundedCornerShape(16.dp)).background(Panel).border(1.dp, Color(0xFF28303D), RoundedCornerShape(16.dp)).clickable(onClick = onClick).padding(horizontal = 14.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(39.dp).clip(RoundedCornerShape(12.dp)).background(Color(0xFF242238)), contentAlignment = Alignment.Center) { Icon(icon, null, tint = Violet, modifier = Modifier.size(21.dp)) }
        Spacer(Modifier.width(11.dp))
        Column(Modifier.weight(1f)) { Text(title, color = TextMain, fontWeight = FontWeight.SemiBold, fontSize = 12.sp); Text(subtitle, color = TextMuted, fontSize = 10.sp, modifier = Modifier.padding(top = 3.dp)) }
        Icon(Icons.Filled.ChevronRight, null, tint = TextMuted, modifier = Modifier.size(18.dp))
    }
}

@Composable
private fun MemoryScreen(notes: String, history: String, onSave: (String) -> Unit, onClear: () -> Unit) {
    var draft by remember(notes) { mutableStateOf(notes) }
    Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp).padding(top = 12.dp)) {
        Text("Xotira", fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
        Text("Muhim narsalarni o‘zingiz uchun saqlang.", fontSize = 13.sp, color = TextMuted, modifier = Modifier.padding(top = 5.dp, bottom = 20.dp))
        Text("ESLATMALAR", fontSize = 10.sp, letterSpacing = 2.sp, color = Mint)
        OutlinedTextField(value = draft, onValueChange = { draft = it }, modifier = Modifier.fillMaxWidth().padding(top = 10.dp).height(170.dp), placeholder = { Text("Eslatmalaringizni shu yerga yozing…", color = TextMuted) }, shape = RoundedCornerShape(18.dp), colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = Violet, unfocusedBorderColor = Color(0xFF303746), focusedTextColor = TextMain, unfocusedTextColor = TextMain, cursorColor = Violet))
        Button(onClick = { onSave(draft) }, modifier = Modifier.fillMaxWidth().padding(top = 10.dp).height(50.dp), shape = RoundedCornerShape(14.dp), colors = ButtonDefaults.buttonColors(containerColor = Violet, contentColor = Ink)) { Icon(Icons.Filled.Save, null); Spacer(Modifier.width(8.dp)); Text("Xotirani saqlash") }
        Row(Modifier.fillMaxWidth().padding(top = 26.dp, bottom = 10.dp), verticalAlignment = Alignment.CenterVertically) { Text("SUHBATLAR TARIXI", fontSize = 10.sp, letterSpacing = 2.sp, color = TextMuted, modifier = Modifier.weight(1f)); TextButton(onClick = onClear) { Text("Tozalash", color = Violet) } }
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(Panel).padding(15.dp)) { Text(if (history.isBlank()) "Hozircha saqlangan suhbat yo‘q." else history, color = if (history.isBlank()) TextMuted else TextMain, fontSize = 12.sp, lineHeight = 18.sp, maxLines = 8, overflow = TextOverflow.Ellipsis) }
    }
}

@Composable
private fun SettingsScreen(language: String, onLanguage: (String) -> Unit, voice: Boolean, onVoice: (Boolean) -> Unit, onSystemSettings: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(horizontal = 22.dp).padding(top = 12.dp)) {
        Text("Sozlamalar", fontSize = 26.sp, fontWeight = FontWeight.SemiBold)
        Text("JARVIS tajribasini o‘zingizga moslang.", fontSize = 13.sp, color = TextMuted, modifier = Modifier.padding(top = 5.dp, bottom = 22.dp))
        SettingsCard {
            Text("Til", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text("Ovozli buyruqlar tili", color = TextMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp, bottom = 12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { listOf("O‘zbekcha", "English", "Русский").forEach { lang -> FilterChip(selected = language == lang, onClick = { onLanguage(lang) }, label = { Text(lang, fontSize = 11.sp) }, colors = FilterChipDefaults.filterChipColors(selectedContainerColor = Color(0xFF34304F), selectedLabelColor = TextMain)) } }
        }
        SettingsCard(Modifier.padding(top = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("Ovozli javob", fontWeight = FontWeight.SemiBold, fontSize = 15.sp); Text("JARVIS javoblarini o‘qib beradi", color = TextMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp)) }; Switch(checked = voice, onCheckedChange = onVoice, colors = SwitchDefaults.colors(checkedThumbColor = Ink, checkedTrackColor = Violet)) }
        }
        SettingsCard(Modifier.padding(top = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) { Column(Modifier.weight(1f)) { Text("Telefon sozlamalari", fontWeight = FontWeight.SemiBold, fontSize = 15.sp); Text("Ruxsat va tizim boshqaruvi", color = TextMuted, fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp)) }; IconButton(onClick = onSystemSettings) { Icon(Icons.Filled.OpenInNew, null, tint = Violet) } }
        }
        Text("JARVIS 2.0  •  PREMIUM INTERFACE", fontSize = 9.sp, letterSpacing = 1.4.sp, color = TextMuted, modifier = Modifier.padding(top = 25.dp))
        Text("Eslatma: hozirgi versiyada buyruqlar telefonda bajariladi. To‘liq internet-AI va doimiy ‘Hey Jarvis’ uchun alohida backend hamda qo‘shimcha ruxsatlar kerak.", fontSize = 11.sp, lineHeight = 16.sp, color = TextMuted, modifier = Modifier.padding(top = 8.dp))
    }
}

@Composable
private fun SettingsCard(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(modifier.fillMaxWidth().clip(RoundedCornerShape(19.dp)).background(Panel).border(1.dp, Color(0xFF28303D), RoundedCornerShape(19.dp)).padding(16.dp), content = content)
}
