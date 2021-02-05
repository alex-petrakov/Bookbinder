package me.alex.pet.bookbinder

import javax.xml.stream.XMLEventReader
import javax.xml.stream.events.Attribute
import javax.xml.stream.events.EndElement
import javax.xml.stream.events.StartElement

private const val ATTR_RULE = "rule"
private const val ELEMENT_EMPHASIS = "e"
private const val ELEMENT_STRONG_EMPHASIS = "s"
private const val ELEMENT_MISSPELL = "m"
private const val ELEMENT_LINK = "l"

private val tagsToStyles = mapOf(
    ELEMENT_EMPHASIS to StyleType.EMPHASIS,
    ELEMENT_STRONG_EMPHASIS to StyleType.STRONG_EMPHASIS,
    ELEMENT_MISSPELL to StyleType.MISSPELL
)


fun XMLEventReader.parseLink(): Pair<String, Int> {
    val element = consumeStartElement(ELEMENT_LINK)
    val attributes = element.asStartElement().attributeMap
    val rule = attributes[ATTR_RULE]?.toIntOrNull() ?: throw RuntimeException()
    if (rule < 1) {
        throw RuntimeException()
    }
    val text = readText()
    consumeEndElement(ELEMENT_LINK)
    return text to rule
}

private val StartElement.attributeMap get() = attributes.toMap()

private fun Iterator<Attribute>.toMap(): Map<String, String> {
    return asSequence().associate { it.name.localPart to it.value }
}

fun XMLEventReader.parseStyledSubstring(): Pair<String, StyleType> {
    val startElement = consumeStartElement()
    val style = tagsToStyles[startElement.localName] ?: throw RuntimeException()
    val text = readText()
    consumeEndElement(startElement.localName)
    return text to style
}

private val StartElement.localName get() = name.localPart

private fun XMLEventReader.consumeStartElement(requiredName: String): StartElement {
    val startElement = consumeStartElement()
    if (startElement.name.localPart != requiredName) {
        throw RuntimeException()
    }
    return startElement
}

private fun XMLEventReader.consumeStartElement(): StartElement {
    skipWhitespace()
    val event = nextEvent()
    if (!event.isStartElement) {
        throw RuntimeException()
    }
    return event.asStartElement()
}

private fun XMLEventReader.consumeEndElement(requiredName: String): EndElement {
    val endElement = consumeEndElement()
    if (endElement.name.localPart != requiredName) {
        throw RuntimeException()
    }
    return endElement
}

private fun XMLEventReader.consumeEndElement(): EndElement {
    skipWhitespace()
    val event = nextEvent()
    if (!event.isEndElement) {
        throw RuntimeException()
    }
    return event.asEndElement()
}

private fun XMLEventReader.skipWhitespace() {
    var event = peek()
    while (event.isCharacters && event.asCharacters().isWhiteSpace) {
        nextEvent()
        event = peek()
    }
}

private fun XMLEventReader.readText(): String {
    if (!peek().isCharacters) {
        throw RuntimeException()
    }
    return nextEvent().asCharacters().data
}