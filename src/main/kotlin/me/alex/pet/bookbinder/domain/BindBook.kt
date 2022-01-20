package me.alex.pet.bookbinder.domain

import com.squareup.moshi.Moshi
import com.squareup.sqldelight.db.SqlDriver
import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver
import me.alex.pet.bookbinder.data.BookDataStore
import me.alex.pet.bookbinder.data.RulesDatabase
import java.io.BufferedReader
import java.io.File
import javax.xml.stream.XMLEventReader
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLStreamException

class BindBook(private val moshi: Moshi, private val xmlInputFactory: XMLInputFactory) {

    operator fun invoke(inputFile: File, outputFile: File, databaseVersion: Long) {
        require(inputFile.isFile) { "File at ${inputFile.absolutePath} is not a plain file" }
        require(!outputFile.exists()) { "File at ${outputFile.absolutePath} already exists, specify another output path" }
        require(databaseVersion > 0L) { "Illegal database version ($databaseVersion)" }

        val outputFileParent = outputFile.parentFile
        if (outputFileParent != null && !outputFileParent.exists() && !outputFileParent.mkdirs()) {
            throw BindBookException("Failed to create parent directories for ${outputFile.absolutePath}")
        }

        val book = inputFile.bufferedReader().use { parseBookFrom(it) }

        JdbcSqliteDriver("jdbc:sqlite:${outputFile.absolutePath}").use { driver ->
            RulesDatabase.Schema.create(driver)
            BookDataStore(RulesDatabase(driver).bookQueries, moshi).saveBook(book)
            driver.setPragmaUserVersion(databaseVersion)
        }
    }

    private fun require(value: Boolean, messageBuilder: () -> String) {
        if (!value) throw BindBookException(messageBuilder())
    }

    private fun parseBookFrom(bufferedReader: BufferedReader): Book {
        val xmlReader = createXmlEventReader(bufferedReader)
        return try {
            xmlReader.parseBook()
        } catch (e: UnexpectedXmlException) {
            throw BindBookException("Unable to process the input file", e)
        } finally {
            xmlReader.close()
        }
    }

    private fun createXmlEventReader(bufferedReader: BufferedReader): XMLEventReader {
        return try {
            xmlInputFactory.createXMLEventReader(bufferedReader)
        } catch (e: XMLStreamException) {
            throw BindBookException("Unable to create an XML reader", e)
        }
    }

    private fun SqlDriver.setPragmaUserVersion(version: Long) {
        execute(null, "PRAGMA user_version = $version;", 0)
    }

    class BindBookException(message: String, cause: Throwable? = null) : RuntimeException(message, cause)
}