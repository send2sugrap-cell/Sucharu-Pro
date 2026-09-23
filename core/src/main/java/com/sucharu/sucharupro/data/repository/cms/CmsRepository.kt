package com.sucharu.sucharupro.data.repository.cms

import com.sucharu.sucharupro.data.api.model.cms.CmsBannerDto
import com.sucharu.sucharupro.data.api.model.cms.CmsCategoryDto
import com.sucharu.sucharupro.data.api.model.cms.CreateCmsBannerRequestDto
import com.sucharu.sucharupro.data.api.model.cms.CreateCmsCategoryRequestDto
import com.sucharu.sucharupro.data.api.model.cms.PublicWallCmsFeedResponseDto
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Repository interface and in-memory thread-safe implementation for Sucharu Pro CMS Banners & Categories.
 */
interface CmsRepository {
    fun createBanner(request: CreateCmsBannerRequestDto): CmsBannerDto
    fun getAllBanners(): List<CmsBannerDto>
    fun toggleBannerStatus(bannerId: String, isActive: Boolean): CmsBannerDto?
    fun createCategory(request: CreateCmsCategoryRequestDto): CmsCategoryDto
    fun getAllCategories(): List<CmsCategoryDto>
    fun getPublicWallFeed(): PublicWallCmsFeedResponseDto
}

class InMemoryCmsRepository : CmsRepository {
    private val banners = ConcurrentHashMap<String, CmsBannerDto>()
    private val categories = ConcurrentHashMap<String, CmsCategoryDto>()

    init {
        // Initial Default Banners
        val b1 = CmsBannerDto(
            bannerId = "BNR-EX-001",
            title = "Eid Special Bulk Printing Offer",
            description = "Get 15% discount on custom business cards, brochures, and packaging.",
            imageUrl = "https://images.unsplash.com/photo-1562654501-a0ccc0fc3fb1?w=800&q=80",
            discountTag = "15% DISCOUNT",
            isActive = true,
            displayOrder = 1,
            createdAt = System.currentTimeMillis()
        )
        val b2 = CmsBannerDto(
            bannerId = "BNR-EX-002",
            title = "2027 Executive Calendar Pre-Order",
            description = "Early bird pricing for custom desktop and wall calendars.",
            imageUrl = "https://images.unsplash.com/photo-1506784983877-45594efa4cbe?w=800&q=80",
            discountTag = "EARLY BIRD 10%",
            isActive = true,
            displayOrder = 2,
            createdAt = System.currentTimeMillis()
        )
        banners[b1.bannerId] = b1
        banners[b2.bannerId] = b2

        // Initial Default Categories
        val c1 = CmsCategoryDto("CAT-EX-001", "অফসেট প্রিন্টিং", "https://images.unsplash.com/photo-1581092160607-ee22621dd758?w=500&q=80", "বিজনেস কার্ড, লেটারহেড, প্যাড ও ব্রোশিয়ার", 1)
        val c2 = CmsCategoryDto("CAT-EX-002", "ডিজিটাল ফাস্ট প্রিন্টিং", "https://images.unsplash.com/photo-1562654501-a0ccc0fc3fb1?w=500&q=80", "জরুরী অর্ডারের জন্য সেম-ডে ডেলিভারি", 2)
        val c3 = CmsCategoryDto("CAT-EX-003", "প্যাকেজিং ও কার্টন", "https://images.unsplash.com/photo-1589939705384-5185137a7f0f?w=500&q=80", "কাস্টম কার্টন, পেপার ব্যাগ ও ডায়-কাট বক্স", 3)
        categories[c1.categoryId] = c1
        categories[c2.categoryId] = c2
        categories[c3.categoryId] = c3
    }

    override fun createBanner(request: CreateCmsBannerRequestDto): CmsBannerDto {
        val banner = CmsBannerDto(
            bannerId = "BNR-" + UUID.randomUUID().toString().take(8).uppercase(),
            title = request.title,
            description = request.description,
            imageUrl = request.imageUrl,
            discountTag = request.discountTag,
            targetDestination = request.targetDestination,
            isActive = true,
            displayOrder = request.displayOrder,
            createdAt = System.currentTimeMillis()
        )
        banners[banner.bannerId] = banner
        return banner
    }

    override fun getAllBanners(): List<CmsBannerDto> {
        return banners.values.sortedBy { it.displayOrder }
    }

    override fun toggleBannerStatus(bannerId: String, isActive: Boolean): CmsBannerDto? {
        val existing = banners[bannerId] ?: return null
        val updated = existing.copy(isActive = isActive)
        banners[bannerId] = updated
        return updated
    }

    override fun createCategory(request: CreateCmsCategoryRequestDto): CmsCategoryDto {
        val category = CmsCategoryDto(
            categoryId = "CAT-" + UUID.randomUUID().toString().take(8).uppercase(),
            categoryName = request.categoryName,
            imageUrl = request.imageUrl,
            description = request.description,
            displayOrder = request.displayOrder,
            isActive = true
        )
        categories[category.categoryId] = category
        return category
    }

    override fun getAllCategories(): List<CmsCategoryDto> {
        return categories.values.sortedBy { it.displayOrder }
    }

    override fun getPublicWallFeed(): PublicWallCmsFeedResponseDto {
        val activeBanners = banners.values.filter { it.isActive }.sortedBy { it.displayOrder }
        val activeCategories = categories.values.filter { it.isActive }.sortedBy { it.displayOrder }
        return PublicWallCmsFeedResponseDto(banners = activeBanners, categories = activeCategories)
    }
}
