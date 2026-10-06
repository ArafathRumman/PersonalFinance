package com.taka.personalfinance.data

object DefaultCategories {
    fun all(): List<CategoryEntity> {
        fun e(en: String, bn: String, icon: String) = CategoryEntity(name = en, nameBn = bn, type = TxType.EXPENSE, icon = icon, isDefault = true)
        fun i(en: String, bn: String, icon: String) = CategoryEntity(name = en, nameBn = bn, type = TxType.INCOME, icon = icon, isDefault = true)
        return listOf(
            e("Food & Dining", "খাবার", "🍽️"),
            e("Groceries", "বাজার", "🛒"),
            e("Transport", "যাতায়াত", "🚌"),
            e("Rent", "বাসা ভাড়া", "🏠"),
            e("Utility Bills", "ইউটিলিটি বিল", "💡"),
            e("Mobile & Internet", "মোবাইল ও ইন্টারনেট", "📱"),
            e("Health", "স্বাস্থ্য", "💊"),
            e("Education", "শিক্ষা", "🎓"),
            e("Shopping", "কেনাকাটা", "🛍️"),
            e("Entertainment", "বিনোদন", "🎬"),
            e("Family", "পরিবার", "👪"),
            e("Gifts & Charity", "উপহার ও দান", "🎁"),
            e("Travel", "ভ্রমণ", "✈️"),
            e("Other", "অন্যান্য", "📦"),
            i("Salary", "বেতন", "💼"),
            i("Business", "ব্যবসা", "🏪"),
            i("Freelance", "ফ্রিল্যান্স", "💻"),
            i("Gift", "উপহার", "🎁"),
            i("Investment", "বিনিয়োগ", "📈"),
            i("Other Income", "অন্যান্য আয়", "💰"),
        )
    }
}
