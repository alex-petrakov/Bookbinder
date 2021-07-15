package me.alex.pet.bookbinder.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@DisplayName("Spanned text")
class StyledTextTest {

    @Nested
    @DisplayName("when mapping from styled string")
    inner class StyledStringMapperTest {

        @ParameterizedTest
        @ValueSource(strings = ["", " ", "\n", "String content"])
        fun `maps string content`(content: String) {
            val string = StyledString(content)

            val result = string.toStyledText()

            assertThat(result).isEqualTo(StyledText(content))
        }

        @Test
        fun `maps styles`() {
            val string = StyledString(
                "0123456789",
                styles = listOf(
                    CharacterStyle(0, 2, CharacterStyleType.EMPHASIS),
                    CharacterStyle(4, 6, CharacterStyleType.STRONG_EMPHASIS),
                    CharacterStyle(8, 10, CharacterStyleType.MISSPELL)
                )
            )

            val result = string.toStyledText()

            assertThat(result).isEqualTo(
                StyledText(
                    "0123456789",
                    characterSpans = listOf(
                        CharacterSpan(0, 2, CharacterAppearance.EMPHASIS),
                        CharacterSpan(4, 6, CharacterAppearance.STRONG_EMPHASIS),
                        CharacterSpan(8, 10, CharacterAppearance.MISSPELL)
                    )
                )
            )
        }

        @Test
        fun `maps links`() {
            val string = StyledString(
                "0123456789",
                links = listOf(
                    Link(0, 2, 1),
                    Link(4, 6, 10),
                    Link(8, 10, 100)
                )
            )

            val result = string.toStyledText()

            assertThat(result).isEqualTo(
                StyledText(
                    "0123456789",
                    linkSpans = listOf(
                        LinkSpan(0, 2, 1),
                        LinkSpan(4, 6, 10),
                        LinkSpan(8, 10, 100)
                    )
                )
            )
        }
    }

    @Nested
    @DisplayName("when splitting paragraphs with blank lines")
    inner class ParagraphsSplitterTest {

        @Test
        fun `does nothing with empty list`() {
            val paragraphs = emptyList<Paragraph>()

            val result = paragraphs.splitWithBlankLines()

            val expectedParagraphs = emptyList<Paragraph>()
            assertThat(result).isEqualTo(expectedParagraphs)
        }

        @Test
        fun `does nothing with list that contains one item`() {
            val paragraphs = listOf(Paragraph(StyledString("0123456789")))

            val result = paragraphs.splitWithBlankLines()

            val expectedParagraphs = listOf(Paragraph(StyledString("0123456789")))
            assertThat(result).isEqualTo(expectedParagraphs)
        }

        @Test
        fun `inserts blank lines between normal paragraphs`() {
            val paragraphs = listOf(
                Paragraph(StyledString("0123456789")),
                Paragraph(StyledString("0123456789"))
            )

            val result = paragraphs.splitWithBlankLines()

            val expectedParagraphs = listOf(
                Paragraph(StyledString("0123456789")),
                Paragraph(StyledString("")),
                Paragraph(StyledString("0123456789"))
            )
            assertThat(result).isEqualTo(expectedParagraphs)
        }

        @Test
        fun `preserves blank line style between quote paragraphs`() {
            val paragraphs = listOf(
                Paragraph(StyledString("0123456789"), ParagraphStyle.QUOTE),
                Paragraph(StyledString("0123456789"), ParagraphStyle.QUOTE)
            )

            val result = paragraphs.splitWithBlankLines()

            val expectedParagraphs = listOf(
                Paragraph(StyledString("0123456789"), ParagraphStyle.QUOTE),
                Paragraph(StyledString(""), ParagraphStyle.QUOTE),
                Paragraph(StyledString("0123456789"), ParagraphStyle.QUOTE)
            )
            assertThat(result).isEqualTo(expectedParagraphs)
        }

        @Test
        fun `preserves blank line style between footnote paragraphs 1`() {
            val paragraphs = listOf(
                Paragraph(StyledString("0123456789"), ParagraphStyle.FOOTNOTE),
                Paragraph(StyledString("0123456789"), ParagraphStyle.FOOTNOTE)
            )

            val result = paragraphs.splitWithBlankLines()

            val expectedParagraphs = listOf(
                Paragraph(StyledString("0123456789"), ParagraphStyle.FOOTNOTE),
                Paragraph(StyledString(""), ParagraphStyle.FOOTNOTE),
                Paragraph(StyledString("0123456789"), ParagraphStyle.FOOTNOTE)
            )
            assertThat(result).isEqualTo(expectedParagraphs)
        }

        @Test
        fun `preserves blank line style between footnote paragraphs 2`() {
            val paragraphs = listOf(
                Paragraph(StyledString("0123456789"), ParagraphStyle.FOOTNOTE),
                Paragraph(StyledString("0123456789"), ParagraphStyle.FOOTNOTE_QUOTE)
            )

            val result = paragraphs.splitWithBlankLines()

            val expectedParagraphs = listOf(
                Paragraph(StyledString("0123456789"), ParagraphStyle.FOOTNOTE),
                Paragraph(StyledString(""), ParagraphStyle.FOOTNOTE),
                Paragraph(StyledString("0123456789"), ParagraphStyle.FOOTNOTE_QUOTE)
            )
            assertThat(result).isEqualTo(expectedParagraphs)
        }

        @Test
        fun `preserves blank line style between footnote paragraphs 3`() {
            val paragraphs = listOf(
                Paragraph(StyledString("0123456789"), ParagraphStyle.FOOTNOTE_QUOTE),
                Paragraph(StyledString("0123456789"), ParagraphStyle.FOOTNOTE)
            )

            val result = paragraphs.splitWithBlankLines()

            val expectedParagraphs = listOf(
                Paragraph(StyledString("0123456789"), ParagraphStyle.FOOTNOTE_QUOTE),
                Paragraph(StyledString(""), ParagraphStyle.FOOTNOTE),
                Paragraph(StyledString("0123456789"), ParagraphStyle.FOOTNOTE)
            )
            assertThat(result).isEqualTo(expectedParagraphs)
        }

        @Test
        fun `applies normal paragraph style to blank lines between paragraphs of different style`() {
            val paragraphs = listOf(
                Paragraph(StyledString("0123456789"), ParagraphStyle.NORMAL),
                Paragraph(StyledString("0123456789"), ParagraphStyle.QUOTE),
                Paragraph(StyledString("0123456789"), ParagraphStyle.FOOTNOTE)
            )

            val result = paragraphs.splitWithBlankLines()

            val expectedParagraphs = listOf(
                Paragraph(StyledString("0123456789"), ParagraphStyle.NORMAL),
                Paragraph(StyledString(""), ParagraphStyle.NORMAL),
                Paragraph(StyledString("0123456789"), ParagraphStyle.QUOTE),
                Paragraph(StyledString(""), ParagraphStyle.NORMAL),
                Paragraph(StyledString("0123456789"), ParagraphStyle.FOOTNOTE)
            )
            assertThat(result).isEqualTo(expectedParagraphs)
        }
    }

    @Nested
    @DisplayName("when mapping from paragraphs")
    inner class ParagraphsMapperTest {

        @Test
        fun `maps empty list`() {
            val paragraphs = emptyList<Paragraph>()

            val styledText = paragraphs.toStyledText()

            assertThat(styledText).isEqualTo(StyledText(""))
        }

        @Test
        fun `maps empty paragraphs`() {
            val paragraphs = listOf(
                Paragraph(StyledString("")),
                Paragraph(StyledString(""))
            )

            val styledText = paragraphs.toStyledText()

            val expectedParagraphSpans = listOf(
                ParagraphSpan(0, 1, ParagraphAppearance.NORMAL, Indent(0, 0, "")),
                ParagraphSpan(1, 2, ParagraphAppearance.NORMAL, Indent(0, 0, "")),
            )
            assertThat(styledText).isEqualTo(
                StyledText(
                    "\n\n",
                    paragraphSpans = expectedParagraphSpans
                )
            )
        }

        @Test
        fun `maps text content`() {
            val paragraphs = listOf(
                Paragraph(StyledString("012345678")),
                Paragraph(StyledString("012345678")),
                Paragraph(StyledString("012345678"))
            )

            val styledText = paragraphs.toStyledText()

            val expectedParagraphSpans = listOf(
                ParagraphSpan(0, 10, ParagraphAppearance.NORMAL, Indent(0, 0, "")),
                ParagraphSpan(10, 20, ParagraphAppearance.NORMAL, Indent(0, 0, "")),
                ParagraphSpan(20, 30, ParagraphAppearance.NORMAL, Indent(0, 0, "")),
            )
            assertThat(styledText).isEqualTo(
                StyledText(
                    "012345678\n012345678\n012345678\n",
                    paragraphSpans = expectedParagraphSpans
                )
            )
        }

        @Test
        fun `maps paragraph outer indents`() {
            val paragraphs = listOf(
                Paragraph(StyledString("012345678"), outerIndentLevel = 1),
                Paragraph(StyledString("012345678"), outerIndentLevel = 0),
                Paragraph(StyledString("012345678"), outerIndentLevel = 5)
            )

            val styledText = paragraphs.toStyledText()

            val expectedParagraphSpans = listOf(
                ParagraphSpan(0, 10, ParagraphAppearance.NORMAL, Indent(1, 0, "")),
                ParagraphSpan(10, 20, ParagraphAppearance.NORMAL, Indent(0, 0, "")),
                ParagraphSpan(20, 30, ParagraphAppearance.NORMAL, Indent(5, 0, ""))
            )
            assertThat(styledText).isEqualTo(
                StyledText(
                    "012345678\n012345678\n012345678\n",
                    paragraphSpans = expectedParagraphSpans
                )
            )
        }

        @Test
        fun `maps paragraph inner indents`() {
            val paragraphs = listOf(
                Paragraph(StyledString("012345678"), innerIndentLevel = 1),
                Paragraph(StyledString("012345678"), innerIndentLevel = 0),
                Paragraph(StyledString("012345678"), innerIndentLevel = 5)
            )

            val styledText = paragraphs.toStyledText()

            val expectedParagraphSpans = listOf(
                ParagraphSpan(0, 10, ParagraphAppearance.NORMAL, Indent(0, 1, "")),
                ParagraphSpan(10, 20, ParagraphAppearance.NORMAL, Indent(0, 0, "")),
                ParagraphSpan(20, 30, ParagraphAppearance.NORMAL, Indent(0, 5, ""))
            )
            assertThat(styledText).isEqualTo(
                StyledText(
                    "012345678\n012345678\n012345678\n",
                    paragraphSpans = expectedParagraphSpans
                )
            )
        }

        @Test
        fun `maps paragraph hanging text`() {
            val paragraphs = listOf(
                Paragraph(StyledString("012345678"), hangingText = "F. "),
                Paragraph(StyledString("012345678"), hangingText = ""),
                Paragraph(StyledString("012345678"), innerIndentLevel = 1, hangingText = "1) ")
            )

            val styledText = paragraphs.toStyledText()

            val expectedParagraphSpans = listOf(
                ParagraphSpan(0, 10, ParagraphAppearance.NORMAL, Indent(0, 0, "F. ")),
                ParagraphSpan(10, 20, ParagraphAppearance.NORMAL, Indent(0, 0, "")),
                ParagraphSpan(20, 30, ParagraphAppearance.NORMAL, Indent(0, 1, "1) "))
            )
            assertThat(styledText).isEqualTo(
                StyledText(
                    "012345678\n012345678\n012345678\n",
                    paragraphSpans = expectedParagraphSpans
                )
            )
        }

        @Test
        fun `maps paragraph styles`() {
            val paragraphs = listOf(
                Paragraph(StyledString("012345678"), style = ParagraphStyle.NORMAL),
                Paragraph(StyledString("012345678"), style = ParagraphStyle.QUOTE),
                Paragraph(StyledString("012345678"), style = ParagraphStyle.FOOTNOTE),
                Paragraph(StyledString("012345678"), style = ParagraphStyle.FOOTNOTE_QUOTE)
            )

            val styledText = paragraphs.toStyledText()

            val expectedParagraphSpans = listOf(
                ParagraphSpan(0, 10, ParagraphAppearance.NORMAL, Indent(0, 0, "")),
                ParagraphSpan(10, 20, ParagraphAppearance.QUOTE, Indent(0, 0, "")),
                ParagraphSpan(20, 30, ParagraphAppearance.FOOTNOTE, Indent(0, 0, "")),
                ParagraphSpan(30, 40, ParagraphAppearance.FOOTNOTE_QUOTE, Indent(0, 0, "")),
            )
            assertThat(styledText).isEqualTo(
                StyledText(
                    "012345678\n012345678\n012345678\n012345678\n",
                    paragraphSpans = expectedParagraphSpans
                )
            )
        }

        @Test
        fun `respects paragraph span order`() {
            val paragraphs = listOf(
                Paragraph(
                    StyledString("012345678"),
                    style = ParagraphStyle.QUOTE,
                    outerIndentLevel = 1,
                    innerIndentLevel = 2,
                    hangingText = "1) "
                ),
            )

            val styledText = paragraphs.toStyledText()

            val expectedParagraphSpans = listOf(
                ParagraphSpan(0, 10, ParagraphAppearance.QUOTE, Indent(1, 2, "1) "))
            )
            assertThat(styledText).isEqualTo(
                StyledText(
                    "012345678\n",
                    paragraphSpans = expectedParagraphSpans
                )
            )
        }

        @Test
        fun `maps character styles within paragraphs`() {
            val paragraphs = listOf(
                Paragraph(
                    StyledString(
                        "012345678",
                        listOf(CharacterStyle(0, 4, CharacterStyleType.EMPHASIS))
                    )
                ),
                Paragraph(
                    StyledString(
                        "012345678",
                        listOf(CharacterStyle(4, 8, CharacterStyleType.STRONG_EMPHASIS))
                    )
                )
            )

            val styledText = paragraphs.toStyledText()

            val expectedParagraphStyles = listOf(
                ParagraphSpan(0, 10, ParagraphAppearance.NORMAL, Indent(0, 0, "")),
                ParagraphSpan(10, 20, ParagraphAppearance.NORMAL, Indent(0, 0, ""))
            )
            val expectedCharacterStyles = listOf(
                CharacterSpan(0, 4, CharacterAppearance.EMPHASIS),
                CharacterSpan(14, 18, CharacterAppearance.STRONG_EMPHASIS)
            )
            assertThat(styledText).isEqualTo(
                StyledText(
                    "012345678\n012345678\n",
                    paragraphSpans = expectedParagraphStyles,
                    characterSpans = expectedCharacterStyles
                )
            )
        }

        @Test
        fun `maps links within paragraphs`() {
            val paragraphs = listOf(
                Paragraph(
                    StyledString(
                        "012345678",
                        links = listOf(Link(0, 4, 1))
                    )
                ),
                Paragraph(
                    StyledString(
                        "012345678",
                        links = listOf(Link(4, 8, 2))
                    )
                )
            )

            val styledText = paragraphs.toStyledText()

            val expectedParagraphStyles = listOf(
                ParagraphSpan(0, 10, ParagraphAppearance.NORMAL, Indent(0, 0, "")),
                ParagraphSpan(10, 20, ParagraphAppearance.NORMAL, Indent(0, 0, ""))
            )
            val expectedLinks = listOf(
                LinkSpan(0, 4, 1),
                LinkSpan(14, 18, 2)
            )
            assertThat(styledText).isEqualTo(
                StyledText(
                    "012345678\n012345678\n",
                    paragraphSpans = expectedParagraphStyles,
                    linkSpans = expectedLinks
                )
            )
        }
    }
}