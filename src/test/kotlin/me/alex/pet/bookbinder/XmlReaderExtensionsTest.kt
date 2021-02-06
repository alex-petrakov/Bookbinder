package me.alex.pet.bookbinder

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
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

    @ParameterizedTest
    @ValueSource(strings = [" ", "\n", "Text", "Te xt\nText\n"])
    fun `handles plain text`(plainText: String) {
        val input = "<test>$plainText</test>".byteInputStream()
        val reader = factory.createXMLEventReader(input).apply {
            nextEvent() // Skip the START_DOCUMENT event
            nextEvent() // Skip the <test> tag
        }

        val output = reader.parseStyledText()

        assertThat(output).isEqualTo(StyledString(plainText))
    }

    @Test
    fun `handles styled text`() {
        val input = "<test><e>01</e>23<s>45</s>67<m>89</m>01<l rule=\"1\">23</l></test>".byteInputStream()
        val reader = factory.createXMLEventReader(input).apply {
            nextEvent() // Skip the START_DOCUMENT event
            nextEvent() // Skip the <test> tag
        }

        val output = reader.parseStyledText()

        val styles = listOf(
            CharacterStyle.emphasis(0, 2),
            CharacterStyle.strongEmphasis(4, 6),
            CharacterStyle.misspell(8, 10),
        )
        val links = listOf(
            Link(12, 14, 1)
        )
        assertThat(output).isEqualTo(StyledString("01234567890123", styles, links))
    }

    @Test
    fun `does not allow empty text`() {
        val input = "<test></test>".byteInputStream()
        val reader = factory.createXMLEventReader(input).apply {
            nextEvent() // Skip the START_DOCUMENT event
            nextEvent() // Skip the <test> tag
        }

        assertThrows<RuntimeException> {
            reader.parseStyledText()
        }
    }

    @Test
    fun `does not allow nested style tags`() {
        val input = "<test><e><m>Text</m></e></test>".byteInputStream()
        val reader = factory.createXMLEventReader(input).apply {
            nextEvent() // Skip the START_DOCUMENT event
            nextEvent() // Skip the <test> tag
        }

        assertThrows<RuntimeException> {
            reader.parseStyledText()
        }
    }

    @Nested
    @DisplayName("when parsing paragraphs")
    inner class ParagraphParserTest {

        @Test
        fun `applies default indent and style if they are not specified explicitly`() {
            val input = "<p>Paragraph content</p>".byteInputStream()
            val reader = factory.createXMLEventReader(input).apply {
                nextEvent() // Skip the START_DOCUMENT event
            }

            val output = reader.parseParagraph()

            assertThat(output).isEqualTo(
                Paragraph(StyledString("Paragraph content"), ParagraphStyle.NORMAL, 0)
            )
        }

        @Test
        fun `handles normal style`() {
            val input = "<p style=\"normal\">Paragraph content</p>".byteInputStream()
            val reader = factory.createXMLEventReader(input).apply {
                nextEvent() // Skip the START_DOCUMENT event
            }

            val output = reader.parseParagraph()

            assertThat(output).isEqualTo(
                Paragraph(StyledString("Paragraph content"), ParagraphStyle.NORMAL)
            )
        }

        @Test
        fun `handles quote style`() {
            val input = "<p style=\"quote\">Paragraph content</p>".byteInputStream()
            val reader = factory.createXMLEventReader(input).apply {
                nextEvent() // Skip the START_DOCUMENT event
            }

            val output = reader.parseParagraph()

            assertThat(output).isEqualTo(
                Paragraph(StyledString("Paragraph content"), ParagraphStyle.QUOTE)
            )
        }

        @Test
        fun `handles footnote style`() {
            val input = "<p style=\"footnote\">Paragraph content</p>".byteInputStream()
            val reader = factory.createXMLEventReader(input).apply {
                nextEvent() // Skip the START_DOCUMENT event
            }

            val output = reader.parseParagraph()

            assertThat(output).isEqualTo(
                Paragraph(StyledString("Paragraph content"), ParagraphStyle.FOOTNOTE)
            )
        }

        @Test
        fun `does not allow unknown styles`() {
            val input = "<p style=\"some_unknown_style\">Paragraph content</p>".byteInputStream()
            val reader = factory.createXMLEventReader(input).apply {
                nextEvent() // Skip the START_DOCUMENT event
            }

            assertThrows<RuntimeException> {
                reader.parseParagraph()
            }
        }

        @ParameterizedTest
        @ValueSource(ints = [0, 1, 2, 3, 4, 5])
        fun `handles indents`(indent: Int) {
            val input = "<p indent=\"$indent\">Paragraph content</p>".byteInputStream()
            val reader = factory.createXMLEventReader(input).apply {
                nextEvent() // Skip the START_DOCUMENT event
            }

            val output = reader.parseParagraph()

            assertThat(output).isEqualTo(
                Paragraph(StyledString("Paragraph content"), indentLevel = indent)
            )
        }

        @ParameterizedTest
        @ValueSource(ints = [-10, -2, -1, 6, 7, 10])
        fun `does not allow incorrect indents`(indent: Int) {
            val input = "<p indent=\"$indent\">Paragraph content</p>".byteInputStream()
            val reader = factory.createXMLEventReader(input).apply {
                nextEvent() // Skip the START_DOCUMENT event
            }

            assertThrows<RuntimeException> {
                reader.parseParagraph()
            }
        }
    }
}