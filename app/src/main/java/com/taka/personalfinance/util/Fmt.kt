package com.taka.personalfinance.util

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.taka.personalfinance.data.CategoryEntity
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import kotlin.math.abs

data class CurrencyInfo(
    val code: String,
    val symbol: String,
    val nameEn: String,
    val nameBn: String,
    /** true: 12,34,567 (lakh/crore) grouping; false: 1,234,567 */
    val southAsian: Boolean,
)

object Currencies {
    val all = listOf(
        CurrencyInfo("BDT", "৳", "Bangladeshi Taka", "বাংলাদেশি টাকা", true),
        CurrencyInfo("INR", "₹", "Indian Rupee", "ভারতীয় রুপি", true),
        CurrencyInfo("PKR", "Rs ", "Pakistani Rupee", "পাকিস্তানি রুপি", true),
        CurrencyInfo("NPR", "Rs ", "Nepalese Rupee", "নেপালি রুপি", true),
        CurrencyInfo("LKR", "Rs ", "Sri Lankan Rupee", "শ্রীলঙ্কান রুপি", true),
        CurrencyInfo("USD", "$", "US Dollar", "মার্কিন ডলার", false),
        CurrencyInfo("EUR", "€", "Euro", "ইউরো", false),
        CurrencyInfo("GBP", "£", "British Pound", "ব্রিটিশ পাউন্ড", false),
        CurrencyInfo("SAR", "SAR ", "Saudi Riyal", "সৌদি রিয়াল", false),
        CurrencyInfo("AED", "AED ", "UAE Dirham", "সংযুক্ত আরব আমিরাত দিরহাম", false),
        CurrencyInfo("MYR", "RM ", "Malaysian Ringgit", "মালয়েশিয়ান রিঙ্গিত", false),
        CurrencyInfo("SGD", "S$", "Singapore Dollar", "সিঙ্গাপুর ডলার", false),
        CurrencyInfo("QAR", "QAR ", "Qatari Riyal", "কাতারি রিয়াল", false),
        CurrencyInfo("KWD", "KWD ", "Kuwaiti Dinar", "কুয়েতি দিনার", false),
    )

    fun byCode(code: String?): CurrencyInfo = all.firstOrNull { it.code == code } ?: all.first()
}

object Fmt {
    /** Observable: screens that format money redraw automatically when this changes. */
    var currency: CurrencyInfo by mutableStateOf(Currencies.all.first())

    private val BN_DIGITS = charArrayOf('০', '১', '২', '৩', '৪', '৫', '৬', '৭', '৮', '৯')
    private val BN_LOCALE: Locale = Locale.forLanguageTag("bn-BD")

    private fun loc(bn: Boolean): Locale = if (bn) BN_LOCALE else Locale.ENGLISH

    fun toBnDigits(s: String): String {
        val sb = StringBuilder(s.length)
        for (c in s) sb.append(if (c in '0'..'9') BN_DIGITS[c - '0'] else c)
        return sb.toString()
    }

    fun toAsciiDigits(s: String): String {
        val sb = StringBuilder(s.length)
        for (c in s) {
            val idx = BN_DIGITS.indexOf(c)
            sb.append(if (idx >= 0) ('0' + idx) else c)
        }
        return sb.toString()
    }

    fun num(s: String, bn: Boolean): String = if (bn) toBnDigits(s) else s
    fun num(n: Int, bn: Boolean): String = num(n.toString(), bn)

    private fun group(intPart: String): String =
        if (currency.southAsian) groupBd(intPart) else groupWestern(intPart)

    private fun groupWestern(intPart: String): String {
        if (intPart.length <= 3) return intPart
        val parts = ArrayList<String>()
        var rest = intPart
        while (rest.length > 3) {
            parts.add(0, rest.takeLast(3))
            rest = rest.dropLast(3)
        }
        if (rest.isNotEmpty()) parts.add(0, rest)
        return parts.joinToString(",")
    }

    /** Bangladeshi digit grouping: 12,34,567 */
    fun groupBd(intPart: String): String {
        if (intPart.length <= 3) return intPart
        val last3 = intPart.takeLast(3)
        var rest = intPart.dropLast(3)
        val parts = ArrayList<String>()
        while (rest.length > 2) {
            parts.add(0, rest.takeLast(2))
            rest = rest.dropLast(2)
        }
        if (rest.isNotEmpty()) parts.add(0, rest)
        return parts.joinToString(",") + "," + last3
    }

    /** "৳1,23,456.50" (symbol follows the currency setting; decimals only when needed). */
    fun money(minor: Long, bn: Boolean): String {
        val a = abs(minor)
        val whole = a / 100
        val frac = a % 100
        var body = group(whole.toString())
        if (frac != 0L) body += "." + frac.toString().padStart(2, '0')
        val sign = if (minor < 0) "-" else ""
        return num(sign + currency.symbol + body, bn)
    }

    /** Plain number for an input box: "1200" or "1200.50" */
    fun editable(minor: Long): String {
        val a = abs(minor)
        val whole = a / 100
        val frac = a % 100
        return if (frac == 0L) whole.toString() else whole.toString() + "." + frac.toString().padStart(2, '0')
    }

    /** Short form for charts: 1.2K, 3.4 L (lakh), 1.1 Cr (crore) or K / M / B for other currencies. */
    fun compact(minor: Long, bn: Boolean): String {
        val unit = abs(minor) / 100.0
        val sign = if (minor < 0) "-" else ""
        val body = if (currency.southAsian) {
            when {
                unit >= 1e7 -> oneDec(unit / 1e7) + (if (bn) " কোটি" else " Cr")
                unit >= 1e5 -> oneDec(unit / 1e5) + (if (bn) " লাখ" else " L")
                unit >= 1e3 -> oneDec(unit / 1e3) + (if (bn) " হাজার" else "K")
                else -> unit.toLong().toString()
            }
        } else {
            when {
                unit >= 1e9 -> oneDec(unit / 1e9) + "B"
                unit >= 1e6 -> oneDec(unit / 1e6) + "M"
                unit >= 1e3 -> oneDec(unit / 1e3) + "K"
                else -> unit.toLong().toString()
            }
        }
        return num(sign + currency.symbol + body, bn)
    }

    private fun oneDec(v: Double): String =
        String.format(Locale.ENGLISH, "%.1f", v).removeSuffix(".0")

    fun percent(fraction: Double, bn: Boolean): String =
        num(String.format(Locale.ENGLISH, "%.0f", fraction * 100) + "%", bn)

    /** Accepts English or Bengali digits. Returns paisa, or null if invalid. */
    fun parseAmount(raw: String): Long? {
        val s = toAsciiDigits(raw).replace(",", "").replace(" ", "").trim()
        if (s.isEmpty()) return null
        if (!Regex("^\\d{1,12}(\\.\\d{0,2})?$").matches(s)) return null
        return try {
            BigDecimal(s.trimEnd('.')).movePointRight(2).longValueExact()
        } catch (e: ArithmeticException) {
            null
        } catch (e: NumberFormatException) {
            null
        }
    }

    fun date(epochDay: Long, bn: Boolean): String =
        num(LocalDate.ofEpochDay(epochDay).format(DateTimeFormatter.ofPattern("d MMM yyyy", loc(bn))), bn)

    fun dayHeader(epochDay: Long, bn: Boolean): String =
        num(LocalDate.ofEpochDay(epochDay).format(DateTimeFormatter.ofPattern("EEE, d MMM yyyy", loc(bn))), bn)

    fun monthYear(ym: YearMonth, bn: Boolean): String =
        num(ym.format(DateTimeFormatter.ofPattern("MMMM yyyy", loc(bn))), bn)

    fun monthShort(ym: YearMonth, bn: Boolean): String =
        ym.month.getDisplayName(TextStyle.SHORT, loc(bn))

    fun weekdayShort(dow: DayOfWeek, bn: Boolean): String =
        dow.getDisplayName(TextStyle.SHORT, loc(bn))

    fun utcMillisToEpochDay(ms: Long): Long = Math.floorDiv(ms, 86_400_000L)
    fun epochDayToUtcMillis(day: Long): Long = day * 86_400_000L
}

fun categoryLabel(c: CategoryEntity?, bn: Boolean): String = when {
    c == null -> if (bn) "শ্রেণিহীন" else "Uncategorized"
    bn && c.nameBn.isNotBlank() -> c.nameBn
    else -> c.name
}
