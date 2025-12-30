package com.rainbowt.swipestackcard.extension

import android.content.Context
import com.rainbowt.swipestackcard.model.WordItem
import kotlinx.serialization.json.Json
import kotlinx.serialization.decodeFromString

private val json = Json {
    ignoreUnknownKeys = true
    isLenient = true
}

fun loadWordsFromAssets(context: Context, fileName: String = "第 1 課.json"): List<WordItem> {
    val jsonText = context.assets.open(fileName).bufferedReader().use { it.readText() }
    return json.decodeFromString(jsonText)
}