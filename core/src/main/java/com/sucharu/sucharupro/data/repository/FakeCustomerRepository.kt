package com.sucharu.sucharupro.data.repository

import com.sucharu.sucharupro.data.datasource.FakeCustomerDataSource
import com.sucharu.sucharupro.domain.repository.CustomerRepository

/**
 * Fake in-memory repository for Customer Management.
 *
 * Direct subclass of [CustomerRepositoryImpl] pre-configured with [FakeCustomerDataSource].
 */
class FakeCustomerRepository(
    dataSource: FakeCustomerDataSource = FakeCustomerDataSource()
) : CustomerRepository by CustomerRepositoryImpl(dataSource)
