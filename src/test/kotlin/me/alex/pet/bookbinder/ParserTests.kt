package me.alex.pet.bookbinder

import com.github.kittinunf.result.Result
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.ObjectAssert
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test


class ParserTests {

    private val parser = Parser()

    @Test
    fun `empty input is not allowed`() {
        val input = "".byteInputStream()

        val output = parser.parse(input)

        assertThat(output).isFailure
    }

    @Test
    fun `empty book is not allowed 1`() {
        @Language("XML") val input = """<?xml version="1.0" encoding="UTF-8"?>
            <book/>
        """.trimIndent().byteInputStream()

        val output = parser.parse(input)

        assertThat(output).isFailure
    }

    @Test
    fun `empty book is not allowed 2`() {
        @Language("XML") val input = """<?xml version="1.0" encoding="UTF-8"?>
            <book></book>
        """.trimIndent().byteInputStream()

        val output = parser.parse(input)

        assertThat(output).isFailure
    }

    @Test
    fun `empty book is not allowed 3`() {
        @Language("XML") val input = """<?xml version="1.0" encoding="UTF-8"?>
            <book>
            </book>
        """.trimIndent().byteInputStream()

        val output = parser.parse(input)

        assertThat(output).isFailure
    }

    @Test
    fun `parts without a name are not allowed`() {
        @Language("XML") val input = """<?xml version="1.0" encoding="UTF-8"?>
            <book>
                <part>
                    <chapter/>
                </part>
            </book>
        """.trimIndent().byteInputStream()

        val output = parser.parse(input)

        assertThat(output).isFailure
    }

    @Test
    fun `parts without chapters are not allowed`() {
        @Language("XML") val input = """<?xml version="1.0" encoding="UTF-8"?>
            <book>
                <part>
                    <name>Part 1</name>
                </part>
            </book>
        """.trimIndent().byteInputStream()

        val output = parser.parse(input)

        assertThat(output).isFailure
    }

    @Test
    fun `chapters without a name are not allowed`() {
        @Language("XML") val input = """<?xml version="1.0" encoding="UTF-8"?>
            <book>
                <part>
                    <name>Part 1</name>
                    <chapter>
                        <section/>
                    </chapter>
                </part>
            </book>
        """.trimIndent().byteInputStream()

        val output = parser.parse(input)

        assertThat(output).isFailure
    }

    @Test
    fun `chapters without sections are not allowed`() {
        @Language("XML") val input = """<?xml version="1.0" encoding="UTF-8"?>
            <book>
                <part>
                    <name>Part 1</name>
                    <chapter>
                        <name>Chapter 1</name>
                    </chapter>
                </part>
            </book>
        """.trimIndent().byteInputStream()

        val output = parser.parse(input)

        assertThat(output).isFailure
    }

    @Test
    fun `sections without a name are not allowed`() {
        @Language("XML") val input = """<?xml version="1.0" encoding="UTF-8"?>
            <book>
                <part>
                    <name>Part 1</name>
                    <chapter>
                        <name>Chapter 1</name>
                        <section>
                            <rule/>
                        </section>
                    </chapter>
                </part>
            </book>
        """.trimIndent().byteInputStream()

        val output = parser.parse(input)

        assertThat(output).isFailure
    }

    @Test
    fun `sections without rules are not allowed`() {
        @Language("XML") val input = """<?xml version="1.0" encoding="UTF-8"?>
            <book>
                <part>
                    <name>Part 1</name>
                    <chapter>
                        <name>Chapter 1</name>
                        <section>
                            <name>Section 1</name>
                        </section>
                    </chapter>
                </part>
            </book>
        """.trimIndent().byteInputStream()

        val output = parser.parse(input)

        assertThat(output).isFailure
    }

    @Test
    fun `empty rules are not allowed`() {
        @Language("XML") val input = """<?xml version="1.0" encoding="UTF-8"?>
            <book>
                <part>
                    <name>Part 1</name>
                    <chapter>
                        <name>Chapter 1</name>
                        <section>
                            <name>Section 1</name>
                            <rule/>
                        </section>
                    </chapter>
                </part>
            </book>
        """.trimIndent().byteInputStream()

        val output = parser.parse(input)

        assertThat(output).isFailure
    }

    @Test
    fun `parses a book`() {
        @Language("XML") val input = """<?xml version="1.0" encoding="UTF-8"?>
            <book>
                <part>
                    <name>Part 1</name>
                    <chapter>
                        <name>Chapter 1</name>
                        <section>
                            <name>Section 1</name>
                            <rule>
                                <p/>
                            </rule>
                        </section>
                    </chapter>
                </part>
            </book>
        """.trimIndent().byteInputStream()

        val output = parser.parse(input)

        val rules = listOf(Rule("Rule 1".toStyledString()))
        val sections = listOf(Section("Section 1".toStyledString(), rules))
        val chapters = listOf(Chapter("Chapter 1", sections))
        val expectedParts = listOf(Part("Part 1", chapters))
        assertThat(output).isEqualTo(
            Result.success(expectedParts)
        )
    }
}


private val <V, E : Exception> ObjectAssert<Result<V, E>>.isFailure: ObjectAssert<Result<V, E>>
    get() {
        return isInstanceOf(Result.Failure::class.java)
    }