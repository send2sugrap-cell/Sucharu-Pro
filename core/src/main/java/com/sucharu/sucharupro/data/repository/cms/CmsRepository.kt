package com.sucharu.sucharupro.data.repository.cms

import com.sucharu.sucharupro.data.api.model.cms.CmsBannerDto
import com.sucharu.sucharupro.data.api.model.cms.CmsCategoryDto
import com.sucharu.sucharupro.data.api.model.cms.CmsDesignTemplateDto
import com.sucharu.sucharupro.data.api.model.cms.CreateCmsBannerRequestDto
import com.sucharu.sucharupro.data.api.model.cms.CreateCmsCategoryRequestDto
import com.sucharu.sucharupro.data.api.model.cms.CreateCmsDesignTemplateRequestDto
import com.sucharu.sucharupro.data.api.model.cms.PublicWallCmsFeedResponseDto
import java.util.UUID
import java.util.concurrent.ConcurrentHashMap

/**
 * Repository interface and in-memory thread-safe implementation for Sucharu Pro CMS Banners, Categories & Design Templates.
 */
interface CmsRepository {
    fun createBanner(request: CreateCmsBannerRequestDto): CmsBannerDto
    fun getAllBanners(): List<CmsBannerDto>
    fun toggleBannerStatus(bannerId: String, isActive: Boolean): CmsBannerDto?
    fun createCategory(request: CreateCmsCategoryRequestDto): CmsCategoryDto
    fun getAllCategories(): List<CmsCategoryDto>
    fun createDesignTemplate(request: CreateCmsDesignTemplateRequestDto): CmsDesignTemplateDto
    fun getDesignTemplatesByCategory(categoryName: String): List<CmsDesignTemplateDto>
    fun getAllDesignTemplates(): List<CmsDesignTemplateDto>
    fun getPublicWallFeed(): PublicWallCmsFeedResponseDto
    fun getOrderFormConfig(): com.sucharu.sucharupro.data.api.model.cms.CmsOrderFormConfigDto
    fun updateOrderFormConfig(config: com.sucharu.sucharupro.data.api.model.cms.CmsOrderFormConfigDto): com.sucharu.sucharupro.data.api.model.cms.CmsOrderFormConfigDto
}

class InMemoryCmsRepository : CmsRepository {
    private val banners = ConcurrentHashMap<String, CmsBannerDto>()
    private val categories = ConcurrentHashMap<String, CmsCategoryDto>()
    private val templates = ConcurrentHashMap<String, CmsDesignTemplateDto>()

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

        // Initial Default Design Templates
        val t1 = CmsDesignTemplateDto(
            templateId = "TMPL-101",
            templateCode = "#TMPL-101",
            categoryName = "অফসেট প্রিন্টিং",
            title = "প্রিমিয়াম ইভেন্ট পোস্টার ডিজাইন",
            colorMode = "৪ কালার (CMYK)",
            paperStock = "১৫০ GSM আর্ট পেপার",
            printSize = "১৮\" × ২৩\" (Demy)",
            finishing = "গ্লস ল্যামিনেশন",
            suitabilityDescription = "প্রচারণা ও ইভেন্টের জন্য সেরা কোয়ালিটি",
            priceText = "৳ ১,২০০ / ১,০০০ পিস",
            perUnitRate = "(৳ ১.২০ / পিস)",
            badgeLabel = "বেস্টসেলার"
        )
        val t2 = CmsDesignTemplateDto(
            templateId = "TMPL-203",
            templateCode = "#TMPL-203",
            categoryName = "ভিজিটিং কার্ড",
            title = "প্রফেশনাল বিজনেস কার্ড",
            colorMode = "৪ কালার (CMYK)",
            paperStock = "৩০০ GSM আর্ট কার্ড",
            printSize = "২\" × ৩.৫\" (স্ট্যান্ডার্ড)",
            finishing = "ম্যাট ফিনিশ + স্পট UV",
            suitabilityDescription = "কর্পোরেট পরিচয়ের জন্য নিখুঁত ডিজাইন",
            priceText = "৳ ৮০০ / ১,০০০ পিস",
            perUnitRate = "(৳ ০.৮০ / পিস)",
            badgeLabel = "পপুলার"
        )
        val t3 = CmsDesignTemplateDto(
            templateId = "TMPL-307",
            templateCode = "#TMPL-307",
            categoryName = "ব্রোশিওর",
            title = "রঙিন প্রচারপত্র (লিফলেট)",
            colorMode = "৪ কালার (CMYK)",
            paperStock = "১০০ GSM আর্ট পেপার",
            printSize = "A4 সাইজ",
            finishing = "কোন ফিনিশ নেই",
            suitabilityDescription = "দ্রুত এবং সস্তা প্রচারের জন্য আদর্শ",
            priceText = "৳ ১,৫০০ / ১,০০০ পিস",
            perUnitRate = "(৳ ১.৫০ / পিস)",
            badgeLabel = "স্পেশাল"
        )
        templates[t1.templateId] = t1
        templates[t2.templateId] = t2
        templates[t3.templateId] = t3
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

    override fun createDesignTemplate(request: CreateCmsDesignTemplateRequestDto): CmsDesignTemplateDto {
        val id = UUID.randomUUID().toString().take(8).uppercase()
        val template = CmsDesignTemplateDto(
            templateId = "TMPL-$id",
            templateCode = "#TMPL-$id",
            categoryName = request.categoryName,
            title = request.title,
            colorMode = request.colorMode,
            paperStock = request.paperStock,
            printSize = request.printSize,
            finishing = request.finishing,
            suitabilityDescription = request.suitabilityDescription,
            priceText = request.priceText,
            perUnitRate = request.perUnitRate,
            imageUrl = request.imageUrl,
            badgeLabel = request.badgeLabel,
            isActive = true
        )
        templates[template.templateId] = template
        return template
    }

    override fun getDesignTemplatesByCategory(categoryName: String): List<CmsDesignTemplateDto> {
        val matching = templates.values.filter { it.isActive && (categoryName.contains(it.categoryName) || it.categoryName.contains(categoryName) || categoryName == "ALL" || categoryName == "সব") }
        return matching.ifEmpty { templates.values.filter { it.isActive }.toList() }
    }

    override fun getAllDesignTemplates(): List<CmsDesignTemplateDto> {
        return templates.values.toList()
    }

    override fun getPublicWallFeed(): PublicWallCmsFeedResponseDto {
        val activeBanners = banners.values.filter { it.isActive }.sortedBy { it.displayOrder }
        val activeCategories = categories.values.filter { it.isActive }.sortedBy { it.displayOrder }
        val activeTemplates = templates.values.filter { it.isActive }
        return PublicWallCmsFeedResponseDto(banners = activeBanners, categories = activeCategories, templates = activeTemplates)
    }

    private var orderFormConfig = com.sucharu.sucharupro.data.api.model.cms.CmsOrderFormConfigDto()

    override fun getOrderFormConfig(): com.sucharu.sucharupro.data.api.model.cms.CmsOrderFormConfigDto {
        return orderFormConfig
    }

    override fun updateOrderFormConfig(config: com.sucharu.sucharupro.data.api.model.cms.CmsOrderFormConfigDto): com.sucharu.sucharupro.data.api.model.cms.CmsOrderFormConfigDto {
        orderFormConfig = config
        return orderFormConfig
    }
}
