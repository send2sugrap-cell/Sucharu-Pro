package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeExpenseCategoryDataSource
import com.sucharu.sucharupro.data.repository.ExpenseCategoryRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ExpenseCategoryProjectIsolationTest {

    private lateinit var categoryDataSource: FakeExpenseCategoryDataSource
    private lateinit var categoryRepository: ExpenseCategoryRepository

    @Before
    fun setUp() {
        categoryDataSource = FakeExpenseCategoryDataSource()
        categoryRepository = ExpenseCategoryRepositoryImpl(categoryDataSource)
    }

    @Test
    fun `categories in Project A are completely isolated from Project B`() = runBlocking {
        categoryRepository.createCategory(
            projectId = "PRJ-A",
            categoryCode = "OFFICE",
            categoryName = "Office Supplies A",
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )

        categoryRepository.createCategory(
            projectId = "PRJ-B",
            categoryCode = "OFFICE",
            categoryName = "Office Supplies B",
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )

        val catsA = categoryRepository.observeCategories("PRJ-A", UserRole.ACCOUNTS).first()
        val catsB = categoryRepository.observeCategories("PRJ-B", UserRole.ACCOUNTS).first()

        assertEquals(1, catsA.size)
        assertEquals("PRJ-A", catsA[0].projectId)

        assertEquals(1, catsB.size)
        assertEquals("PRJ-B", catsB[0].projectId)
    }
}
