package me.alex.pet.bookbinder

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.CliktError
import com.github.ajalt.clikt.parameters.arguments.argument
import com.github.ajalt.clikt.parameters.arguments.check
import com.github.ajalt.clikt.parameters.types.file
import com.github.ajalt.clikt.parameters.types.long
import com.squareup.moshi.Moshi
import me.alex.pet.bookbinder.domain.BindBook
import java.io.File
import javax.xml.stream.XMLInputFactory

fun main(args: Array<String>) = BindBookCommand().main(args)


class BindBookCommand : CliktCommand() {

    private val input: File by argument("<input-file>").file(
        mustExist = true,
        canBeDir = false
    )

    private val output: File by argument("<output-file>").file(
        canBeDir = false
    )

    private val databaseVersion: Long by argument("<user-version>").long()
        .check("Value must be greater than 0") { it > 0L }

    private val bindBook = BindBook(
        Moshi.Builder().build(),
        XMLInputFactory.newFactory()
    )

    override fun run() {
        try {
            bindBook(input, output, databaseVersion)
            echo("The book has been created at ${output.absolutePath}")
        } catch (e: BindBook.BindBookException) {
            throw CliktError(e.message ?: "Something went wrong", e)
        }
    }
}