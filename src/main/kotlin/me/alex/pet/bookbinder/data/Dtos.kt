package me.alex.pet.bookbinder.data

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import me.alex.pet.bookbinder.domain.*

@JsonClass(generateAdapter = true)
data class MarkupDto(
    val paragraphSpans: ParagraphSpansDto,
    val characterSpans: List<CharacterSpanDto>,
    val linkSpans: List<LinkSpanDto>
)

@JsonClass(generateAdapter = true)
data class ParagraphSpansDto(
    val indents: List<IndentSpanDto>,
    val styles: List<ParagraphStyleDto>
)

interface Sortable {
    val globalOrder: Int
}

@JsonClass(generateAdapter = true)
data class IndentSpanDto(
    val start: Int,
    val end: Int,
    val level: Int,
    val hangingText: String,
    override val globalOrder: Int
) : Sortable

@JsonClass(generateAdapter = true)
data class ParagraphStyleDto(
    val start: Int,
    val end: Int,
    // TODO: remove @Json annotation once JSON fields are renamed from 'style' to 'appearance'
    @Json(name = "style") val appearance: ParagraphAppearanceDto,
    override val globalOrder: Int
) : Sortable

enum class ParagraphAppearanceDto {
    QUOTE,
    FOOTNOTE
}

@JsonClass(generateAdapter = true)
data class CharacterSpanDto(
    val start: Int,
    val end: Int,
    // TODO: remove @Json annotation once JSON fields are renamed from 'style' to 'appearance'
    @Json(name = "style") val appearance: CharacterAppearanceDto
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
        paragraphSpans.toJson(),
        characterSpans.map { it.toJson() },
        linkSpans.map { it.toJson() }
    )
}

private fun List<ParagraphSpan>.toJson(): ParagraphSpansDto {
    val allSpans = this.withIndex()
    val indents = mutableListOf<IndentSpanDto>()
    val styles = mutableListOf<ParagraphStyleDto>()
    for ((index, span) in allSpans) {
        when (span) {
            is ParagraphSpan.Indent -> indents.add(span.toJson(index))
            is ParagraphSpan.Style -> styles.add(span.toJson(index))
        }
    }
    return ParagraphSpansDto(indents.toList(), styles.toList())
}

private fun ParagraphSpan.Style.toJson(index: Int): ParagraphStyleDto {
    val appearance = when (this.appearance) {
        ParagraphAppearance.QUOTE -> ParagraphAppearanceDto.QUOTE
        ParagraphAppearance.FOOTNOTE -> ParagraphAppearanceDto.FOOTNOTE
    }
    return ParagraphStyleDto(start, end, appearance, index)
}

private fun ParagraphSpan.Indent.toJson(index: Int): IndentSpanDto {
    return IndentSpanDto(start, end, level, hangingText, index)
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
