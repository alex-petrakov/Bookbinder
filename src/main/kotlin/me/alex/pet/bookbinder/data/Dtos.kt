package me.alex.pet.bookbinder.data

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import me.alex.pet.bookbinder.domain.*

@JsonClass(generateAdapter = true)
data class MarkupJson(
    val paragraphSpans: List<ParagraphSpanJson>,
    val indentSpans: List<IndentSpanJson>,
    val characterSpans: List<CharacterSpanJson>,
    val linkSpans: List<LinkSpanJson>
)

@JsonClass(generateAdapter = true)
data class ParagraphSpanJson(val start: Int, val end: Int, val style: ParagraphSpanStyleJson)

enum class ParagraphSpanStyleJson {
    QUOTE,
    FOOTNOTE
}

@JsonClass(generateAdapter = true)
data class IndentSpanJson(val start: Int, val end: Int, val level: Int)

@JsonClass(generateAdapter = true)
data class CharacterSpanJson(val start: Int, val end: Int, val style: CharacterSpanStyleJson)

enum class CharacterSpanStyleJson {
    EMPHASIS,
    STRONG_EMPHASIS,
    MISSPELL
}

@JsonClass(generateAdapter = true)
data class LinkSpanJson(val start: Int, val end: Int, val ruleId: Int)


fun MarkupJson.asString(moshi: Moshi): String {
    val jsonAdapter = moshi.adapter(MarkupJson::class.java)
    return jsonAdapter.toJson(this)
}

fun SpannedText.toJson(): MarkupJson {
    return MarkupJson(
        paragraphSpans.map { it.toJson() },
        indentSpans.map { it.toJson() },
        characterSpans.map { it.toJson() },
        linkSpans.map { it.toJson() }
    )
}

private fun ParagraphSpan.toJson(): ParagraphSpanJson {
    return ParagraphSpanJson(start, end, style.toJson())
}

private fun ParagraphSpanStyle.toJson(): ParagraphSpanStyleJson {
    return when (this) {
        ParagraphSpanStyle.QUOTE -> ParagraphSpanStyleJson.QUOTE
        ParagraphSpanStyle.FOOTNOTE -> ParagraphSpanStyleJson.FOOTNOTE
    }
}

private fun IndentSpan.toJson(): IndentSpanJson {
    return IndentSpanJson(start, end, level)
}

private fun CharacterSpan.toJson(): CharacterSpanJson {
    return CharacterSpanJson(start, end, style.toJson())
}

private fun CharacterSpanStyle.toJson(): CharacterSpanStyleJson {
    return when (this) {
        CharacterSpanStyle.EMPHASIS -> CharacterSpanStyleJson.EMPHASIS
        CharacterSpanStyle.STRONG_EMPHASIS -> CharacterSpanStyleJson.STRONG_EMPHASIS
        CharacterSpanStyle.MISSPELL -> CharacterSpanStyleJson.MISSPELL
    }
}

private fun LinkSpan.toJson(): LinkSpanJson {
    return LinkSpanJson(start, end, ruleId)
}
