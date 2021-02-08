package me.alex.pet.bookbinder

typealias Book = List<Part>

data class Part(
    val name: String,
    val chapters: List<Chapter>
)

data class Chapter(
    val name: String,
    val sections: List<Section>
)

data class Section(
    val name: StyledString,
    val rules: List<Rule>
)

data class Rule(
    val paragraphs: List<Paragraph>
)

data class Paragraph(
    val content: StyledString,
    val style: ParagraphStyle = ParagraphStyle.NORMAL,
    val indentLevel: Int = 0
)

enum class ParagraphStyle {
    NORMAL,
    QUOTE,
    FOOTNOTE
}

data class StyledString(
    val string: String,
    val styles: List<CharacterStyle> = emptyList(),
    val links: List<Link> = emptyList()
)

data class CharacterStyle(val start: Int, val end: Int, val styleType: StyleType) {
    companion object {
        fun emphasis(start: Int, end: Int) = CharacterStyle(start, end, StyleType.EMPHASIS)
        fun strongEmphasis(start: Int, end: Int) = CharacterStyle(start, end, StyleType.STRONG_EMPHASIS)
        fun misspell(start: Int, end: Int) = CharacterStyle(start, end, StyleType.MISSPELL)
    }
}

enum class StyleType {
    EMPHASIS,
    STRONG_EMPHASIS,
    MISSPELL
}

data class Link(val start: Int, val end: Int, val sectionId: Int)