package me.alex.pet.bookbinder.domain

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

@DisplayName("Spanned text")
class SpannedTextTest {

    @Nested
    @DisplayName("when mapping from styled string")
    inner class StyledStringMapperTest {

        @ParameterizedTest
        @ValueSource(strings = ["", " ", "\n", "String content"])
        fun `maps string content`(content: String) {
            val string = StyledString(content)

            val result = string.toSpannedText()

            assertThat(result).isEqualTo(SpannedText(content))
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

            val result = string.toSpannedText()

            assertThat(result).isEqualTo(
                SpannedText(
                    "0123456789",
                    characterSpans = listOf(
                        CharacterSpan(0, 2, CharacterSpanStyle.EMPHASIS),
                        CharacterSpan(4, 6, CharacterSpanStyle.STRONG_EMPHASIS),
                        CharacterSpan(8, 10, CharacterSpanStyle.MISSPELL)
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

            val result = string.toSpannedText()

            assertThat(result).isEqualTo(
                SpannedText(
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

            val styledText = paragraphs.toSpannedText()

            assertThat(styledText).isEqualTo(SpannedText(""))
        }

        @Test
        fun `maps text content`() {
            val paragraphs = listOf(
                Paragraph(StyledString("Paragraph 1")),
                Paragraph(StyledString("Paragraph 2")),
                Paragraph(StyledString("Paragraph 3"))
            )

            val styledText = paragraphs.toSpannedText("\n\n")

            assertThat(styledText).isEqualTo(
                SpannedText("Paragraph 1\n\nParagraph 2\n\nParagraph 3\n\n")
            )
        }

        @Test
        fun `maps paragraph indents`() {
            val paragraphs = listOf(
                Paragraph(StyledString("01234567"), indentLevel = 1),
                Paragraph(StyledString("01234567"), indentLevel = 0),
                Paragraph(StyledString("01234567"), indentLevel = 5)
            )

            val styledText = paragraphs.toSpannedText("\n\n")

            val expectedIndents = listOf(
                IndentSpan(0, 8, 1),
                IndentSpan(20, 28, 5)
            )
            assertThat(styledText).isEqualTo(
                SpannedText(
                    "01234567\n\n01234567\n\n01234567\n\n",
                    indentSpans = expectedIndents
                )
            )
        }

        @Test
        fun `maps paragraph styles`() {
            val paragraphs = listOf(
                Paragraph(StyledString("01234567"), style = ParagraphStyle.NORMAL),
                Paragraph(StyledString("01234567"), style = ParagraphStyle.QUOTE),
                Paragraph(StyledString("01234567"), style = ParagraphStyle.FOOTNOTE)
            )

            val styledText = paragraphs.toSpannedText("\n\n")

            val expectedParagraphStyles = listOf(
                ParagraphSpan(10, 18, ParagraphSpanStyle.QUOTE),
                ParagraphSpan(20, 28, ParagraphSpanStyle.FOOTNOTE)
            )
            assertThat(styledText).isEqualTo(
                SpannedText(
                    "01234567\n\n01234567\n\n01234567\n\n",
                    paragraphSpans = expectedParagraphStyles
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

            val styledText = paragraphs.toSpannedText("\n\n")

            val expectedCharacterStyles = listOf(
                CharacterSpan(0, 4, CharacterSpanStyle.EMPHASIS),
                CharacterSpan(14, 18, CharacterSpanStyle.STRONG_EMPHASIS)
            )
            assertThat(styledText).isEqualTo(
                SpannedText(
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

            val styledText = paragraphs.toSpannedText("\n\n")

            val expectedLinks = listOf(
                LinkSpan(0, 4, 1),
                LinkSpan(14, 18, 2)
            )
            assertThat(styledText).isEqualTo(
                SpannedText(
                    "01234567\n\n01234567\n\n",
                    linkSpans = expectedLinks
                )
            )
        }
    }
}