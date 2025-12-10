package com.intokapp.app.ui.components

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.intokapp.app.ui.theme.*

// Emoji Categories
enum class EmojiCategory(val icon: ImageVector, val label: String) {
    FREQUENT(Icons.Default.Schedule, "Frequent"),
    SMILEYS(Icons.Default.EmojiEmotions, "Smileys"),
    PEOPLE(Icons.Default.People, "People"),
    ANIMALS(Icons.Default.Pets, "Animals"),
    FOOD(Icons.Default.Restaurant, "Food"),
    ACTIVITIES(Icons.Default.SportsBasketball, "Activities"),
    TRAVEL(Icons.Default.Flight, "Travel"),
    OBJECTS(Icons.Default.Lightbulb, "Objects"),
    SYMBOLS(Icons.Default.Tag, "Symbols"),
    FLAGS(Icons.Default.Flag, "Flags")
}

// Emoji Data
object EmojiData {
    val smileys = listOf(
        "😀", "😃", "😄", "😁", "😆", "😅", "🤣", "😂", "🙂", "🙃",
        "😉", "😊", "😇", "🥰", "😍", "🤩", "😘", "😗", "☺️", "😚",
        "😙", "🥲", "😋", "😛", "😜", "🤪", "😝", "🤑", "🤗", "🤭",
        "🤫", "🤔", "🤐", "🤨", "😐", "😑", "😶", "😏", "😒", "🙄",
        "😬", "🤥", "😌", "😔", "😪", "🤤", "😴", "😷", "🤒", "🤕",
        "🤢", "🤮", "🤧", "🥵", "🥶", "🥴", "😵", "🤯", "🤠", "🥳",
        "🥸", "😎", "🤓", "🧐", "😕", "😟", "🙁", "☹️", "😮", "😯",
        "😲", "😳", "🥺", "😦", "😧", "😨", "😰", "😥", "😢", "😭",
        "😱", "😖", "😣", "😞", "😓", "😩", "😫", "🥱", "😤", "😡",
        "😠", "🤬", "😈", "👿", "💀", "☠️", "💩", "🤡", "👹", "👺"
    )
    
    val people = listOf(
        "👋", "🤚", "🖐️", "✋", "🖖", "👌", "🤌", "🤏", "✌️", "🤞",
        "🤟", "🤘", "🤙", "👈", "👉", "👆", "🖕", "👇", "☝️", "👍",
        "👎", "✊", "👊", "🤛", "🤜", "👏", "🙌", "👐", "🤲", "🤝",
        "🙏", "✍️", "💅", "🤳", "💪", "🦾", "🦿", "🦵", "🦶", "👂",
        "🦻", "👃", "🧠", "🫀", "🫁", "🦷", "🦴", "👀", "👁️", "👅",
        "👄", "👶", "🧒", "👦", "👧", "🧑", "👱", "👨", "🧔", "👩"
    )
    
    val animals = listOf(
        "🐶", "🐱", "🐭", "🐹", "🐰", "🦊", "🐻", "🐼", "🐻‍❄️", "🐨",
        "🐯", "🦁", "🐮", "🐷", "🐽", "🐸", "🐵", "🙈", "🙉", "🙊",
        "🐒", "🐔", "🐧", "🐦", "🐤", "🐣", "🐥", "🦆", "🦅", "🦉",
        "🦇", "🐺", "🐗", "🐴", "🦄", "🐝", "🪱", "🐛", "🦋", "🐌",
        "🐞", "🐜", "🪰", "🪲", "🪳", "🦟", "🦗", "🕷️", "🦂", "🐢",
        "🐍", "🦎", "🦖", "🦕", "🐙", "🦑", "🦐", "🦞", "🦀", "🐡"
    )
    
    val food = listOf(
        "🍏", "🍎", "🍐", "🍊", "🍋", "🍌", "🍉", "🍇", "🍓", "🫐",
        "🍈", "🍒", "🍑", "🥭", "🍍", "🥥", "🥝", "🍅", "🍆", "🥑",
        "🥦", "🥬", "🥒", "🌶️", "🫑", "🌽", "🥕", "🫒", "🧄", "🧅",
        "🥔", "🍠", "🥐", "🥯", "🍞", "🥖", "🥨", "🧀", "🥚", "🍳",
        "🧈", "🥞", "🧇", "🥓", "🥩", "🍗", "🍖", "🦴", "🌭", "🍔",
        "🍟", "🍕", "🫓", "🥪", "🥙", "🧆", "🌮", "🌯", "🫔", "🥗"
    )
    
    val activities = listOf(
        "⚽", "🏀", "🏈", "⚾", "🥎", "🎾", "🏐", "🏉", "🥏", "🎱",
        "🪀", "🏓", "🏸", "🏒", "🏑", "🥍", "🏏", "🪃", "🥅", "⛳",
        "🪁", "🏹", "🎣", "🤿", "🥊", "🥋", "🎽", "🛹", "🛼", "🛷",
        "⛸️", "🥌", "🎿", "⛷️", "🏂", "🪂", "🏋️", "🤼", "🤸", "⛹️",
        "🤺", "🤾", "🏌️", "🏇", "⛑️", "🧘", "🏄", "🏊", "🤽", "🚣",
        "🧗", "🚵", "🚴", "🏆", "🥇", "🥈", "🥉", "🏅", "🎖️", "🎗️"
    )
    
    val travel = listOf(
        "🚗", "🚕", "🚙", "🚌", "🚎", "🏎️", "🚓", "🚑", "🚒", "🚐",
        "🛻", "🚚", "🚛", "🚜", "🦯", "🦽", "🦼", "🛴", "🚲", "🛵",
        "🏍️", "🛺", "🚨", "🚔", "🚍", "🚘", "🚖", "🚡", "🚠", "🚟",
        "🚃", "🚋", "🚞", "🚝", "🚄", "🚅", "🚈", "🚂", "🚆", "🚇",
        "🚊", "🚉", "✈️", "🛫", "🛬", "🛩️", "💺", "🛰️", "🚀", "🛸",
        "🚁", "🛶", "⛵", "🚤", "🛥️", "🛳️", "⛴️", "🚢", "⚓", "🪝"
    )
    
    val objects = listOf(
        "⌚", "📱", "📲", "💻", "⌨️", "🖥️", "🖨️", "🖱️", "🖲️", "🕹️",
        "🗜️", "💽", "💾", "💿", "📀", "📼", "📷", "📸", "📹", "🎥",
        "📽️", "🎞️", "📞", "☎️", "📟", "📠", "📺", "📻", "🎙️", "🎚️",
        "🎛️", "🧭", "⏱️", "⏲️", "⏰", "🕰️", "⌛", "⏳", "📡", "🔋",
        "🔌", "💡", "🔦", "🕯️", "🪔", "🧯", "🛢️", "💸", "💵", "💴",
        "💶", "💷", "🪙", "💰", "💳", "💎", "⚖️", "🪜", "🧰", "🪛"
    )
    
    val symbols = listOf(
        "❤️", "🧡", "💛", "💚", "💙", "💜", "🖤", "🤍", "🤎", "💔",
        "❣️", "💕", "💞", "💓", "💗", "💖", "💘", "💝", "💟", "☮️",
        "✝️", "☪️", "🕉️", "☸️", "✡️", "🔯", "🕎", "☯️", "☦️", "🛐",
        "⛎", "♈", "♉", "♊", "♋", "♌", "♍", "♎", "♏", "♐",
        "♑", "♒", "♓", "🆔", "⚛️", "🉑", "☢️", "☣️", "📴", "📳",
        "🈶", "🈚", "🈸", "🈺", "🈷️", "✴️", "🆚", "💮", "🉐", "㊙️"
    )
    
    val flags = listOf(
        "🏳️", "🏴", "🏴‍☠️", "🏁", "🚩", "🎌", "🏳️‍🌈", "🏳️‍⚧️", "🇺🇳", "🇦🇫",
        "🇦🇱", "🇩🇿", "🇦🇸", "🇦🇩", "🇦🇴", "🇦🇮", "🇦🇶", "🇦🇬", "🇦🇷", "🇦🇲",
        "🇦🇼", "🇦🇺", "🇦🇹", "🇦🇿", "🇧🇸", "🇧🇭", "🇧🇩", "🇧🇧", "🇧🇾", "🇧🇪",
        "🇧🇿", "🇧🇯", "🇧🇲", "🇧🇹", "🇧🇴", "🇧🇦", "🇧🇼", "🇧🇷", "🇮🇴", "🇻🇬",
        "🇧🇳", "🇧🇬", "🇧🇫", "🇧🇮", "🇰🇭", "🇨🇲", "🇨🇦", "🇮🇨", "🇨🇻", "🇧🇶",
        "🇰🇾", "🇨🇫", "🇹🇩", "🇨🇱", "🇨🇳", "🇨🇽", "🇨🇨", "🇨🇴", "🇰🇲", "🇨🇬"
    )
    
    fun getEmojisForCategory(category: EmojiCategory): List<String> {
        return when (category) {
            EmojiCategory.FREQUENT -> emptyList() // Handled separately
            EmojiCategory.SMILEYS -> smileys
            EmojiCategory.PEOPLE -> people
            EmojiCategory.ANIMALS -> animals
            EmojiCategory.FOOD -> food
            EmojiCategory.ACTIVITIES -> activities
            EmojiCategory.TRAVEL -> travel
            EmojiCategory.OBJECTS -> objects
            EmojiCategory.SYMBOLS -> symbols
            EmojiCategory.FLAGS -> flags
        }
    }
    
    fun searchEmojis(query: String): List<String> {
        if (query.isBlank()) return emptyList()
        val allEmojis = smileys + people + animals + food + activities + travel + objects + symbols + flags
        // Simple search - in real app would have emoji names/keywords
        return allEmojis.take(50)
    }
}

// Frequently Used Emoji Manager
class FrequentEmojiManager(context: Context) {
    private val prefs = context.getSharedPreferences("emoji_prefs", Context.MODE_PRIVATE)
    private val key = "frequent"
    private val defaultEmojis = listOf("👍", "❤️", "😂", "🔥", "👏")
    
    fun getFrequentEmojis(limit: Int = 20): List<String> {
        val stored = prefs.getString(key, null)
        return if (stored.isNullOrEmpty()) {
            defaultEmojis
        } else {
            stored.split(",").take(limit)
        }
    }
    
    fun getTopFrequent(limit: Int = 5): List<String> {
        return getFrequentEmojis().take(limit).ifEmpty { defaultEmojis.take(limit) }
    }
    
    fun recordUsage(emoji: String) {
        val current = getFrequentEmojis().toMutableList()
        current.remove(emoji)
        current.add(0, emoji)
        val updated = current.take(20).joinToString(",")
        prefs.edit().putString(key, updated).apply()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmojiPickerSheet(
    onEmojiSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val frequentManager = remember { FrequentEmojiManager(context) }
    
    var selectedCategory by remember { mutableStateOf(EmojiCategory.FREQUENT) }
    var searchQuery by remember { mutableStateOf("") }
    
    val frequentEmojis = remember { frequentManager.getFrequentEmojis() }
    
    val displayEmojis = remember(selectedCategory, searchQuery) {
        when {
            searchQuery.isNotBlank() -> EmojiData.searchEmojis(searchQuery)
            selectedCategory == EmojiCategory.FREQUENT -> frequentEmojis.ifEmpty { EmojiData.smileys.take(20) }
            else -> EmojiData.getEmojisForCategory(selectedCategory)
        }
    }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = Surface800,
        contentColor = White,
        dragHandle = {
            Surface(
                modifier = Modifier
                    .padding(vertical = 8.dp)
                    .width(32.dp)
                    .height(4.dp),
                color = Surface600,
                shape = RoundedCornerShape(2.dp)
            ) {}
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 400.dp)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("Search emojis...", color = Surface500) },
                leadingIcon = { Icon(Icons.Default.Search, null, tint = Surface500) },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = White,
                    unfocusedTextColor = White,
                    focusedBorderColor = Purple500,
                    unfocusedBorderColor = Surface700,
                    focusedContainerColor = Surface900,
                    unfocusedContainerColor = Surface900
                ),
                singleLine = true
            )
            
            // Category Tabs
            if (searchQuery.isBlank()) {
                ScrollableTabRow(
                    selectedTabIndex = EmojiCategory.entries.indexOf(selectedCategory),
                    containerColor = Surface800,
                    contentColor = White,
                    edgePadding = 8.dp,
                    divider = {}
                ) {
                    EmojiCategory.entries.forEach { category ->
                        Tab(
                            selected = selectedCategory == category,
                            onClick = { selectedCategory = category },
                            icon = {
                                Icon(
                                    category.icon,
                                    contentDescription = category.label,
                                    tint = if (selectedCategory == category) Purple500 else Surface400,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        )
                    }
                }
            }
            
            // Emoji Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(8),
                contentPadding = PaddingValues(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(displayEmojis) { emoji ->
                    Box(
                        modifier = Modifier
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                frequentManager.recordUsage(emoji)
                                onEmojiSelected(emoji)
                            }
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = emoji,
                            fontSize = 24.sp
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

@Composable
fun QuickEmojiBar(
    frequentEmojis: List<String>,
    onEmojiSelected: (String) -> Unit,
    onMoreClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        frequentEmojis.take(5).forEach { emoji ->
            Surface(
                shape = CircleShape,
                color = Surface700,
                modifier = Modifier
                    .size(40.dp)
                    .clickable { onEmojiSelected(emoji) }
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(emoji, fontSize = 20.sp)
                }
            }
        }
        
        // More button
        Surface(
            shape = CircleShape,
            color = Purple500.copy(alpha = 0.3f),
            modifier = Modifier
                .size(40.dp)
                .clickable { onMoreClick() }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "More emojis",
                    tint = Purple500,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
