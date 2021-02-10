package me.alex.pet.bookbinder.domain

import javax.xml.namespace.QName
import javax.xml.stream.Location
import javax.xml.stream.XMLEventReader
import javax.xml.stream.XMLStreamException
import javax.xml.stream.events.EndElement
import javax.xml.stream.events.StartElement
import javax.xml.stream.events.XMLEvent

private const val ATTR_STYLE = "style"
private const val ATTR_INDENT = "indent"
private const val ATTR_RULE = "rule"

private const val ELEMENT_BOOK = "book"
private const val ELEMENT_PART = "part"
private const val ELEMENT_CHAPTER = "chapter"
private const val ELEMENT_SECTION = "section"
private const val ELEMENT_RULE = "rule"
private const val ELEMENT_NAME = "name"
private const val ELEMENT_PARAGRAPH = "p"
private const val ELEMENT_EMPHASIS = "e"
private const val ELEMENT_STRONG_EMPHASIS = "s"
private const val ELEMENT_MISSPELL = "m"
private const val ELEMENT_LINK = "l"

private const val PAR_STYLE_NORMAL = "normal"
private const val PAR_STYLE_QUOTE = "quote"
private const val PAR_STYLE_FOOTNOTE = "footnote"

private val tagsToStyles = mapOf(
    ELEMENT_EMPHASIS to CharacterStyleType.EMPHASIS,
    ELEMENT_STRONG_EMPHASIS to CharacterStyleType.STRONG_EMPHASIS,
    ELEMENT_MISSPELL to CharacterStyleType.MISSPELL
)

private val styleTags = setOf(
    ELEMENT_EMPHASIS,
    ELEMENT_STRONG_EMPHASIS,
    ELEMENT_MISSPELL
)


class UnexpectedXmlException(
    msg: String,
    location: Location,
    cause: Throwable? = null
) : XMLStreamException(msg, location, cause)

private class IllegalAttributeValue(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
private class MissingAttribute(message: String, cause: Throwable? = null) : RuntimeException(message, cause)


fun XMLEventReader.parseBook(): Book {
    consumeStartDocument()
    consumeStartElement(ELEMENT_BOOK)
    val parts = mutableListOf<Part>()
    do {
        parts.add(parsePart())
        skipWhitespace()
    } while (!peek().isEndElement)
    consumeEndElement(ELEMENT_BOOK)
    consumeEndDocument()
    return parts
}

fun XMLEventReader.parsePart(): Part {
    consumeStartElement(ELEMENT_PART)
    val name = parseName().string
    val chapters = mutableListOf<Chapter>()
    do {
        chapters.add(parseChapter())
        skipWhitespace()
    } while (!peek().isEndElement)
    consumeEndElement(ELEMENT_PART)
    return Part(name, chapters)
}

fun XMLEventReader.parseChapter(): Chapter {
    consumeStartElement(ELEMENT_CHAPTER)
    val name = parseName().string
    val sections = mutableListOf<Section>()
    do {
        sections.add(parseSection())
        skipWhitespace()
    } while (!peek().isEndElement)
    consumeEndElement(ELEMENT_CHAPTER)
    return Chapter(name, sections)
}

fun XMLEventReader.parseSection(): Section {
    consumeStartElement(ELEMENT_SECTION)
    val name = parseName()
    val rules = mutableListOf<Rule>()
    do {
        rules.add(parseRule())
        skipWhitespace()
    } while (!peek().isEndElement)
    consumeEndElement(ELEMENT_SECTION)
    return Section(name, rules)
}

private fun XMLEventReader.parseName(): StyledString {
    consumeStartElement(ELEMENT_NAME)
    val name = parseStyledText()
    consumeEndElement(ELEMENT_NAME)
    return name
}

fun XMLEventReader.parseRule(): Rule {
    consumeStartElement(ELEMENT_RULE)
    val paragraphs = mutableListOf<Paragraph>()
    do {
        paragraphs.add(parseParagraph())
        skipWhitespace()
    } while (!peek().isEndElement)
    consumeEndElement(ELEMENT_RULE)
    return Rule(paragraphs)
}

fun XMLEventReader.parseParagraph(): Paragraph {
    val startElement = consumeStartElement(ELEMENT_PARAGRAPH)

    val paragraphStyle = try {
        startElement.getParagraphStyle()
    } catch (e: IllegalAttributeValue) {
        throw UnexpectedXmlException("Illegal paragraph style", startElement.location, e)
    }
    val paragraphIndentLevel = try {
        startElement.getIndentLevel()
    } catch (e: IllegalAttributeValue) {
        throw UnexpectedXmlException("Illegal indent level", startElement.location, e)
    }

    val paragraphContent = parseStyledText()

    consumeEndElement(ELEMENT_PARAGRAPH)

    return Paragraph(paragraphContent, paragraphStyle, paragraphIndentLevel)
}

private fun StartElement.getParagraphStyle(): ParagraphStyle {
    val attrStringValue = getAttributeByName(ATTR_STYLE)?.value ?: PAR_STYLE_NORMAL
    return paragraphStyleOf(attrStringValue) ?: throw IllegalAttributeValue(
        "Illegal '$ATTR_STYLE' attribute value $attrStringValue}"
    )
}

private fun StartElement.getIndentLevel(): Int {
    val attrStringValue = getAttributeByName(ATTR_INDENT)?.value ?: "0"
    val indentLevel = attrStringValue.toIntOrNull() ?: throw IllegalAttributeValue(
        "Illegal '$ATTR_INDENT' attribute value $attrStringValue, it must be an integer"
    )
    checkAttribute(indentLevel in 0..5) {
        "Illegal '$ATTR_INDENT' attribute value $indentLevel, it must be in [0..5]"
    }
    return indentLevel
}

private fun StartElement.getReferencedRuleId(): Int {
    val attr = getAttributeByName(ATTR_RULE) ?: throw MissingAttribute(
        "Missing attribute '$ATTR_RULE'"
    )
    val attrStringValue = attr.value
    val ruleId = attrStringValue.toIntOrNull() ?: throw IllegalAttributeValue(
        "Illegal '$ATTR_RULE' attribute value $attrStringValue, it must be an integer"
    )
    checkAttribute(ruleId >= 1) { "Illegal '$ATTR_RULE' attribute value $ruleId, it must be >= 1" }
    return ruleId
}

private fun StartElement.getAttributeByName(name: String) = getAttributeByName(QName.valueOf(name))

private fun checkAttribute(value: Boolean, message: () -> String = { "Illegal attribute value $value" }) {
    if (!value) {
        throw IllegalAttributeValue(message())
    }
}

private fun paragraphStyleOf(str: String): ParagraphStyle? {
    return when (str) {
        PAR_STYLE_NORMAL -> ParagraphStyle.NORMAL
        PAR_STYLE_QUOTE -> ParagraphStyle.QUOTE
        PAR_STYLE_FOOTNOTE -> ParagraphStyle.FOOTNOTE
        else -> null
    }
}

fun XMLEventReader.parseStyledText(): StyledString {
    val textBuffer = StringBuilder()
    val styles = mutableListOf<CharacterStyle>()
    val links = mutableListOf<Link>()
    do {
        val element = peek()
        when {
            element.isStyleStart -> {
                val (str, style) = parseStyledSubstring()
                styles.add(CharacterStyle(textBuffer.length, textBuffer.length + str.length, style))
                textBuffer.append(str)
            }
            element.isLinkStart -> {
                val (str, ruleId) = parseLink()
                links.add(Link(textBuffer.length, textBuffer.length + str.length, ruleId))
                textBuffer.append(str)
            }
            element.isCharacters -> textBuffer.append(readText())
            else -> throw UnexpectedXmlException("Unexpected styled text element $element", element.location)
        }
    } while (!peek().isEndElement)
    return StyledString(textBuffer.toString(), styles = styles, links = links)
}

private val XMLEvent.isStyleStart: Boolean
    get() = isStartElement && asStartElement().localName in styleTags

private val XMLEvent.isLinkStart: Boolean
    get() = isStartElement && asStartElement().localName == ELEMENT_LINK

fun XMLEventReader.parseLink(): Pair<String, Int> {
    val element = consumeStartElement(ELEMENT_LINK)
    val rule = try {
        element.getReferencedRuleId()
    } catch (e: MissingAttribute) {
        throw UnexpectedXmlException("Missing reference", element.location)
    } catch (e: IllegalAttributeValue) {
        throw UnexpectedXmlException("Illegal reference value", element.location)
    }
    val text = readText()
    consumeEndElement(ELEMENT_LINK)
    return text to rule
}

fun XMLEventReader.parseStyledSubstring(): Pair<String, CharacterStyleType> {
    val startElement = consumeStartElement()
    val style = tagsToStyles[startElement.localName] ?: throw UnexpectedXmlException(
        "Unknown style tag <${startElement.localName}>",
        startElement.location
    )
    val text = readText()
    consumeEndElement(startElement.localName)
    return text to style
}

private val StartElement.localName get() = name.localPart
private val EndElement.localName get() = name.localPart

private fun XMLEventReader.consumeStartDocument() {
    val event = nextEvent()
    if (!event.isStartDocument) {
        throw UnexpectedXmlException("A START_DOCUMENT was expected but it was ${event.eventType}", event.location)
    }
}

private fun XMLEventReader.consumeEndDocument() {
    val event = nextEvent()
    if (!event.isEndDocument) {
        throw UnexpectedXmlException("An END_DOCUMENT was expected but it was ${event.eventType}", event.location)
    }
}

private fun XMLEventReader.consumeStartElement(requiredName: String): StartElement {
    val startElement = consumeStartElement()
    if (startElement.localName != requiredName) {
        throw UnexpectedXmlException(
            "A <$requiredName> element was expected but it was <${startElement.localName}>",
            startElement.location
        )
    }
    return startElement
}

private fun XMLEventReader.consumeStartElement(): StartElement {
    skipWhitespace()
    val event = nextEvent()
    if (!event.isStartElement) {
        throw UnexpectedXmlException("A START_ELEMENT was expected but it was ${event.eventType}", event.location)
    }
    return event.asStartElement()
}

private fun XMLEventReader.consumeEndElement(requiredName: String): EndElement {
    val endElement = consumeEndElement()
    if (endElement.localName != requiredName) {
        throw UnexpectedXmlException(
            "A </$requiredName> element was expected but it was </${endElement.localName}>",
            endElement.location
        )
    }
    return endElement
}

private fun XMLEventReader.consumeEndElement(): EndElement {
    skipWhitespace()
    val event = nextEvent()
    if (!event.isEndElement) {
        throw UnexpectedXmlException("An END_ELEMENT was expected but it was ${event.eventType}", event.location)
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
    val event = nextEvent()
    if (!event.isCharacters) {
        throw UnexpectedXmlException("Text was expected but it was ${event.eventType}", event.location)
    }
    return event.asCharacters().data
}