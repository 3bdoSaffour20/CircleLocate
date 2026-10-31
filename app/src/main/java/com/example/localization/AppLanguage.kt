package com.example.localization

enum class AppLanguage(val code: String, val displayName: String, val isRtl: Boolean) {
    ENGLISH("en", "English", false),
    ARABIC("ar", "العربية", true);

    companion object {
        fun fromCode(code: String): AppLanguage {
            return entries.firstOrNull { it.code.equals(code, ignoreCase = true) } ?: ENGLISH
        }
    }
}

object AppStrings {
    fun get(key: String, lang: AppLanguage): String {
        return translations[key]?.get(lang) ?: translations[key]?.get(AppLanguage.ENGLISH) ?: key
    }

    private val translations = mapOf(
        "app_name" to mapOf(
            AppLanguage.ENGLISH to "CircleLocate",
            AppLanguage.ARABIC to "سيركل لوكيت"
        ),
        "tagline" to mapOf(
            AppLanguage.ENGLISH to "Live Phone & Device Location Tracker",
            AppLanguage.ARABIC to "محدد موقع الهواتف والأجهزة المباشر"
        ),
        "enter_name" to mapOf(
            AppLanguage.ENGLISH to "Enter Name",
            AppLanguage.ARABIC to "أدخل الاسم"
        ),
        "name_placeholder" to mapOf(
            AppLanguage.ENGLISH to "e.g. Alex Morgan, Mom, Work Phone",
            AppLanguage.ARABIC to "مثال: أحمد، أمي، هاتف العمل"
        ),
        "type_number" to mapOf(
            AppLanguage.ENGLISH to "Type a Number",
            AppLanguage.ARABIC to "اكتب رقم الهاتف"
        ),
        "number_placeholder" to mapOf(
            AppLanguage.ENGLISH to "555 123 4567",
            AppLanguage.ARABIC to "555 123 4567"
        ),
        "get_location" to mapOf(
            AppLanguage.ENGLISH to "GET LOCATION",
            AppLanguage.ARABIC to "تحديد الموقع"
        ),
        "terms_notice" to mapOf(
            AppLanguage.ENGLISH to "By clicking Get Location, you agree to our Terms of Use and Privacy Policy. Location lookup complies with telecom safety regulations.",
            AppLanguage.ARABIC to "بالنقر على تحديد الموقع، فإنك توافق على شروط الاستخدام وسياسة الخصوصية. عمليات الاستعلام تخضع لضوابط الاتصالات الآمنة."
        ),
        "nav_search" to mapOf(
            AppLanguage.ENGLISH to "Search",
            AppLanguage.ARABIC to "بحث"
        ),
        "nav_map" to mapOf(
            AppLanguage.ENGLISH to "Map View",
            AppLanguage.ARABIC to "الخريطة"
        ),
        "nav_history" to mapOf(
            AppLanguage.ENGLISH to "History",
            AppLanguage.ARABIC to "السجل"
        ),
        "nav_settings" to mapOf(
            AppLanguage.ENGLISH to "Settings",
            AppLanguage.ARABIC to "الإعدادات"
        ),
        "processing_title" to mapOf(
            AppLanguage.ENGLISH to "Locating Device...",
            AppLanguage.ARABIC to "جاري تحديد موقع الجهاز..."
        ),
        "stage_1" to mapOf(
            AppLanguage.ENGLISH to "Connecting to Global Telecom Network Registry...",
            AppLanguage.ARABIC to "الاتصال بسجل شبكات الاتصالات العالمية..."
        ),
        "stage_2" to mapOf(
            AppLanguage.ENGLISH to "Analyzing MCC/MNC and Carrier Signal Route...",
            AppLanguage.ARABIC to "تحليل إشارة المشغل ومسار التغطية..."
        ),
        "stage_3" to mapOf(
            AppLanguage.ENGLISH to "Triangulating Cell Towers & Wi-Fi Nodes...",
            AppLanguage.ARABIC to "تثليث أبراج الإرسال ونقاط الاتصال..."
        ),
        "stage_4" to mapOf(
            AppLanguage.ENGLISH to "Acquiring Encrypted GPS Coordinates & Altitude...",
            AppLanguage.ARABIC to "الحصول على إحداثيات نظام تحديد المواقع المشفرة..."
        ),
        "stage_5" to mapOf(
            AppLanguage.ENGLISH to "Device Location Verified & Locked!",
            AppLanguage.ARABIC to "تم التحقق وتثبيت موقع الجهاز بنجاح!"
        ),
        "device_found" to mapOf(
            AppLanguage.ENGLISH to "Device Located",
            AppLanguage.ARABIC to "تم العثور على الجهاز"
        ),
        "coordinates" to mapOf(
            AppLanguage.ENGLISH to "Coordinates",
            AppLanguage.ARABIC to "الإحداثيات"
        ),
        "carrier" to mapOf(
            AppLanguage.ENGLISH to "Carrier",
            AppLanguage.ARABIC to "شركة الاتصالات"
        ),
        "accuracy" to mapOf(
            AppLanguage.ENGLISH to "Accuracy",
            AppLanguage.ARABIC to "دقة التحديد"
        ),
        "battery" to mapOf(
            AppLanguage.ENGLISH to "Battery",
            AppLanguage.ARABIC to "البطارية"
        ),
        "status_online" to mapOf(
            AppLanguage.ENGLISH to "Live / Active",
            AppLanguage.ARABIC to "متصل / نشط"
        ),
        "speed" to mapOf(
            AppLanguage.ENGLISH to "Speed",
            AppLanguage.ARABIC to "السرعة"
        ),
        "last_seen" to mapOf(
            AppLanguage.ENGLISH to "Last Updated",
            AppLanguage.ARABIC to "آخر تحديث"
        ),
        "view_on_map" to mapOf(
            AppLanguage.ENGLISH to "View on Map",
            AppLanguage.ARABIC to "عرض على الخريطة"
        ),
        "track_again" to mapOf(
            AppLanguage.ENGLISH to "Track Again",
            AppLanguage.ARABIC to "إعادة التتبع"
        ),
        "copy_coords" to mapOf(
            AppLanguage.ENGLISH to "Copy Coordinates",
            AppLanguage.ARABIC to "نسخ الإحداثيات"
        ),
        "coords_copied" to mapOf(
            AppLanguage.ENGLISH to "Coordinates copied to clipboard",
            AppLanguage.ARABIC to "تم نسخ الإحداثيات إلى الحافظة"
        ),
        "history_title" to mapOf(
            AppLanguage.ENGLISH to "Tracking History",
            AppLanguage.ARABIC to "سجل التتبع"
        ),
        "history_empty" to mapOf(
            AppLanguage.ENGLISH to "No tracking records yet",
            AppLanguage.ARABIC to "لا توجد سجلات تتبع سابقة"
        ),
        "history_empty_hint" to mapOf(
            AppLanguage.ENGLISH to "Search for any phone number on the Home screen to view and record its live location.",
            AppLanguage.ARABIC to "ابحث عن أي رقم هاتف من الشاشة الرئيسية لعرض وتسجيل موقعه المباشر."
        ),
        "clear_all" to mapOf(
            AppLanguage.ENGLISH to "Clear All",
            AppLanguage.ARABIC to "مسح الكل"
        ),
        "clear_history_confirm" to mapOf(
            AppLanguage.ENGLISH to "Are you sure you want to delete all tracking history?",
            AppLanguage.ARABIC to "هل أنت متأكد من رغبتك في مسح كافة سجلات التتبع؟"
        ),
        "delete" to mapOf(
            AppLanguage.ENGLISH to "Delete",
            AppLanguage.ARABIC to "حذف"
        ),
        "cancel" to mapOf(
            AppLanguage.ENGLISH to "Cancel",
            AppLanguage.ARABIC to "إلغاء"
        ),
        "settings_title" to mapOf(
            AppLanguage.ENGLISH to "Settings & Preferences",
            AppLanguage.ARABIC to "الإعدادات والتفضيلات"
        ),
        "language_section" to mapOf(
            AppLanguage.ENGLISH to "Language",
            AppLanguage.ARABIC to "اللغة"
        ),
        "theme_section" to mapOf(
            AppLanguage.ENGLISH to "Appearance & Theme",
            AppLanguage.ARABIC to "المظهر والسمة"
        ),
        "dark_mode" to mapOf(
            AppLanguage.ENGLISH to "Dark Mode",
            AppLanguage.ARABIC to "الوضع الداكن"
        ),
        "light_mode" to mapOf(
            AppLanguage.ENGLISH to "Light Mode",
            AppLanguage.ARABIC to "الوضع الفاتح"
        ),
        "updates_section" to mapOf(
            AppLanguage.ENGLISH to "App Updates",
            AppLanguage.ARABIC to "تحديثات التطبيق"
        ),
        "check_updates" to mapOf(
            AppLanguage.ENGLISH to "Check for Updates",
            AppLanguage.ARABIC to "التحقق من وجود تحديثات"
        ),
        "checking_updates" to mapOf(
            AppLanguage.ENGLISH to "Checking update servers...",
            AppLanguage.ARABIC to "جاري فحص خوادم التحديث..."
        ),
        "up_to_date" to mapOf(
            AppLanguage.ENGLISH to "You're using the latest version of CircleLocate (v1.0.0). All location algorithms are up to date.",
            AppLanguage.ARABIC to "أنت تستخدم أحدث إصدار من سيركل لوكيت (v1.0.0). كافة خوارزميات تحديد الموقع محدثة بالكامل."
        ),
        "permissions_section" to mapOf(
            AppLanguage.ENGLISH to "Device Permissions",
            AppLanguage.ARABIC to "أذونات الجهاز"
        ),
        "gps_permission" to mapOf(
            AppLanguage.ENGLISH to "GPS & Location Access",
            AppLanguage.ARABIC to "الوصول للموقع و GPS"
        ),
        "permission_granted" to mapOf(
            AppLanguage.ENGLISH to "Granted - Live positioning active",
            AppLanguage.ARABIC to "ممنوح - التموضع المباشر نشط"
        ),
        "permission_needed" to mapOf(
            AppLanguage.ENGLISH to "Not granted - Tap to enable GPS",
            AppLanguage.ARABIC to "غير ممنوح - اضغط لتفعيل GPS"
        ),
        "cloud_sync_section" to mapOf(
            AppLanguage.ENGLISH to "Cloud Sync & Backend API",
            AppLanguage.ARABIC to "المزامنة السحابية وخادم API"
        ),
        "cloud_sync_desc" to mapOf(
            AppLanguage.ENGLISH to "Real-time tracking synchronization & Supabase cloud backend.",
            AppLanguage.ARABIC to "المزامنة المباشرة للتتبع وقاعدة بيانات سوبابيس السحابية."
        ),
        "maps_api_section" to mapOf(
            AppLanguage.ENGLISH to "Google Maps Platform",
            AppLanguage.ARABIC to "منصة خرائط جوجل"
        ),
        "maps_api_desc" to mapOf(
            AppLanguage.ENGLISH to "Interactive high-precision radar map is active. Optional custom API keys can be specified in .env",
            AppLanguage.ARABIC to "خريطة الرادار التفاعلية عالية الدقة نشطة. يمكن إضافة مفاتيح مخصصة في ملف .env"
        ),
        "about_app" to mapOf(
            AppLanguage.ENGLISH to "About CircleLocate",
            AppLanguage.ARABIC to "حول سيركل لوكيت"
        ),
        "about_text" to mapOf(
            AppLanguage.ENGLISH to "CircleLocate provides advanced cell network and GPS location tracking simulation, route history recording, and visual mapping with a vibrant modern design.",
            AppLanguage.ARABIC to "يوفر سيركل لوكيت محاكاة متطورة لتتبع مواقع أجهزة الاتصال والخرائط التفاعلية وتسجيل مسار الحركة بتصميم عصري وجذاب."
        ),
        "validation_empty" to mapOf(
            AppLanguage.ENGLISH to "Please enter both name and a valid phone number",
            AppLanguage.ARABIC to "يرجى إدخال الاسم ورقم هاتف صالح"
        ),
        "distance_away" to mapOf(
            AppLanguage.ENGLISH to "Away from your location",
            AppLanguage.ARABIC to "المسافة من موقعك الحالي"
        ),
        "map_layer" to mapOf(
            AppLanguage.ENGLISH to "Map Style",
            AppLanguage.ARABIC to "نمط الخريطة"
        ),
        "style_street" to mapOf(
            AppLanguage.ENGLISH to "Standard",
            AppLanguage.ARABIC to "القياسي"
        ),
        "style_satellite" to mapOf(
            AppLanguage.ENGLISH to "Satellite",
            AppLanguage.ARABIC to "قمر صناعي"
        ),
        "style_radar" to mapOf(
            AppLanguage.ENGLISH to "Dark Radar",
            AppLanguage.ARABIC to "رادار داكن"
        ),
        "style_terrain" to mapOf(
            AppLanguage.ENGLISH to "Terrain",
            AppLanguage.ARABIC to "تضاريس"
        ),
        "account_title" to mapOf(
            AppLanguage.ENGLISH to "User Account",
            AppLanguage.ARABIC to "حساب المستخدم"
        ),
        "instant_login" to mapOf(
            AppLanguage.ENGLISH to "Instant Login",
            AppLanguage.ARABIC to "تسجيل دخول فوري"
        ),
        "welcome_back" to mapOf(
            AppLanguage.ENGLISH to "Welcome Back!",
            AppLanguage.ARABIC to "مرحباً بعودتك!"
        ),
        "welcome_back_desc" to mapOf(
            AppLanguage.ENGLISH to "An existing account was found on this device. You can log in instantly.",
            AppLanguage.ARABIC to "تم العثور على حساب مسجل على هذا الجهاز. يمكنك تسجيل الدخول فوراً."
        ),
        "create_account" to mapOf(
            AppLanguage.ENGLISH to "Create Account",
            AppLanguage.ARABIC to "إنشاء حساب"
        ),
        "create_account_desc" to mapOf(
            AppLanguage.ENGLISH to "No account found on this app. Create an account to securely save and sync your tracked devices.",
            AppLanguage.ARABIC to "لا يوجد حساب مسجل. أنشئ حساباً لحفظ ومزامنة سجلات التتبع بأمان."
        ),
        "full_name" to mapOf(
            AppLanguage.ENGLISH to "Full Name",
            AppLanguage.ARABIC to "الاسم الكامل"
        ),
        "email_or_phone" to mapOf(
            AppLanguage.ENGLISH to "Email or Phone Number",
            AppLanguage.ARABIC to "البريد الإلكتروني أو الهاتف"
        ),
        "security_pin" to mapOf(
            AppLanguage.ENGLISH to "Security PIN",
            AppLanguage.ARABIC to "رمز الأمان (PIN)"
        ),
        "auto_login_label" to mapOf(
            AppLanguage.ENGLISH to "Remember me (Instant login next time)",
            AppLanguage.ARABIC to "تذكرني (تسجيل دخول فوري في المرة القادمة)"
        ),
        "create_and_enter" to mapOf(
            AppLanguage.ENGLISH to "CREATE ACCOUNT & ENTER",
            AppLanguage.ARABIC to "إنشاء الحساب والدخول"
        ),
        "already_have_account" to mapOf(
            AppLanguage.ENGLISH to "Already have an account? Log In",
            AppLanguage.ARABIC to "لديك حساب بالفعل؟ تسجيل الدخول"
        ),
        "no_account_yet" to mapOf(
            AppLanguage.ENGLISH to "New user? Create an account",
            AppLanguage.ARABIC to "مستخدم جديد؟ إنشاء حساب"
        ),
        "switch_account" to mapOf(
            AppLanguage.ENGLISH to "Switch Account",
            AppLanguage.ARABIC to "تبديل الحساب"
        ),
        "logout" to mapOf(
            AppLanguage.ENGLISH to "Log Out",
            AppLanguage.ARABIC to "تسجيل الخروج"
        ),
        "active_account" to mapOf(
            AppLanguage.ENGLISH to "Active Account",
            AppLanguage.ARABIC to "الحساب النشط"
        ),
        "guest_mode" to mapOf(
            AppLanguage.ENGLISH to "Continue as Guest",
            AppLanguage.ARABIC to "المتابعة كضيف"
        )
    )
}
