package com.example.whoareyou.util

object TextCleaner{
    fun cleanText(rawText: String): String{
        return rawText.replace(Regex("""(?:https?://|www\.)\S+"""), "ссылка")
            .replace(Regex("""(?:\p{InEmoticons})|\p{InMiscellaneous_Symbols_And_Pictographs}|\p{InTransport_And_Map_Symbols}|
                |\p{InSupplemental_Symbols_And_Pictographs}|\p{InDingbats}|\p{InVariation_Selectors}""".trimMargin()), "")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }
}

