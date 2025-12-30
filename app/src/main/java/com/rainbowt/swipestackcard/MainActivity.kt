package com.rainbowt.swipestackcard

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.rainbowt.swipestack.CardItem
import com.rainbowt.swipestack.SwipeStack
import com.rainbowt.swipestackcard.extension.loadWordsFromAssets
import com.rainbowt.swipestackcard.model.WordItem
import com.rainbowt.swipestackcard.ui.theme.SwipeStackCardTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            SwipeStackCardTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    SwipeStackCardTheme {
        Greeting("Android")
    }
}

@Preview
@Composable
fun DemoSwipeStack() {
    val items = remember { List(10) { CardItem(it, "Item $it") } }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        Text("showAllCards = false（固定 3 張）", style = MaterialTheme.typography.titleMedium)
        SwipeStack(
            items = items,
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp),
            showAllCards = false,
            cardContent = { item ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Card: ${item.title}",
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
            }
        )

        Text("showAllCards = true（有幾張顯示幾張）", style = MaterialTheme.typography.titleMedium)
        SwipeStack(
            items = items,
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp),
            showAllCards = true,
            cardContent = { item ->
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Card: ${item.title}",
                        style = MaterialTheme.typography.headlineSmall
                    )
                }
            }
        )
    }
}

@Preview
@Composable
fun LessonScreen() {
    val context = LocalContext.current

    val words by produceState<List<WordItem>>(initialValue = emptyList(), "第 1 課.json") {
        value = loadWordsFromAssets(context, "第 1 課.json")
    }

    SwipeStack(
        items = words,
        modifier = Modifier
            .fillMaxWidth()
            .height(320.dp),
        showAllCards = false,
        onSwiped = { _, _ -> },
        cardContent = { w ->
            Column(
                Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(w.jp, style = MaterialTheme.typography.headlineLarge)
                Spacer(Modifier.height(8.dp))
                Text(w.kanji, style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(12.dp))
                Text(w.zh, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(6.dp))
                Text("${w.pos} · ${w.romaji}", style = MaterialTheme.typography.bodyMedium)
            }
        }
    )
}

private enum class WordFace {
    JP,        // 日文
    KANJI,     // 漢字
    ZH,        // 中文
    POS,       // 詞性
    ROMAJI     // 羅馬拼音
}

@Preview
@Composable
fun SingleWordScreen(fileName: String = "第 1 課.json") {
    val context = LocalContext.current

    val words by produceState<List<WordItem>>(initialValue = emptyList(), fileName) {
        value = loadWordsFromAssets(context, fileName)
    }

    if (words.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    // 目前第幾個單字
    var wordIndex by remember { mutableIntStateOf(0) }
    val word = words[wordIndex.coerceIn(0, words.lastIndex)]

    // 這個單字要顯示的「所有卡片」
    val faces = remember {
        listOf(
            WordFace.JP,
            WordFace.KANJI,
            WordFace.ZH,
            WordFace.POS,
            WordFace.ROMAJI
        )
    }

    // 記錄這個單字已 swipe 幾張
    var swipedCount by remember(wordIndex) { mutableIntStateOf(0) }

    // 關鍵：wordIndex 變 → 整個 SwipeStack 重建
    key(wordIndex) {
        SwipeStack(
            items = faces,
            modifier = Modifier
                .fillMaxWidth()
                .height(320.dp),
            stackCount = faces.size,
            showAllCards = true, // 一個堆疊就顯示全部卡
            onSwiped = { _, _ ->
                swipedCount += 1
                if (swipedCount >= faces.size) {
                    // 這個單字的所有卡片都滑完了
                    if (wordIndex < words.lastIndex) {
                        wordIndex += 1
                    } else {
                        // 已是最後一個單字（你可以在這裡顯示完成畫面）
                    }
                }
            },
            cardContent = { face ->
                WordCard(face = face, word = word)
            }
        )
    }
}

@Composable
private fun WordCard(
    face: WordFace,
    word: WordItem
) {
    Box(
        Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        when (face) {
            WordFace.JP ->
                Text(word.jp, style = MaterialTheme.typography.headlineLarge)

            WordFace.KANJI ->
                Text(word.kanji, style = MaterialTheme.typography.headlineLarge)

            WordFace.ZH ->
                Text(word.zh, style = MaterialTheme.typography.headlineMedium)

            WordFace.POS ->
                Text(word.pos, style = MaterialTheme.typography.titleLarge)

            WordFace.ROMAJI ->
                Text(word.romaji, style = MaterialTheme.typography.titleLarge)
        }
    }
}