package me.alex.pet.bookbinder.data

import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import me.alex.pet.bookbinder.domain.*

@JsonClass(generateAdapter = true)
data class MarkupDto(
    val paragraphSpans: List<ParagraphSpanDto>,
    val characterSpans: List<CharacterSpanDto>,
    val linkSpans: List<LinkSpanDto>
)

@JsonClass(generateAdapter = true)
data class ParagraphSpanDto(
    val start: Int,
    val end: Int,
    val appearance: ParagraphAppearanceDto,
    val indent: IndentDto
)

@JsonClass(generateAdapter = true)
data class IndentDto(
    val outer: Int,
    val inner: Int,
    val hangingText: String
)

enum class ParagraphAppearanceDto {
    NORMAL,
    FOOTNOTE,
    QUOTE,
    FOOTNOTE_QUOTE
}

@JsonClass(generateAdapter = true)
data class CharacterSpanDto(
    val start: Int,
    val end: Int,
    val appearance: CharacterAppearanceDto
)

enum class CharacterAppearanceDto {
    EMPHASIS,
    STRONG_EMPHASIS,
    MISSPELL
}

@JsonClass(generateAdapter = true)
data class LinkSpanDto(
    val start: Int,
    val end: Int,
    val ruleId: Int
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
