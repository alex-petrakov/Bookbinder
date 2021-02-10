package me.alex.pet.bookbinder.domain

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

    @Nested
    @DisplayName("when parsing styled text")
    inner class StyledTextParserTest {

        @Nested
        @DisplayName("when parsing style elements")
        inner class StyleElementsParserTest {

            @ParameterizedTest
            @ValueSource(strings = ["\n", " ", "Emphasized text", " Emphasized text "])
            fun `handles emphasis`(emphasizedText: String) {
                val input = "<e>$emphasizedText</e>".byteInputStream()
                val reader = factory.createXMLEventReader(input).apply {
                    nextEvent() // Skip the START_DOCUMENT event
                }

                val output = reader.parseStyledSubstring()

                assertThat(output).isEqualTo(emphasizedText to CharacterStyleType.EMPHASIS)
            }

            @ParameterizedTest
            @ValueSource(strings = ["\n", " ", "Emphasized text", " Emphasized text "])
            fun `handles strong emphasis`(emphasizedText: String) {
                val input = "<s>$emphasizedText</s>".byteInputStream()
                val reader = factory.createXMLEventReader(input).apply {
                    nextEvent() // Skip the START_DOCUMENT event
                }

                val output = reader.parseStyledSubstring()

                assertThat(output).isEqualTo(emphasizedText to CharacterStyleType.STRONG_EMPHASIS)
            }

            @ParameterizedTest
            @ValueSource(strings = ["\n", " ", "Emphasized text", " Emphasized text "])
            fun `handles misspell style`(emphasizedText: String) {
                val input = "<m>$emphasizedText</m>".byteInputStream()
                val reader = factory.createXMLEventReader(input).apply {
                    nextEvent() // Skip the START_DOCUMENT event
                }

                val output = reader.parseStyledSubstring()

                assertThat(output).isEqualTo(emphasizedText to CharacterStyleType.MISSPELL)
            }

            @Test
            fun `does not allow empty styled text`() {
                val input = "<e></e>".byteInputStream()
                val reader = factory.createXMLEventReader(input).apply {
                    nextEvent() // Skip the START_DOCUMENT event
                }

                assertThrows<UnexpectedXmlException> {
                    reader.parseStyledSubstring()
                }
            }
        }

        @Nested
        @DisplayName("when parsing links")
        inner class LinksParserTest {

            @ParameterizedTest
            @ValueSource(strings = ["\n", " ", "Emphasized text", " Emphasized text "])
            fun `handles link content`(content: String) {
                val input = "<l rule=\"1\">$content</l>".byteInputStream()
                val reader = factory.createXMLEventReader(input).apply {
                    nextEvent() // Skip the START_DOCUMENT event
                }

                val output = reader.parseLink()

                assertThat(output).isEqualTo(content to 1)
            }

            @Test
            fun `does not allow empty content`() {
                val input = "<l rule=\"1\"></l>".byteInputStream()
                val reader = factory.createXMLEventReader(input).apply {
                    nextEvent() // Skip the START_DOCUMENT event
                }

                assertThrows<UnexpectedXmlException> {
                    reader.parseLink()
                }
            }

            @ParameterizedTest
            @ValueSource(ints = [1, 2, 3, 5, 10, 100])
            fun `handles correct link destinations`(ruleId: Int) {
                val input = "<l rule=\"$ruleId\">Content</l>".byteInputStream()
                val reader = factory.createXMLEventReader(input).apply {
                    nextEvent() // Skip the START_DOCUMENT event
                }

                val output = reader.parseLink()

                assertThat(output).isEqualTo("Content" to ruleId)
            }

            @Test
            fun `does not allow links without a rule attribute`() {
                val input = "<l>Content</l>".byteInputStream()
                val reader = factory.createXMLEventReader(input).apply {
                    nextEvent() // Skip the START_DOCUMENT event
                }

                assertThrows<UnexpectedXmlException> {
                    reader.parseLink()
                }
            }

            @ParameterizedTest
            @ValueSource(strings = ["", "str", "10.1", "0", "-1"])
            fun `does not allow links that point to something other than a rule id`(ruleId: String) {
                val input = "<l rule=\"$ruleId\">Content</l>".byteInputStream()
                val reader = factory.createXMLEventReader(input).apply {
                    nextEvent() // Skip the START_DOCUMENT event
                }

                assertThrows<UnexpectedXmlException> {
                    reader.parseLink()
                }
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

            assertThrows<UnexpectedXmlException> {
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

            assertThrows<UnexpectedXmlException> {
                reader.parseStyledText()
            }
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

            assertThrows<UnexpectedXmlException> {
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

            assertThrows<UnexpectedXmlException> {
                reader.parseParagraph()
            }
        }
    }

    @Nested
    @DisplayName("when parsing rules")
    inner class RuleParserTest {

        @Test
        fun `handles rules`() {
            val input = """
                <rule>
                    <p>Paragraph 1</p>
                    <p>Paragraph 2</p>
                    <p>Paragraph 3</p>
                </rule>
            """.trimIndent().byteInputStream()
            val reader = factory.createXMLEventReader(input).apply {
                nextEvent() // Skip the START_DOCUMENT event
            }

            val output = reader.parseRule()

            assertThat(output).isEqualTo(
                Rule(
                    listOf(
                        Paragraph(StyledString("Paragraph 1")),
                        Paragraph(StyledString("Paragraph 2")),
                        Paragraph(StyledString("Paragraph 3"))
                    )
                )
            )
        }

        @Test
        fun `does not allow empty rules`() {
            val input = """
                <rule></rule>
            """.trimIndent().byteInputStream()
            val reader = factory.createXMLEventReader(input).apply {
                nextEvent() // Skip the START_DOCUMENT event
            }

            assertThrows<UnexpectedXmlException> {
                reader.parseRule()
            }
        }
    }

    @Nested
    @DisplayName("when parsing sections")
    inner class SectionParserTest {

        @Test
        fun `handles sections`() {
            val input = """
                <section>
                    <name>Section 1</name>
                    <rule>
                        <p>Paragraph 1</p>
                    </rule>
                </section>
            """.trimIndent().byteInputStream()
            val reader = factory.createXMLEventReader(input).apply {
                nextEvent() // Skip the START_DOCUMENT event
            }

            val output = reader.parseSection()

            val expectedParagraphs = listOf(Paragraph(StyledString("Paragraph 1")))
            val expectedRules = listOf(Rule(expectedParagraphs))
            assertThat(output).isEqualTo(
                Section(StyledString("Section 1"), expectedRules)
            )
        }

        @Test
        fun `does not allow sections without a name`() {
            val input = """
                <section>
                    <rule>
                        <p>Paragraph 1</p>
                    </rule>
                </section>
            """.trimIndent().byteInputStream()
            val reader = factory.createXMLEventReader(input).apply {
                nextEvent() // Skip the START_DOCUMENT event
            }

            assertThrows<UnexpectedXmlException> {
                reader.parseSection()
            }
        }

        @Test
        fun `does not allow sections with empty names`() {
            val input = """
                <section>
                    <name></name>
                    <rule>
                        <p>Paragraph 1</p>
                    </rule>
                </section>
            """.trimIndent().byteInputStream()
            val reader = factory.createXMLEventReader(input).apply {
                nextEvent() // Skip the START_DOCUMENT event
            }

            assertThrows<UnexpectedXmlException> {
                reader.parseSection()
            }
        }

        @Test
        fun `does not allow sections without rules`() {
            val input = """
                <section>
                    <name>Section 1</name>
                </section>
            """.trimIndent().byteInputStream()
            val reader = factory.createXMLEventReader(input).apply {
                nextEvent() // Skip the START_DOCUMENT event
            }

            assertThrows<UnexpectedXmlException> {
                reader.parseSection()
            }
        }
    }

    @Nested
    @DisplayName("when parsing chapters")
    inner class ChapterParserTest {

        @Test
        fun `handles chapters`() {
            val input = """
                <chapter>
                    <name>Chapter 1</name>
                    <section>
                        <name>Section 1</name>
                        <rule>
                            <p>Paragraph 1</p>
                        </rule>
                    </section>
                </chapter>
            """.trimIndent().byteInputStream()
            val reader = factory.createXMLEventReader(input).apply {
                nextEvent() // Skip the START_DOCUMENT event
            }

            val output = reader.parseChapter()

            val expectedParagraphs = listOf(Paragraph(StyledString("Paragraph 1")))
            val expectedRules = listOf(Rule(expectedParagraphs))
            val expectedSections = listOf(Section(StyledString("Section 1"), expectedRules))
            assertThat(output).isEqualTo(
                Chapter("Chapter 1", expectedSections)
            )
        }

        @Test
        fun `does not allow chapters without a name`() {
            val input = """
                <chapter>
                    <section>
                        <name>Section 1</name>
                        <rule>
                            <p>Paragraph 1</p>
                        </rule>
                    </section>
                </chapter>
            """.trimIndent().byteInputStream()
            val reader = factory.createXMLEventReader(input).apply {
                nextEvent() // Skip the START_DOCUMENT event
            }

            assertThrows<UnexpectedXmlException> {
                reader.parseChapter()
            }
        }

        @Test
        fun `does not allow chapters with empty names`() {
            val input = """
                <chapter>
                    <name></name>
                    <section>
                        <name>Section 1</name>
                        <rule>
                            <p>Paragraph 1</p>
                        </rule>
                    </section>
                </chapter>
            """.trimIndent().byteInputStream()
            val reader = factory.createXMLEventReader(input).apply {
                nextEvent() // Skip the START_DOCUMENT event
            }

            assertThrows<UnexpectedXmlException> {
                reader.parseChapter()
            }
        }

        @Test
        fun `does not allow chapters without sections`() {
            val input = """
                <chapter>
                    <name>Chapter 1</name>
                </chapter>
            """.trimIndent().byteInputStream()
            val reader = factory.createXMLEventReader(input).apply {
                nextEvent() // Skip the START_DOCUMENT event
            }

            assertThrows<UnexpectedXmlException> {
                reader.parseChapter()
            }
        }
    }

    @Nested
    @DisplayName("when parsing parts")
    inner class PartParserTest {

        @Test
        fun `handles parts`() {
            val input = """
                <part>
                    <name>Part 1</name>
                    <chapter>
                        <name>Chapter 1</name>
                        <section>
                            <name>Section 1</name>
                            <rule>
                                <p>Paragraph 1</p>
                            </rule>
                        </section>
                    </chapter>
                </part>
            """.trimIndent().byteInputStream()
            val reader = factory.createXMLEventReader(input).apply {
                nextEvent() // Skip the START_DOCUMENT event
            }

            val output = reader.parsePart()

            val expectedParagraphs = listOf(Paragraph(StyledString("Paragraph 1")))
            val expectedRules = listOf(Rule(expectedParagraphs))
            val expectedSections = listOf(Section(StyledString("Section 1"), expectedRules))
            val expectedChapters = listOf(Chapter("Chapter 1", expectedSections))
            assertThat(output).isEqualTo(
                Part("Part 1", expectedChapters)
            )
        }

        @Test
        fun `does not allow parts without a name`() {
            val input = """
                <part>
                    <chapter>
                        <name>Chapter 1</name>
                        <section>
                            <name>Section 1</name>
                            <rule>
                                <p>Paragraph 1</p>
                            </rule>
                        </section>
                    </chapter>
                </part>
            """.trimIndent().byteInputStream()
            val reader = factory.createXMLEventReader(input).apply {
                nextEvent() // Skip the START_DOCUMENT event
            }

            assertThrows<UnexpectedXmlException> {
                reader.parsePart()
            }
        }

        @Test
        fun `does not allow parts with empty names`() {
            val input = """
                <part>
                    <name></name>
                    <chapter>
                        <name>Chapter 1</name>
                        <section>
                            <name>Section 1</name>
                            <rule>
                                <p>Paragraph 1</p>
                            </rule>
                        </section>
                    </chapter>
                </part>
            """.trimIndent().byteInputStream()
            val reader = factory.createXMLEventReader(input).apply {
                nextEvent() // Skip the START_DOCUMENT event
            }

            assertThrows<UnexpectedXmlException> {
                reader.parsePart()
            }
        }

        @Test
        fun `does not allow parts without chapters`() {
            val input = """
                <part>
                    <name>Part 1</name>
                </part>
            """.trimIndent().byteInputStream()
            val reader = factory.createXMLEventReader(input).apply {
                nextEvent() // Skip the START_DOCUMENT event
            }

            assertThrows<UnexpectedXmlException> {
                reader.parsePart()
            }
        }
    }

    @Nested
    @DisplayName("when parsing a book")
    inner class BookParserTest {

        @Test
        fun `handles book`() {
            val input = """
                <?xml version="1.0" encoding="UTF-8"?>
                <book>
                    <part>
                        <name>Part 1</name>
                        <chapter>
                            <name>Chapter 1</name>
                            <section>
                                <name>Section 1</name>
                                <rule>
                                    <p>Paragraph 1</p>
                                </rule>
                            </section>
                        </chapter>
                    </part>
                </book>
            """.trimIndent().byteInputStream()
            val reader = factory.createXMLEventReader(input)

            val output = reader.parseBook()

            val expectedParagraphs = listOf(Paragraph(StyledString("Paragraph 1")))
            val expectedRules = listOf(Rule(expectedParagraphs))
            val expectedSections = listOf(Section(StyledString("Section 1"), expectedRules))
            val expectedChapters = listOf(Chapter("Chapter 1", expectedSections))
            val expectedParts = listOf(Part("Part 1", expectedChapters))
            assertThat(output).isEqualTo(expectedParts)
        }

        @Test
        fun `does not permit an empty book`() {
            val input = """
                <?xml version="1.0" encoding="UTF-8"?>
                <book></book>
            """.trimIndent().byteInputStream()
            val reader = factory.createXMLEventReader(input)

            assertThrows<UnexpectedXmlException> {
                reader.parseBook()
            }
        }
    }
}