package me.alex.pet.bookbinder.domain

data class StyledText(
    val string: String,
    val paragraphSpans: List<ParagraphSpan> = emptyList(),
    val characterSpans: List<CharacterSpan> = emptyList(),
    val linkSpans: List<LinkSpan> = emptyList()
)

data class ParagraphSpan(
    val start: Int,
    val end: Int,
    val appearance: ParagraphAppearance,
    val indent: Indent
) {
    init {
        require(start >= 0)
        require(end > start)
    }
}

data class Indent(val outer: Int, val inner: Int, val hangingText: String) {
    init {
        require(outer >= 0)
        require(inner >= 0)
    }
}

enum class ParagraphAppearance {
    NORMAL,
    FOOTNOTE,
    QUOTE,
    FOOTNOTE_QUOTE
}

data class CharacterSpan(val start: Int, val end: Int, val appearance: CharacterAppearance)

enum class CharacterAppearance {
    EMPHASIS,
    STRONG_EMPHASIS,
    MISSPELL
}

data class LinkSpan(val start: Int, val end: Int, val ruleId: Int)


fun StyledString.toStyledText(): StyledText {
    return StyledText(this.string, characterSpans = toCharacterSpans(), linkSpans = toLinkSpans())
}

private fun StyledString.toLinkSpans(paragraphOffset: Int = 0): List<LinkSpan> {
    return links.map { link ->
        LinkSpan(paragraphOffset + link.start, paragraphOffset + link.end, link.ruleId)
    }
}

private fun StyledString.toCharacterSpans(paragraphOffset: Int = 0): List<CharacterSpan> {
    return styles.map { style ->
        val appearance = when (style.styleType) {
            CharacterStyleType.EMPHASIS -> CharacterAppearance.EMPHASIS
            CharacterStyleType.STRONG_EMPHASIS -> CharacterAppearance.STRONG_EMPHASIS
            CharacterStyleType.MISSPELL -> CharacterAppearance.MISSPELL
        }
        CharacterSpan(paragraphOffset + style.start, paragraphOffset + style.end, appearance)
    }
}

fun List<Paragraph>.splitWithBlankLines(): List<Paragraph> {
    if (this.isEmpty()) return emptyList()
    return this.zipWithNext { current, next ->
        val blankLineStyle = getIntermediateLineStyle(current.style, next.style)
        val blankLine = Paragraph(
            StyledString(""),
            blankLineStyle,
            current.outerIndentLevel,
            current.innerIndentLevel,
            ""
        )
        listOf(current, blankLine)
    }.flatten() + this.last()
}

private fun getIntermediateLineStyle(first: ParagraphStyle, second: ParagraphStyle): ParagraphStyle {
    return if (first == second) {
        first
    } else if (first == ParagraphStyle.FOOTNOTE && second == ParagraphStyle.FOOTNOTE_QUOTE) {
        ParagraphStyle.FOOTNOTE
    } else if (first == ParagraphStyle.FOOTNOTE_QUOTE && second == ParagraphStyle.FOOTNOTE) {
        ParagraphStyle.FOOTNOTE
    } else {
        ParagraphStyle.NORMAL
    }
}

fun List<Paragraph>.toStyledText(): StyledText {
    val textBuffer = StringBuilder()
    val allParagraphSpans = mutableListOf<ParagraphSpan>()
    val allCharacterSpans = mutableListOf<CharacterSpan>()
    val allLinkSpans = mutableListOf<LinkSpan>()

    for (paragraph in this) {
        allParagraphSpans.add(paragraph.toParagraphSpan(textBuffer.length))
        allLinkSpans.addAll(paragraph.content.toLinkSpans(textBuffer.length))
        allCharacterSpans.addAll(paragraph.content.toCharacterSpans(textBuffer.length))
        textBuffer.append(paragraph.content.string).append("\n")
    }

    return StyledText(textBuffer.toString(), allParagraphSpans, allCharacterSpans, allLinkSpans)
}

private fun Paragraph.toParagraphSpan(offset: Int): ParagraphSpan {
    val appearance = when (style) {
        ParagraphStyle.NORMAL -> ParagraphAppearance.NORMAL
        ParagraphStyle.QUOTE -> ParagraphAppearance.QUOTE
        ParagraphStyle.FOOTNOTE -> ParagraphAppearance.FOOTNOTE
        ParagraphStyle.FOOTNOTE_QUOTE -> ParagraphAppearance.FOOTNOTE_QUOTE
    }
    val indent = Indent(outerIndentLevel, innerIndentLevel, hangingText)
    return ParagraphSpan(offset, offset + content.string.length + 1, appearance, indent)
}