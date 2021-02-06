package me.alex.pet.bookbinder

import com.github.kittinunf.result.Result
import java.io.InputStream
import javax.xml.stream.XMLEventReader
import javax.xml.stream.XMLInputFactory

class Parser {

    private val factory = XMLInputFactory.newInstance()

    fun parse(inputStream: InputStream): Result<Book, RuntimeException> {
        val reader = obtainEventReader(inputStream)
        return Result.of<Book, RuntimeException> { readDocument(reader) }.also {
            reader.close()
        }
    }

    private fun readDocument(reader: XMLEventReader): Book {
        reader.consumeStartDocument()
        val book = readBook(reader)
        reader.consumeEndDocument()
        return book
    }

    private fun readBook(reader: XMLEventReader): Book {
        val parts = mutableListOf<Part>()
        reader.consumeStartElement(BOOK)
        do {
            parts.add(readPart(reader))
            reader.skipWhitespace()
        } while (!reader.peek().isEndElement)
        reader.consumeEndElement(BOOK)
        return parts
    }

    private fun readPart(reader: XMLEventReader): Part {
        reader.consumeStartElement(PART)
        val name = readName(reader)
        val chapters = mutableListOf<Chapter>()
        do {
            chapters.add(readChapter(reader))
            reader.skipWhitespace()
        } while (!reader.peek().isEndElement)
        reader.consumeEndElement(PART)
        return Part(name, chapters)
    }

    private fun readChapter(reader: XMLEventReader): Chapter {
        reader.consumeStartElement(CHAPTER)
        val name = readName(reader)
        val sections = mutableListOf<Section>()
        do {
            sections.add(readSection(reader))
            reader.skipWhitespace()
        } while (!reader.peek().isEndElement)
        reader.consumeEndElement(CHAPTER)
        return Chapter(name, sections)
    }

    private fun readSection(reader: XMLEventReader): Section {
        reader.consumeStartElement(SECTION)
        val name = readName(reader)
        val rules = mutableListOf<Rule>()
        do {
            rules.add(readRule(reader))
            reader.skipWhitespace()
        } while (!reader.peek().isEndElement)
        reader.consumeEndElement(SECTION)
        return Section(name.toStyledString(), rules)
    }

    private fun readRule(reader: XMLEventReader): Rule {
        reader.consumeStartElement(RULE)
        val paragraphs = mutableListOf<StyledString>()
        do {
            paragraphs.add(readParagraph(reader))
            reader.skipWhitespace()
        } while (!reader.peek().isEndElement)
        reader.consumeEndElement(RULE)
        return Rule(emptyList())
    }

    private fun readParagraph(reader: XMLEventReader): StyledString {
        reader.consumeStartElement(PARAGRAPH)
        reader.consumeEndElement(PARAGRAPH)
        return "".toStyledString()
    }

    private fun readName(reader: XMLEventReader): String {
        reader.consumeStartElement(NAME)
        val name = reader.readText()
        reader.consumeEndElement(NAME)
        return name
    }

    private fun obtainEventReader(input: InputStream) = factory.createXMLEventReader(input)
}


private fun XMLEventReader.skipWhitespace() {
    var event = peek()
    while (event.isCharacters && event.asCharacters().isWhiteSpace) {
        nextEvent()
        event = peek()
    }
}

private fun XMLEventReader.consumeStartDocument() {
    if (!peek().isStartDocument) {
        throw RuntimeException()
    }
    nextEvent()
}

private fun XMLEventReader.consumeEndDocument() {
    if (!peek().isEndDocument) {
        throw RuntimeException()
    }
    nextEvent()
}

private fun XMLEventReader.consumeStartElement(requiredName: String) {
    skipWhitespace()
    val event = peek()
    if (!event.isStartElement || event.asStartElement().name.localPart != requiredName) {
        throw RuntimeException()
    }
    nextEvent()
}

private fun XMLEventReader.consumeEndElement(requiredName: String) {
    skipWhitespace()
    val event = peek()
    if (!event.isEndElement || event.asEndElement().name.localPart != requiredName) {
        throw RuntimeException()
    }
    nextEvent()
}

private fun XMLEventReader.readText(): String {
    val event = nextEvent()
    if (!event.isCharacters) {
        throw RuntimeException()
    }
    return event.asCharacters().data.trim()
}


private const val BOOK = "book"
private const val PART = "part"
private const val CHAPTER = "chapter"
private const val SECTION = "section"
private const val RULE = "rule"
private const val NAME = "name"
private const val PARAGRAPH = "p"