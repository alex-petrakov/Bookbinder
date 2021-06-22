package me.alex.pet.bookbinder.data

import com.squareup.moshi.Moshi
import me.alex.pet.bookbinder.BookQueries
import me.alex.pet.bookbinder.domain.*

class BookDataStore(private val bookQueries: BookQueries, private val moshi: Moshi) {

    fun saveBook(book: Book) = bookQueries.transaction {
        book.forEach { part -> savePart(part) }
    }

    private fun savePart(part: Part) {
        bookQueries.insertPart(part.name)
        val partId = bookQueries.lastInsertedRowId().executeAsOne()
        part.chapters.forEach { saveChapter(it, partId) }
    }

    private fun saveChapter(chapter: Chapter, partId: Long) {
        bookQueries.insertChapter(partId, chapter.name)
        val chapterId = bookQueries.lastInsertedRowId().executeAsOne()
        chapter.sections.forEach { saveSection(it, chapterId) }
    }

    private fun saveSection(section: Section, chapterId: Long) {
        val name = section.name.toStyledText()
        val nameStr = name.string
        val markup = name.toJson().asString(moshi)
        bookQueries.insertSection(chapterId, nameStr, markup)
        val sectionId = bookQueries.lastInsertedRowId().executeAsOne()
        section.rules.forEach { saveRule(it, sectionId) }
    }

    private fun saveRule(rule: Rule, sectionId: Long) {
        val content = rule.paragraphs.toStyledText()
        val contentStr = content.string
        val contentMarkup = content.toJson().asString(moshi)
        val annotation = rule.annotation.toStyledText()
        val annotationStr = annotation.string
        val annotationMarkup = annotation.toJson().asString(moshi)
        bookQueries.insertRule(sectionId, annotationStr, annotationMarkup, contentStr, contentMarkup)
    }
}