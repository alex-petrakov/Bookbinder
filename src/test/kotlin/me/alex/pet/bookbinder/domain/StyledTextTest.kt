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
    @DisplayName("when mapping from paragraphs")
    inner class ParagraphsMapperTest {

        @Test
        fun `maps empty list`() {
            val paragraphs = emptyList<Paragraph>()

            val styledText = paragraphs.toStyledText()

            assertThat(styledText).isEqualTo(StyledText(""))
        }

        @Test
        fun `maps text content`() {
            val paragraphs = listOf(
                Paragraph(StyledString("Paragraph 1")),
                Paragraph(StyledString("Paragraph 2")),
                Paragraph(StyledString("Paragraph 3"))
            )

            val styledText = paragraphs.toStyledText("\n\n")

            assertThat(styledText).isEqualTo(
                StyledText("Paragraph 1\n\nParagraph 2\n\nParagraph 3\n\n")
            )
        }

        @Test
        fun `maps paragraph outer indents`() {
            val paragraphs = listOf(
                Paragraph(StyledString("01234567"), outerIndentLevel = 1),
                Paragraph(StyledString("01234567"), outerIndentLevel = 0),
                Paragraph(StyledString("01234567"), outerIndentLevel = 5)
            )

            val styledText = paragraphs.toStyledText("\n\n")

            val expectedParagraphSpans = listOf(
                ParagraphSpan.Indent(0, 8, 1, ""),
                ParagraphSpan.Indent(20, 28, 5, "")
            )
            assertThat(styledText).isEqualTo(
                StyledText(
                    "01234567\n\n01234567\n\n01234567\n\n",
                    paragraphSpans = expectedParagraphSpans
                )
            )
        }

        @Test
        fun `maps paragraph inner indents`() {
            val paragraphs = listOf(
                Paragraph(StyledString("01234567"), innerIndentLevel = 1),
                Paragraph(StyledString("01234567"), innerIndentLevel = 0),
                Paragraph(StyledString("01234567"), innerIndentLevel = 5)
            )

            val styledText = paragraphs.toStyledText("\n\n")

            val expectedParagraphSpans = listOf(
                ParagraphSpan.Indent(0, 8, 1, ""),
                ParagraphSpan.Indent(20, 28, 5, "")
            )
            assertThat(styledText).isEqualTo(
                StyledText(
                    "01234567\n\n01234567\n\n01234567\n\n",
                    paragraphSpans = expectedParagraphSpans
                )
            )
        }

        @Test
        fun `maps paragraph hanging text`() {
            val paragraphs = listOf(
                Paragraph(StyledString("01234567"), hangingText = "F. "),
                Paragraph(StyledString("01234567"), hangingText = ""),
                Paragraph(StyledString("01234567"), innerIndentLevel = 1, hangingText = "1) ")
            )

            val styledText = paragraphs.toStyledText("\n\n")

            val expectedParagraphSpans = listOf(
                ParagraphSpan.Indent(0, 8, 0, "F. "),
                ParagraphSpan.Indent(20, 28, 1, "1) ")
            )
            assertThat(styledText).isEqualTo(
                StyledText(
                    "01234567\n\n01234567\n\n01234567\n\n",
                    paragraphSpans = expectedParagraphSpans
                )
            )
        }

        @Test
        fun `maps paragraph styles`() {
            val paragraphs = listOf(
                Paragraph(StyledString("01234567"), style = ParagraphStyle.NORMAL),
                Paragraph(StyledString("01234567"), style = ParagraphStyle.QUOTE),
                Paragraph(StyledString("01234567"), style = ParagraphStyle.FOOTNOTE),
                Paragraph(StyledString("01234567"), style = ParagraphStyle.FOOTNOTE_QUOTE)
            )

            val styledText = paragraphs.toStyledText("\n\n")

            val expectedParagraphSpans = listOf(
                ParagraphSpan.Style(10, 18, ParagraphAppearance.QUOTE),
                ParagraphSpan.Style(20, 28, ParagraphAppearance.FOOTNOTE),
                ParagraphSpan.Style(30, 38, ParagraphAppearance.FOOTNOTE),
                ParagraphSpan.Style(30, 38, ParagraphAppearance.QUOTE)
            )
            assertThat(styledText).isEqualTo(
                StyledText(
                    "01234567\n\n01234567\n\n01234567\n\n01234567\n\n",
                    paragraphSpans = expectedParagraphSpans
                )
            )
        }

        @Test
        fun `respects paragraph span order`() {
            val paragraphs = listOf(
                Paragraph(
                    StyledString("01234567"),
                    style = ParagraphStyle.QUOTE,
                    outerIndentLevel = 1,
                    innerIndentLevel = 2,
                    hangingText = "1) "
                ),
            )

            val styledText = paragraphs.toStyledText("\n\n")

            val expectedParagraphSpans = listOf(
                ParagraphSpan.Indent(0, 8, 1, ""),
                ParagraphSpan.Style(0, 8, ParagraphAppearance.QUOTE),
                ParagraphSpan.Indent(0, 8, 2, "1) ")
            )
            assertThat(styledText).isEqualTo(
                StyledText(
                    "01234567\n\n",
                    paragraphSpans = expectedParagraphSpans
                )
            )
        }

        @Test
        fun `maps character styles within paragraphs`() {
            val paragraphs = listOf(
                Paragraph(
                    StyledString(
                        "01234567",
                        listOf(CharacterStyle(0, 4, CharacterStyleType.EMPHASIS))
                    )
                ),
                Paragraph(
                    StyledString(
                        "01234567",
                        listOf(CharacterStyle(4, 8, CharacterStyleType.STRONG_EMPHASIS))
                    )
                )
            )

            val styledText = paragraphs.toStyledText("\n\n")

            val expectedCharacterStyles = listOf(
                CharacterSpan(0, 4, CharacterAppearance.EMPHASIS),
                CharacterSpan(14, 18, CharacterAppearance.STRONG_EMPHASIS)
            )
            assertThat(styledText).isEqualTo(
                StyledText(
                    "01234567\n\n01234567\n\n",
                    characterSpans = expectedCharacterStyles
                )
            )
        }

        @Test
        fun `maps links within paragraphs`() {
            val paragraphs = listOf(
                Paragraph(
                    StyledString(
                        "01234567",
                        links = listOf(Link(0, 4, 1))
                    )
                ),
                Paragraph(
                    StyledString(
                        "01234567",
                        links = listOf(Link(4, 8, 2))
                    )
                )
            )

            val styledText = paragraphs.toStyledText("\n\n")

            val expectedLinks = listOf(
                LinkSpan(0, 4, 1),
                LinkSpan(14, 18, 2)
            )
            assertThat(styledText).isEqualTo(
                StyledText(
                    "01234567\n\n01234567\n\n",
                    linkSpans = expectedLinks
                )
            )
        }
    }
}