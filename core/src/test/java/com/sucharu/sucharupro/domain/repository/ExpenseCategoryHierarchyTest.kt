package com.sucharu.sucharupro.domain.repository

import com.sucharu.sucharupro.data.datasource.FakeExpenseCategoryDataSource
import com.sucharu.sucharupro.data.repository.ExpenseCategoryRepositoryImpl
import com.sucharu.sucharupro.domain.model.common.DomainResult
import com.sucharu.sucharupro.domain.model.user.UserRole
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ExpenseCategoryHierarchyTest {

    private lateinit var categoryDataSource: FakeExpenseCategoryDataSource
    private lateinit var categoryRepository: ExpenseCategoryRepository

    @Before
    fun setUp() {
        categoryDataSource = FakeExpenseCategoryDataSource()
        categoryRepository = ExpenseCategoryRepositoryImpl(categoryDataSource)
    }

    @Test
    fun `circular hierarchy is detected and rejected on category update`() = runBlocking {
        val parent = (categoryRepository.createCategory(
            projectId = "PRJ-01",
            categoryCode = "UTILITIES",
            categoryName = "Utilities",
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        ) as DomainResult.Success).data

        val child = (categoryRepository.createCategory(
            projectId = "PRJ-01",
            categoryCode = "POWER",
            categoryName = "Power",
            parentCategoryId = parent.categoryId,
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        ) as DomainResult.Success).data

        // Attempt to make parent's parent the child (cycle: parent -> child -> parent)
        val cycleUpdate = categoryRepository.updateCategory(
            categoryId = parent.categoryId,
            parentCategoryId = child.categoryId,
            actorId = "acct-1",
            callerRole = UserRole.ACCOUNTS
        )
        assertTrue(cycleUpdate is DomainResult.Error)
    }
}
