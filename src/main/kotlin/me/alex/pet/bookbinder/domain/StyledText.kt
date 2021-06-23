package me.alex.pet.bookbinder.domain

data class StyledText(
    val string: String,
    val paragraphSpans: List<ParagraphSpan> = emptyList(),
    val characterSpans: List<CharacterSpan> = emptyList(),
    val linkSpans: List<LinkSpan> = emptyList()
)

sealed class ParagraphSpan(val start: Int, val end: Int) {

    init {
        require(start >= 0)
        require(end > start)
    }

    class Indent(start: Int, end: Int, val level: Int, val hangingText: String) : ParagraphSpan(start, end) {
        init {
            require(level >= 0)
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Indent) return false
            if (!super.equals(other)) return false

            if (level != other.level) return false
            if (hangingText != other.hangingText) return false

            return true
        }

        override fun hashCode(): Int {
            var result = super.hashCode()
            result = 31 * result + level
            result = 31 * result + hangingText.hashCode()
            return result
        }

        override fun toString(): String {
            return "Indent(start=$start, end=$end, level=$level, hangingText='$hangingText')"
        }
    }

    class Style(start: Int, end: Int, val appearance: ParagraphAppearance) : ParagraphSpan(start, end) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Style) return false
            if (!super.equals(other)) return false

            if (appearance != other.appearance) return false

            return true
        }

        override fun hashCode(): Int {
            var result = super.hashCode()
            result = 31 * result + appearance.hashCode()
            return result
        }

        override fun toString(): String {
            return "Style(start=$start, end=$end, appearance=$appearance)"
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is ParagraphSpan) return false

        if (start != other.start) return false
        if (end != other.end) return false

        return true
    }

    override fun hashCode(): Int {
        var result = start
        result = 31 * result + end
        return result
    }
}

enum class ParagraphAppearance {
    QUOTE,
    FOOTNOTE
}

data class CharacterSpan(val start: Int, val end: Int, val appearance: CharacterAppearance)

enum class CharacterAppearance {
    EMPHASIS,
    STRONG_EMPHASIS,
    MISSPELL
}

data class LinkSpan(val start: Int, val end: Int, val ruleId: Int)


fun StyledString.toStyledText(): StyledText {
    val linkSpans = links.map { it.toLinkSpan() }
    val characterSpans = styles.map { it.toCharacterSpan() }
    return StyledText(this.string, characterSpans = characterSpans, linkSpans = linkSpans)
}

private fun Link.toLinkSpan(offset: Int = 0) = LinkSpan(offset + start, offset + end, ruleId)

private fun CharacterStyle.toCharacterSpan(offset: Int = 0): CharacterSpan {
    val style = when (styleType) {
        CharacterStyleType.EMPHASIS -> CharacterAppearance.EMPHASIS
        CharacterStyleType.STRONG_EMPHASIS -> CharacterAppearance.STRONG_EMPHASIS
        CharacterStyleType.MISSPELL -> CharacterAppearance.MISSPELL
    }
    return CharacterSpan(offset + start, offset + end, style)
}

fun List<Paragraph>.toStyledText(paragraphDelimiter: String = "\n\n"): StyledText {
    val textBuffer = StringBuilder()
    val allParagraphSpans = mutableListOf<ParagraphSpan>()
    val allCharacterSpans = mutableListOf<CharacterSpan>()
    val allLinkSpans = mutableListOf<LinkSpan>()

    forEach { paragraph ->
        allParagraphSpans.addAll(paragraph.toParagraphSpans(textBuffer.length))

        val (paragraphText, styles, links) = paragraph.content
        val linkSpans = links.map { it.toLinkSpan(textBuffer.length) }
        allLinkSpans.addAll(linkSpans)

        val characterSpans = styles.map { it.toCharacterSpan(textBuffer.length) }
        allCharacterSpans.addAll(characterSpans)

        textBuffer.append(paragraphText).append(paragraphDelimiter)
    }

    return StyledText(
        textBuffer.toString(),
        allParagraphSpans,
        allCharacterSpans,
        allLinkSpans
    )
}

private fun Paragraph.toParagraphSpans(offset: Int): List<ParagraphSpan> {
    val outerIndent = toOuterIndentSpan(offset)
    val styles = toStyleSpans(offset)
    val innerIndent = toInnerIndentSpan(offset)
    return listOfNotNull(outerIndent) + styles + listOfNotNull(innerIndent)
}

private fun Paragraph.toStyleSpans(offset: Int): List<ParagraphSpan.Style> {
    val appearanceList = when (style) {
        ParagraphStyle.NORMAL -> emptyList()
        ParagraphStyle.QUOTE -> listOf(ParagraphAppearance.QUOTE)
        ParagraphStyle.FOOTNOTE -> listOf(ParagraphAppearance.FOOTNOTE)
        ParagraphStyle.FOOTNOTE_QUOTE -> listOf(ParagraphAppearance.FOOTNOTE, ParagraphAppearance.QUOTE)
    }
    return appearanceList.map { appearance ->
        ParagraphSpan.Style(offset, offset + content.string.length, appearance)
    }
}

private fun Paragraph.toOuterIndentSpan(offset: Int): ParagraphSpan.Indent? {
    return when {
        outerIndentLevel > 0 -> ParagraphSpan.Indent(
            offset,
            offset + content.string.length,
            outerIndentLevel,
            ""
        )
        else -> null
    }
}

private fun Paragraph.toInnerIndentSpan(offset: Int): ParagraphSpan.Indent? {
    return when {
        innerIndentLevel > 0 -> ParagraphSpan.Indent(
            offset,
            offset + content.string.length,
            innerIndentLevel,
            ""
        )
        else -> null
    }
}