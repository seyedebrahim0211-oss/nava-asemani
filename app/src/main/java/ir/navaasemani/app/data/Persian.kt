package ir.navaasemani.app.data

import android.icu.util.IslamicCalendar
import java.util.Locale

object Persian {
    private val digits = charArrayOf('۰', '۱', '۲', '۳', '۴', '۵', '۶', '۷', '۸', '۹')

    fun digits(text: String): String = buildString(text.length) {
        for (c in text) append(if (c in '0'..'9') digits[c - '0'] else c)
    }

    /** یکسان‌سازی برای جست‌وجو: ی/ک عربی، نیم‌فاصله، اعراب، ارقام */
    fun normalize(text: String): String = buildString(text.length) {
        for (c in text.lowercase(Locale.ROOT)) {
            when {
                c == 'ي' || c == 'ى' -> append('ی')
                c == 'ك' -> append('ک')
                c == 'ة' -> append('ه')
                c == 'أ' || c == 'إ' || c == 'آ' -> append('ا')
                c == 'ؤ' -> append('و')
                c in 'ً'..'ٟ' || c == 'ٰ' || c == 'ـ' -> Unit
                c == '‌' || c == '‏' || c == '-' || c == '_' -> append(' ')
                c in '۰'..'۹' -> append('0' + (c - '۰'))
                c in '٠'..'٩' -> append('0' + (c - '٠'))
                else -> append(c)
            }
        }
    }.replace(Regex("\\s+"), " ").trim()

    fun time(ms: Long): String {
        if (ms <= 0) return "0:00"
        val total = ms / 1000
        val h = total / 3600
        val m = (total % 3600) / 60
        val s = total % 60
        return if (h > 0) String.format(Locale.US, "%d:%02d:%02d", h, m, s)
        else String.format(Locale.US, "%d:%02d", m, s)
    }

    private val hijriMonths = listOf(
        "محرم", "صفر", "ربیع‌الاول", "ربیع‌الثانی", "جمادی‌الاول", "جمادی‌الثانی",
        "رجب", "شعبان", "رمضان", "شوال", "ذی‌القعده", "ذی‌الحجه"
    )

    data class HijriDate(val day: Int, val month: Int, val year: Int) {
        val label: String get() = "$day ${hijriMonths.getOrElse(month) { "" }} $year"
    }

    fun hijriToday(): HijriDate? = runCatching {
        val cal = IslamicCalendar()
        cal.calculationType = IslamicCalendar.CalculationType.ISLAMIC_UMALQURA
        HijriDate(
            cal.get(android.icu.util.Calendar.DAY_OF_MONTH),
            cal.get(android.icu.util.Calendar.MONTH),
            cal.get(android.icu.util.Calendar.YEAR)
        )
    }.getOrNull()

    /** شناسه مجموعه مناسب مناسبت امروز بر اساس ماه قمری */
    fun occasionCollection(date: HijriDate?): String = when (date?.month) {
        0 -> "muharram"
        1 -> if ((date?.day ?: 1) <= 21) "arbaeen" else "muharram"
        4, 5 -> "fatimiyeh"
        2, 3, 6, 7 -> "mahdavi"
        else -> "fatimiyeh"
    }

    fun greeting(hour: Int): String = when (hour) {
        in 4..11 -> "صبح بخیر"
        in 12..16 -> "ظهر بخیر"
        in 17..19 -> "عصر بخیر"
        else -> "شب بخیر"
    }
}
