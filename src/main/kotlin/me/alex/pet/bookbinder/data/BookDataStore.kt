package me.alex.pet.bookbinder.data

import com.squareup.moshi.Moshi
import me.alex.pet.bookbinder.BookQueries
import me.alex.pet.bookbinder.domain.*

class BookDataStore(private val bookQueries: BookQueries, private val moshi: Moshi) {

    private var ruleNumber = 1L

    fun saveBook(book: Book) = bookQueries.transaction {
        resetRuleNumber()
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
        val name = section.name.toSpannedText()
        val nameStr = name.string
        val markup = name.toJson().asString(moshi)
        bookQueries.insertSection(chapterId, nameStr, markup)
        val sectionId = bookQueries.lastInsertedRowId().executeAsOne()
        section.rules.forEach { saveRule(it, sectionId) }
    }

    private fun saveRule(rule: Rule, sectionId: Long) {
        val content = rule.paragraphs.toSpannedText()
        val contentStr = content.string
        val markup = content.toJson().asString(moshi)
        bookQueries.insertRule(sectionId, nextRuleNumber(), contentStr, markup)
    }

    private fun resetRuleNumber() {
        ruleNumber = 1L
    }

    private fun nextRuleNumber() = ruleNumber++
}