package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeExpenseCategoryDataSource
import com.sucharu.sucharupro.data.repository.ExpenseCategoryRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ExpenseCategoryDuplicateTest {

    private lateinit var categoryDataSource: FakeExpenseCategoryDataSource
    private lateinit var categoryRepository: ExpenseCategoryRepository

    @Before
    fun setUp() {
        categoryDataSource = FakeExpenseCategoryDataSource()
        categoryRepository = ExpenseCategoryRepositoryImpl(categoryDataSource)
    }

    @Test
    fun `duplicate category code in same project is rejected`() = runBlocking {
        val first = categoryRepository.createCategory(
            projectId = "PRJ-01",
            categoryCode = "RENT",
            categoryName = "Office Rent",
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )
        assertTrue(first is DomainResult.Success)

        val duplicate = categoryRepository.createCategory(
            projectId = "PRJ-01",
            categoryCode = "RENT",
            categoryName = "Factory Rent",
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )
        assertTrue(duplicate is DomainResult.Error)
    }
}
