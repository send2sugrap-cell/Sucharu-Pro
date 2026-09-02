package com.sucharu.sucharupro.data.api.model

import java.math.BigDecimal

/**
 * Public company information DTO (INFRA-02 Step 04).
 */
data class CompanyInfoDto(
    val companyName: String = "Sucharu Printing & Packaging",
    val tagline: String = "Precision Commercial Printing ERP & Production",
    val email: String = "contact@sucharu.pro",
    val phone: String = "+880 1700-000000",
    val address: String = "Dhaka, Bangladesh",
    val supportedServices: List<String> = listOf(
        "Commercial Offset Printing",
        "Packaging & Carton Manufacturing",
        "Digital Printing",
        "Book & Catalog Binding"
    )
)

/**
 * Public catalog product DTO.
 */
data class PublicProductDto(
    val productId: String,
    val name: String,
    val category: String,
    val description: String,
    val startingPrice: BigDecimal
)

/**
 * Public catalog service DTO.
 */
data class PublicServiceDto(
    val serviceId: String,
    val title: String,
    val description: String,
    val turnaroundDays: Int
)

/**
 * Public FAQ item DTO.
 */
data class PublicFaqDto(
    val question: String,
    val answer: String,
    val category: String
)
