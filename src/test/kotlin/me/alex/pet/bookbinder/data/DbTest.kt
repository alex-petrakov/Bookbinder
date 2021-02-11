package me.alex.pet.bookbinder.data

import com.squareup.sqldelight.sqlite.driver.JdbcSqliteDriver
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

@DisplayName("DB")
class DbTest {

    private val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)

    @Test
    fun `creates a DB and establishes connection to it`() {
        RulesDatabase.Schema.create(driver)
        RulesDatabase(driver)
    }
}