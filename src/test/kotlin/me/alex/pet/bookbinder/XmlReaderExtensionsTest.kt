package me.alex.pet.bookbinder

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import javax.xml.stream.XMLInputFactory

@DisplayName("Parser")
class XmlReaderExtensionsTest {

    private val factory = XMLInputFactory.newInstance()

    @ParameterizedTest
    @ValueSource(strings = ["\n", " ", "Emphasized text", " Emphasized text "])
    fun `handles emphasis`(emphasizedText: String) {
        val input = "<e>$emphasizedText</e>".byteInputStream()
        val reader = factory.createXMLEventReader(input).apply {
            nextEvent() // Skip the START_DOCUMENT event
        }

        val output = reader.parseStyledSubstring()

        assertThat(output).isEqualTo(emphasizedText to StyleType.EMPHASIS)
    }

    @ParameterizedTest
    @ValueSource(strings = ["\n", " ", "Emphasized text", " Emphasized text "])
    fun `handles strong emphasis`(emphasizedText: String) {
        val input = "<s>$emphasizedText</s>".byteInputStream()
        val reader = factory.createXMLEventReader(input).apply {
            nextEvent() // Skip the START_DOCUMENT event
        }

        val output = reader.parseStyledSubstring()

        assertThat(output).isEqualTo(emphasizedText to StyleType.STRONG_EMPHASIS)
    }

    @ParameterizedTest
    @ValueSource(strings = ["\n", " ", "Emphasized text", " Emphasized text "])
    fun `handles misspell style`(emphasizedText: String) {
        val input = "<m>$emphasizedText</m>".byteInputStream()
        val reader = factory.createXMLEventReader(input).apply {
            nextEvent() // Skip the START_DOCUMENT event
        }

        val output = reader.parseStyledSubstring()

        assertThat(output).isEqualTo(emphasizedText to StyleType.MISSPELL)
    }

    @Test
    fun `does not allow empty styled text`() {
        val input = "<e></e>".byteInputStream()
        val reader = factory.createXMLEventReader(input).apply {
            nextEvent() // Skip the START_DOCUMENT event
        }

        assertThrows<RuntimeException> {
            reader.parseStyledSubstring()
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["\n", " ", "Emphasized text", " Emphasized text "])
    fun `handles links`(emphasizedText: String) {
        val input = "<l rule=\"1\">$emphasizedText</l>".byteInputStream()
        val reader = factory.createXMLEventReader(input).apply {
            nextEvent() // Skip the START_DOCUMENT event
        }

        val output = reader.parseLink()

        assertThat(output).isEqualTo(emphasizedText to 1)
    }

    @Test
    fun `does not allow links to nowhere`() {
        val input = "<l>Emphasized text</l>".byteInputStream()
        val reader = factory.createXMLEventReader(input).apply {
            nextEvent() // Skip the START_DOCUMENT event
        }

        assertThrows<RuntimeException> {
            reader.parseLink()
        }
    }

    @Test
    fun `does not allow links with a string value`() {
        val input = "<l rule=\"str\">Emphasized text</l>".byteInputStream()
        val reader = factory.createXMLEventReader(input).apply {
            nextEvent() // Skip the START_DOCUMENT event
        }

        assertThrows<RuntimeException> {
            reader.parseLink()
        }
    }

    @Test
    fun `does not allow empty links`() {
        val input = "<l rule=\"1\"></l>".byteInputStream()
        val reader = factory.createXMLEventReader(input).apply {
            nextEvent() // Skip the START_DOCUMENT event
        }

        assertThrows<RuntimeException> {
            reader.parseLink()
        }
    }
}