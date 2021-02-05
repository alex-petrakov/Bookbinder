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
    val content: StyledString,
)

data class StyledString(
    val string: String,
    val styles: List<Style> = emptyList(),
    val indents: List<Indent> = emptyList(),
    val links: List<Link> = emptyList()
)

fun String.toStyledString(): StyledString = StyledString(this)

data class Style(val start: Int, val end: Int, val styleType: StyleType)

enum class StyleType {
    EMPHASIS,
    STRONG_EMPHASIS,
    MISSPELL,
    QUOTE
}

data class Indent(val start: Int, val end: Int, val level: Int)

data class Link(val start: Int, val end: Int, val sectionId: Int)