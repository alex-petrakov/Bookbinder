package me.alex.pet.bookbinder.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import me.alex.pet.bookbinder.domain.*

@JsonClass(generateAdapter = true)
data class MarkupDto(
    @Json(name = "ps") val paragraphSpans: List<ParagraphSpanDto>,
    @Json(name = "cs") val characterSpans: List<CharacterSpanDto>,
    @Json(name = "ls") val linkSpans: List<LinkSpanDto>
)

@JsonClass(generateAdapter = true)
data class ParagraphSpanDto(
    @Json(name = "s") val start: Int,
    @Json(name = "e") val end: Int,
    @Json(name = "a") val appearance: ParagraphAppearanceDto,
    @Json(name = "i") val indent: IndentDto
)

@JsonClass(generateAdapter = true)
data class IndentDto(
    @Json(name = "o") val outer: Int,
    @Json(name = "i") val inner: Int,
    @Json(name = "ht") val hangingText: String
)

enum class ParagraphAppearanceDto {
    NORMAL,
    FOOTNOTE,
    QUOTE,
    FOOTNOTE_QUOTE
}

@JsonClass(generateAdapter = true)
data class CharacterSpanDto(
    @Json(name = "s") val start: Int,
    @Json(name = "e") val end: Int,
    @Json(name = "a") val appearance: CharacterAppearanceDto
)

enum class CharacterAppearanceDto {
    EMPHASIS,
    STRONG_EMPHASIS,
    MISSPELL
}

@JsonClass(generateAdapter = true)
data class LinkSpanDto(
    @Json(name = "s") val start: Int,
    @Json(name = "e") val end: Int,
    @Json(name = "ri") val ruleId: Int
)


fun MarkupDto.asString(moshi: Moshi): String {
    val jsonAdapter = moshi.adapter(MarkupDto::class.java)
    return jsonAdapter.toJson(this)
}

fun StyledText.toJson(): MarkupDto {
    return MarkupDto(
        paragraphSpans.map { it.toJson() },
        characterSpans.map { it.toJson() },
        linkSpans.map { it.toJson() }
    )
}

private fun ParagraphSpan.toJson(): ParagraphSpanDto {
    return ParagraphSpanDto(start, end, appearance.toJson(), indent.toJson())
}

private fun ParagraphAppearance.toJson(): ParagraphAppearanceDto {
    return when (this) {
        ParagraphAppearance.NORMAL -> ParagraphAppearanceDto.NORMAL
        ParagraphAppearance.FOOTNOTE -> ParagraphAppearanceDto.FOOTNOTE
        ParagraphAppearance.QUOTE -> ParagraphAppearanceDto.QUOTE
        ParagraphAppearance.FOOTNOTE_QUOTE -> ParagraphAppearanceDto.FOOTNOTE_QUOTE
    }
}

private fun Indent.toJson(): IndentDto {
    return IndentDto(outer, inner, hangingText)
}

private fun CharacterSpan.toJson(): CharacterSpanDto {
    return CharacterSpanDto(start, end, appearance.toJson())
}

private fun CharacterAppearance.toJson(): CharacterAppearanceDto {
    return when (this) {
        CharacterAppearance.EMPHASIS -> CharacterAppearanceDto.EMPHASIS
        CharacterAppearance.STRONG_EMPHASIS -> CharacterAppearanceDto.STRONG_EMPHASIS
        CharacterAppearance.MISSPELL -> CharacterAppearanceDto.MISSPELL
    }
}

private fun LinkSpan.toJson(): LinkSpanDto {
    return LinkSpanDto(start, end, ruleId)
}
