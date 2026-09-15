# SUCHARU PRO — WALL UI STEP 02 REPORT

## FINAL WALL LAYOUT RECOMPOSITION SURGICAL REPORT

---

### A. Scope
- Recompose the front-facing customer/affiliate Home Wall screen into the locked primary section order.
- Maintain existing ViewModels, DataSources, API calls, Navigation callbacks, and domain contracts.

---

### B. Files Inspected
- `app/src/main/java/com/sucharu/sucharupro/ui/customer/wall/SucharuWallScreen.kt`
- `app/src/main/java/com/sucharu/sucharupro/ui/customer/wall/components/HomeHeader.kt`
- `app/src/main/java/com/sucharu/sucharupro/ui/customer/wall/components/DailyWisdomCard.kt`
- `app/src/main/java/com/sucharu/sucharupro/ui/customer/wall/components/HomeBannerPager.kt`
- `app/src/main/java/com/sucharu/sucharupro/ui/customer/wall/components/PrayerTimesCard.kt`
- `app/src/main/java/com/sucharu/sucharupro/ui/customer/wall/components/RunningOffersCarousel.kt`

---

### C. Files Modified
- `app/src/main/java/com/sucharu/sucharupro/ui/customer/wall/SucharuWallScreen.kt`
- `SUCHARU_WALL_STEP_02_REPORT.md`

---

### D. Final Wall Section Order
1. **HEADER** (`HomeHeader`)
2. **MONISHIR BANI + CURRENT DATE** (`DailyWisdomCard`)
3. **HERO BANNER SLIDER** (`HomeBannerPager` - Full Width)
4. **RUNNING PRAYER-TIME WIDGET** (`PrayerTimesCard` - Full Width Running Widget)
5. **OFFERS SLIDER** (`RunningOffersCarousel`)
6. **PRINTING SERVICES** (4-Column Grid)
7. **POPULAR PRODUCTS** (4-Column Grid Rows)
8. **BOTTOM NAVIGATION** (`CustomerBottomNavigation`)

---

### E. Navigation Preservation Verification
- `HomeHeader`:
  - `onProfileClick`: Preserved (`principal != null -> Customer.Profile`, else `Public.Login`).
  - `onNotificationClick`: Preserved (`principal != null -> Customer.Notifications`, else `Public.Login`).
- `CustomerBottomNavigation`:
  - `selectedTab`: Preserved (`HOME` & `ACCOUNT` bottom dock).

---

### F. Existing Card Destinations Preserved
- **Services Grid**:
  - "অফসেট" -> `AppDestination.Public.ProductGallery("Offset", "অফসেট প্রিন্টিং")`
  - "ডিজিটাল" -> `AppDestination.Public.ProductGallery("Digital", "ডিজিটাল প্রিন্ট")`
  - "প্যাকেজিং" -> `AppDestination.Public.ProductGallery("Packaging", "কাস্টম প্যাকেজিং")`
  - "ব্যানার" -> `AppDestination.Public.ProductGallery("Banner", "পিভিসি ব্যানার")`
- **Products Grid**:
  - "ভিজিটিং কার্ড" -> `AppDestination.Public.ProductGallery("Card", "ভিজিটিং কার্ড")`
  - "ব্রোশিওর" -> `AppDestination.Public.ProductGallery("Brochure", "ব্রোশিওর ও ফ্লায়ার")`
  - "রিজিড বক্স" -> `AppDestination.Public.ProductGallery("RigidBox", "রিজিড বক্স")`
  - "ট্যাগ / লেবেল" -> `AppDestination.Public.ProductGallery("Tag", "ট্যাগ ও লেবেল")`
  - "চালান বই" -> `AppDestination.Public.ProductGallery("Challan", "চালান ও ক্যাশ মেমো")`
  - "৩ডি লেটার" -> `AppDestination.Public.ProductGallery("3D", "৩ডি সাইন লেটার")`
  - "স্টিকার" -> `AppDestination.Public.ProductGallery("Sticker", "ডাই-কাট স্টিকার")`
  - "অন্যান্য" -> `AppDestination.Public.ProductGallery("Others", "অন্যান্য সামগ্রী")`

---

### G. Data-Flow Preservation
- `SucharuWallViewModel` and `SucharuWallUiState` handle feed loading (`loadWallFeed(principal)`).
- Zero domain models or database schemas modified.

---

### H. Mock/Demo Data Handling
- Real feed data (`feed.offers`) is rendered when available.
- Fallback items maintain safety without introducing fake customer/financial values.

---

### I. Removed-From-Composition Components
- `TriCalendarCard`: Source file preserved, removed from primary `LazyColumn` composition.
- `SecondaryPromoBanner`: Source file preserved, removed from primary `LazyColumn` composition.
- `Smart Tools` grid: Removed from primary `LazyColumn` composition.

---

### J. Build Results
- `./gradlew :core:jar` — **SUCCESS**
- `./gradlew :backend:jar` — **SUCCESS**
- `./gradlew :app:assembleDebug` — **SUCCESS**

---

### K. Test Results
- All unit tests in `:core`, `:backend`, and `:app` passed 100%.

---

### L. Runtime Verification Result
- **Device**: Motorola Edge 50 (`ZD222PJ6JH`, Android 16 / API 36)
- **Status**: Launched and initialized cleanly with **0 crashes, 0 ANRs, 0 fatal exceptions** in Logcat.

---

### M. Known Limitations
- None.

---

### N. Files NOT Modified
- `:core` module files (Modules 00–24)
- `:backend` module files
- Room/Postgres database migrations
- Auth and session management classes

---

### O. Final Verdict
**PASS** *(Wall layout recomposition completed according to locked section order. Header, Wisdom, Hero, Prayer, Offers, Services, Products, and Bottom Nav flow fully preserved and verified at Level L7).*
