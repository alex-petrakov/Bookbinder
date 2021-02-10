package me.alex.pet.bookbinder.domain

data class SpannedText(
    val string: String,
    val paragraphSpans: List<ParagraphSpan> = emptyList(),
    val indentSpans: List<IndentSpan> = emptyList(),
    val characterSpans: List<CharacterSpan> = emptyList(),
    val linkSpans: List<LinkSpan> = emptyList()
)

data class ParagraphSpan(val start: Int, val end: Int, val style: ParagraphSpanStyle)

enum class ParagraphSpanStyle {
    QUOTE,
    FOOTNOTE
}

data class IndentSpan(val start: Int, val end: Int, val level: Int)

data class CharacterSpan(val start: Int, val end: Int, val style: CharacterSpanStyle)

enum class CharacterSpanStyle {
    EMPHASIS,
    STRONG_EMPHASIS,
    MISSPELL
}

data class LinkSpan(val start: Int, val end: Int, val ruleId: Int)


fun StyledString.toSpannedText(): SpannedText {
    val linkSpans = links.map { it.toLinkSpan() }
    val characterSpans = styles.map { it.toCharacterSpan() }
    return SpannedText(this.string, characterSpans = characterSpans, linkSpans = linkSpans)
}

private fun Link.toLinkSpan(offset: Int = 0) = LinkSpan(offset + start, offset + end, ruleId)

private fun CharacterStyle.toCharacterSpan(offset: Int = 0): CharacterSpan {
    val style = when (styleType) {
        CharacterStyleType.EMPHASIS -> CharacterSpanStyle.EMPHASIS
        CharacterStyleType.STRONG_EMPHASIS -> CharacterSpanStyle.STRONG_EMPHASIS
        CharacterStyleType.MISSPELL -> CharacterSpanStyle.MISSPELL
    }
    return CharacterSpan(offset + start, offset + end, style)
}

fun List<Paragraph>.toSpannedText(paragraphDelimiter: String = "\n\n"): SpannedText {
    val textBuffer = StringBuilder()
    val allParagraphSpans = mutableListOf<ParagraphSpan>()
    val allIndentSpans = mutableListOf<IndentSpan>()
    val allCharacterSpans = mutableListOf<CharacterSpan>()
    val allLinkSpans = mutableListOf<LinkSpan>()

    forEach { paragraph ->
        allIndentSpans.addAll(paragraph.toIndentSpans(textBuffer.length))
        allParagraphSpans.addAll(paragraph.toParagraphSpans(textBuffer.length))

        val (paragraphText, styles, links) = paragraph.content
        val linkSpans = links.map { it.toLinkSpan(textBuffer.length) }
        allLinkSpans.addAll(linkSpans)

        val characterSpans = styles.map { it.toCharacterSpan(textBuffer.length) }
        allCharacterSpans.addAll(characterSpans)

        textBuffer.append(paragraphText).append(paragraphDelimiter)
    }

    return SpannedText(textBuffer.toString(), allParagraphSpans, allIndentSpans, allCharacterSpans, allLinkSpans)
}

private fun Paragraph.toParagraphSpans(offset: Int): List<ParagraphSpan> {
    val spanStyle = when (style) {
        ParagraphStyle.NORMAL -> null
        ParagraphStyle.QUOTE -> ParagraphSpanStyle.QUOTE
        ParagraphStyle.FOOTNOTE -> ParagraphSpanStyle.FOOTNOTE
    }
    return when (spanStyle) {
        null -> emptyList()
        else -> listOf(ParagraphSpan(offset, offset + content.string.length, spanStyle))
    }
}

private fun Paragraph.toIndentSpans(offset: Int): List<IndentSpan> {
    return when {
        indentLevel > 0 -> listOf(IndentSpan(offset, offset + content.string.length, indentLevel))
        else -> emptyList()
    }
}