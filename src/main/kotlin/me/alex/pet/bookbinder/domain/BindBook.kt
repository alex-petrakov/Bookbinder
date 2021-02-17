package me.alex.pet.bookbinder.domain

import com.squareup.moshi.Moshi
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver
import me.alex.pet.bookbinder.data.BookDataStore
import me.alex.pet.bookbinder.data.RulesDatabase
import java.io.BufferedReader
import java.io.File
import javax.xml.stream.XMLInputFactory

class BindBook(private val moshi: Moshi = Moshi.Builder().build()) {

    operator fun invoke(inputFile: File, outputFile: File) {
        try {
            require(inputFile.isFile && inputFile.canRead())
            require(!outputFile.exists())
        } catch (e: SecurityException) {
            throw BindBookException("Access to the input or output file is denied", e)
        }

        try {
            outputFile.parentFile?.mkdirs()
        } catch (e: SecurityException) {
            throw BindBookException("Can't create parent directories for the output file", e)
        }

        val book = inputFile.bufferedReader().use { parseBookFrom(it) }

        JdbcSqliteDriver("jdbc:sqlite:${outputFile.absolutePath}").use { driver ->
            RulesDatabase.Schema.create(driver)
            BookDataStore(RulesDatabase(driver).bookQueries, moshi).saveBook(book)
        }
    }

    private fun parseBookFrom(bufferedReader: BufferedReader): Book {
        val xmlReader = XMLInputFactory.newInstance()
            .createXMLEventReader(bufferedReader)
        return try {
            xmlReader.parseBook()
        } catch (e: UnexpectedXmlException) {
            throw BindBookException("Unable to process the input file", e)
        } finally {
            xmlReader.close()
        }
    }

    class BindBookException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
}