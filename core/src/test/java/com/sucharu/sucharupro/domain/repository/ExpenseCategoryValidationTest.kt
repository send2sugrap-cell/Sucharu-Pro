package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeExpenseCategoryDataSource
import com.sucharu.sucharupro.data.repository.ExpenseCategoryRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ExpenseCategoryValidationTest {

    private lateinit var categoryDataSource: FakeExpenseCategoryDataSource
    private lateinit var categoryRepository: ExpenseCategoryRepository

    @Before
    fun setUp() {
        categoryDataSource = FakeExpenseCategoryDataSource()
        categoryRepository = ExpenseCategoryRepositoryImpl(categoryDataSource)
    }

    @Test
    fun `valid category passes validation`() = runBlocking {
        val res = categoryRepository.createCategory(
            projectId = "PRJ-01",
            categoryCode = "INTERNET",
            categoryName = "Internet & Broadband",
            description = "Office high speed internet",
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )
        assertTrue(res is DomainResult.Success)
    }

    @Test
    fun `blank code or name is rejected`() = runBlocking {
        val blankCode = categoryRepository.createCategory(
            projectId = "PRJ-01",
            categoryCode = "  ",
            categoryName = "Internet",
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )
        assertTrue(blankCode is DomainResult.Error)

        val blankName = categoryRepository.createCategory(
            projectId = "PRJ-01",
            categoryCode = "INTERNET",
            categoryName = "  ",
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )
        assertTrue(blankName is DomainResult.Error)
    }
}
