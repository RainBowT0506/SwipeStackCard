package com.rainbowt.swipestackcard.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class WordItem(
    @SerialName("日文") val jp: String,
    @SerialName("漢字") val kanji: String,
    @SerialName("中文") val zh: String,
    @SerialName("詞性") val pos: String,
    @SerialName("羅馬拼音") val romaji: String
)