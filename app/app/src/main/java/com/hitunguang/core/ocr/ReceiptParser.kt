package com.hitunguang.core.ocr

import java.util.Calendar
import java.util.regex.Pattern

/**
 * ReceiptParser v2 - Multi-stage pipeline for Indonesian receipts.
 * v2 adds: smart currency normalization, fuzzy keyword matching,
 *          digital receipt support, statistical fallback.
 *
 * Pipeline:
 *  1. Segment lines into header / body / footer using separator lines and totals keywords.
 *  2. Merchant is looked up in the header, items only in the body, amounts only in the footer.
 *  3. Keyword matching is token based (not substring) so item names are never dropped by
 *     accident, with fuzzy matching to survive OCR typos on the totals block.
 */
object ReceiptParser {

    private val MEASUREMENT_TOKENS = listOf(
        "g", "gr", "gram", "kg", "mg", "ml", "l", "ltr", "liter", "pcs",
        "pc", "pack", "sachet", "s", "cm", "mm", "oz", "lusin", "lsn",
        "pck", "box", "dus", "bks", "btl", "botol", "ktk", "kemasan",
        "roll", "set", "pasang"
    )

    private val HEADER_KEYWORDS = listOf(
        "nama barang", "deskripsi", "item", "produk", "nama produk",
        "qty", "jumlah", "harga", "subtotal", "price", "amount",
        "rp", "barang", "nama", "unit", "satuan"
    )

    private val FOOTER_KEYWORDS = listOf(
        "total", "total belanja", "subtotal", "sub total", "ppn", "pajak",
        "pb1", "tax", "diskon", "discount", "tunai", "cash", "kembalian",
        "kembali", "grand total", "bayar", "payment", "tender", "change",
        "kartu", "debit", "kredit", "qris", "card", "service", "charge",
        "biaya", "admin", "delivery", "ongkir", "biaya kirim", "shopee pay",
        "gopay", "ovo", "dana", "linkaja", "ovo points", "promo",
        "voucher", "kupon", "reward", "member", "point", "loyalty",
        "donation", "bulatkan", "rounded", "saving", "tabungan",
        "refund", "pengembalian dana", "biaya penanganan", "handling fee"
    )

    private val PROMOTIONAL_HINTS = listOf(
        "belanja hemat", "setiap hari", "promo", "member", "diskon spesial",
        "terima kasih", "thank you", "thankyou", "terimakasih", "selamat datang",
        "kunjungi", "download", "aplikasi", "website", "fb", "ig", "instagram",
        "facebook", "twitter", "gratis", "free", "undian", "hadiah"
    )

    private val MONTH_MAP = mapOf(
        "jan" to 0, "januari" to 0, "january" to 0,
        "feb" to 1, "februari" to 1, "february" to 1,
        "mar" to 2, "maret" to 2, "march" to 2,
        "apr" to 3, "april" to 3,
        "mei" to 4, "may" to 4,
        "jun" to 5, "juni" to 5, "june" to 5,
        "jul" to 6, "juli" to 6, "july" to 6,
        "agu" to 7, "agustus" to 7, "august" to 7, "agt" to 7,
        "sep" to 8, "september" to 8,
        "okt" to 9, "oktober" to 9, "october" to 9, "oct" to 9,
        "nov" to 10, "november" to 10,
        "des" to 11, "desember" to 11, "december" to 11, "dec" to 11
    )

    /** Keywords that mark the beginning of the totals block at the bottom of a receipt. */
    private val TOTAL_BLOCK_KEYWORDS = listOf(
        "total", "sub total", "subtotal", "grand total", "ppn", "pb1", "pajak",
        "tax", "tunai", "cash", "kembali", "kembalian", "bayar", "service charge",
        "biaya admin", "handling fee"
    )

    private val TOTAL_KEYWORDS = listOf(
        "total", "grand total", "jumlah", "total bayar", "total belanja",
        "netto", "net", "bayar"
    )

    private val TOTAL_EXCLUDE_KEYWORDS = listOf(
        "subtotal", "sub total", "pajak", "ppn", "tax", "kembali", "kembalian",
        "cash", "tunai", "change"
    )

    private val TAX_KEYWORDS = listOf("ppn", "tax", "pajak", "pb1", "vat")

    private val TAX_EXCLUDE_KEYWORDS = listOf("before", "excluding", "tanpa")

    private val SUBTOTAL_KEYWORDS = listOf("subtotal", "sub total", "jumlah sebelum")

    /** Tokens that never belong to a purchasable item line. */
    private val ITEM_SKIP_KEYWORDS = listOf(
        "total", "jumlah", "bayar", "tunai", "cash", "kembali", "kembalian",
        "change", "ppn", "tax", "pajak", "pb1", "debit", "kredit", "card",
        "kartu", "tanggal", "tgl", "date", "time", "jam", "telp", "tlp",
        "phone", "npwp", "alamat", "subtotal", "sub total", "promo", "diskon",
        "discount", "voucher", "kasir", "cashier", "struk", "nota", "invoice",
        "ovo", "gopay", "shopeepay", "dana", "linkaja", "qris", "bca",
        "mandiri", "bri", "bni", "visa", "mastercard", "service", "charge",
        "sc", "payment", "member", "point", "poin"
    )

    /** Tokens that mark an address / contact line, used when picking the merchant name. */
    private val ADDRESS_HINTS = listOf(
        "jl", "jln", "jalan", "no", "rt", "rw", "kel", "kec", "blok", "perum",
        "komplek", "kompleks", "telp", "tlp", "telepon", "phone", "npwp",
        "email", "www"
    )

    private val SEPARATOR_CHARS = setOf('-', '=', '*', '.', '_', '~', '#', '+')

    /** Quantities outside this range are OCR noise, not a real purchase count. */
    private const val MIN_QTY = 0.01
    private const val MAX_QTY = 999.0

    /** Below this length a single edit distance produces false positives. */
    private const val FUZZY_MIN_LENGTH = 7

    fun parse(rawText: String?): ParsedReceipt {
        if (rawText.isNullOrBlank()) {
            return ParsedReceipt(null, null, null, null, 0L, emptyList())
        }

        val lines = rawText.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        if (lines.isEmpty()) {
            return ParsedReceipt(null, null, null, null, 0L, emptyList())
        }

        val footerStart = findFooterStart(lines)
        val bodyStart = findBodyStart(lines, footerStart)

        val headerLines = lines.subList(0, bodyStart.coerceIn(0, lines.size))
        val bodyLines = if (bodyStart < footerStart) lines.subList(bodyStart, footerStart) else emptyList()
        // When no totals block is detected, fall back to scanning the whole receipt.
        val footerLines = if (footerStart < lines.size) lines.subList(footerStart, lines.size) else lines

        val merchantName = parseMerchantName(if (headerLines.isEmpty()) lines.take(1) else headerLines)
        val date = parseDate(lines)
        val tax = parseTax(footerLines)
        val total = parseTotal(footerLines)
        val subtotal = parseSubtotal(footerLines) ?: (total - (tax ?: 0L)).coerceAtLeast(0L)
        val items = parseItems(bodyLines, merchantName)

        return ParsedReceipt(
            merchantName = merchantName,
            date = date,
            subtotal = subtotal,
            tax = tax,
            total = total,
            items = items
        )
    }

    // ---------------------------------------------------------------- segmentation

    /**
     * Index of the first line belonging to the totals block, or [lines].size when the
     * receipt has no recognizable totals.
     */
    private fun findFooterStart(lines: List<String>): Int {
        for (i in lines.indices) {
            val line = lines[i]
            if (isSeparatorLine(line)) continue
            if (matchesKeyword(line, TOTAL_BLOCK_KEYWORDS, fuzzy = true) && extractLastNumber(line) != null) {
                return i
            }
        }
        return lines.size
    }

    /**
     * Index of the first line that may contain items. Items start after the last
     * separator or column header that still leaves item-like content before the totals
     * block, which drops address, date and tax-id lines from the item candidates.
     */
    private fun findBodyStart(lines: List<String>, footerStart: Int): Int {
        var candidate = -1
        for (i in 0 until footerStart) {
            val isDelimiter = isSeparatorLine(lines[i]) ||
                (matchesKeyword(lines[i], HEADER_KEYWORDS) && extractLastNumber(lines[i]) == null)
            if (isDelimiter && hasItemLikeContent(lines, i + 1, footerStart)) {
                candidate = i + 1
            }
        }
        if (candidate >= 0) return candidate
        // No usable delimiter: assume only the merchant line is header.
        return if (footerStart > 1) 1 else 0
    }

    private fun hasItemLikeContent(lines: List<String>, from: Int, until: Int): Boolean =
        (from until until).any { !isSeparatorLine(lines[it]) && lines[it].any { c -> c.isDigit() } }

    private fun isSeparatorLine(line: String): Boolean {
        if (line.length < 3) return false
        return line.all { it in SEPARATOR_CHARS }
    }

    // ---------------------------------------------------------------- merchant

    private fun parseMerchantName(headerLines: List<String>): String? {
        for (line in headerLines) {
            if (line.length < 3) continue
            if (isSeparatorLine(line)) continue
            if (line.matches(Regex(".*\\b\\d{4}\\b.*"))) continue
            if (line.replace(Regex("[^0-9]"), "").length > line.length * 0.5) continue
            if (matchesKeyword(line, ADDRESS_HINTS)) continue
            if (matchesKeyword(line, PROMOTIONAL_HINTS)) continue
            if (line.count { it.isLetter() } < 3) continue

            return line.trim()
        }
        return null
    }

    // ---------------------------------------------------------------- date

    /**
     * Collects every date-like candidate and prefers one sitting on a line that is
     * explicitly labelled as a date. Candidates are validated strictly, so an OCR
     * misread such as 31/02/2026 is rejected instead of silently rolling over.
     */
    private fun parseDate(lines: List<String>): Long? {
        val dateLabels = listOf("tanggal", "tgl", "date", "waktu", "time")
        var firstValid: Long? = null

        for (line in lines) {
            val candidate = extractDateFromLine(line) ?: continue
            if (matchesKeyword(line, dateLabels)) {
                return candidate
            }
            if (firstValid == null) {
                firstValid = candidate
            }
        }
        return firstValid
    }

    private fun extractDateFromLine(line: String): Long? {
        val normalized = line.lowercase()

        val numericMatcher = Pattern
            .compile("\\b(\\d{1,2})[/-](\\d{1,2})[/-](\\d{2,4})\\b")
            .matcher(normalized)
        while (numericMatcher.find()) {
            val day = numericMatcher.group(1)?.toIntOrNull()
            val month = numericMatcher.group(2)?.toIntOrNull()
            val year = normalizeYear(numericMatcher.group(3)?.toIntOrNull())
            if (day != null && month != null && year != null) {
                toTimestamp(year, month - 1, day)?.let { return it }
            }
        }

        val isoMatcher = Pattern
            .compile("\\b(\\d{4})[/-](\\d{1,2})[/-](\\d{1,2})\\b")
            .matcher(normalized)
        while (isoMatcher.find()) {
            val year = isoMatcher.group(1)?.toIntOrNull()
            val month = isoMatcher.group(2)?.toIntOrNull()
            val day = isoMatcher.group(3)?.toIntOrNull()
            if (day != null && month != null && year != null) {
                toTimestamp(year, month - 1, day)?.let { return it }
            }
        }

        val wordMatcher = Pattern
            .compile("\\b(\\d{1,2})\\s+([a-z]{3,9})\\s+(\\d{2,4})\\b")
            .matcher(normalized)
        while (wordMatcher.find()) {
            val day = wordMatcher.group(1)?.toIntOrNull()
            val monthIndex = wordMatcher.group(2)?.let { MONTH_MAP[it] }
            val year = normalizeYear(wordMatcher.group(3)?.toIntOrNull())
            if (day != null && monthIndex != null && year != null) {
                toTimestamp(year, monthIndex, day)?.let { return it }
            }
        }

        return null
    }

    private fun normalizeYear(year: Int?): Int? {
        if (year == null) return null
        return if (year < 100) year + 2000 else year
    }

    /** Strict calendar conversion: invalid dates and implausible years return null. */
    private fun toTimestamp(year: Int, monthIndex: Int, day: Int): Long? {
        val currentYear = Calendar.getInstance().get(Calendar.YEAR)
        if (year < 2000 || year > currentYear + 1) return null
        if (monthIndex !in 0..11 || day !in 1..31) return null

        return try {
            val cal = Calendar.getInstance()
            cal.isLenient = false
            cal.clear()
            cal.set(year, monthIndex, day, 12, 0, 0)
            cal.timeInMillis
        } catch (e: Exception) {
            null
        }
    }

    // ---------------------------------------------------------------- amounts

    private fun parseTotal(lines: List<String>): Long {
        for (i in lines.indices.reversed()) {
            val line = lines[i]
            if (matchesKeyword(line, TOTAL_KEYWORDS, fuzzy = true) &&
                !matchesKeyword(line, TOTAL_EXCLUDE_KEYWORDS, fuzzy = true)
            ) {
                val amount = extractLastNumber(line)
                if (amount != null && amount > 0) return amount
            }
        }

        // Statistical fallback: the largest amount in the totals block is usually the total.
        val amounts = lines.mapNotNull { extractLastNumber(it) }.filter { it > 0 }
        return amounts.maxOrNull() ?: 0L
    }

    private fun parseTax(lines: List<String>): Long? {
        for (i in lines.indices.reversed()) {
            val line = lines[i]
            if (matchesKeyword(line, TAX_KEYWORDS, fuzzy = true) &&
                !matchesKeyword(line, TAX_EXCLUDE_KEYWORDS)
            ) {
                val amount = extractLastNumber(line)
                if (amount != null) return amount
            }
        }
        return null
    }

    private fun parseSubtotal(lines: List<String>): Long? {
        for (i in lines.indices.reversed()) {
            val line = lines[i]
            if (matchesKeyword(line, SUBTOTAL_KEYWORDS, fuzzy = true)) {
                val amount = extractLastNumber(line)
                if (amount != null) return amount
            }
        }
        return null
    }

    // ---------------------------------------------------------------- items

    private fun parseItems(lines: List<String>, merchantName: String?): List<ParsedReceiptItem> {
        val items = mutableListOf<ParsedReceiptItem>()

        for (line in lines) {
            if (line == merchantName) continue
            if (line.length < 3) continue
            if (isSeparatorLine(line)) continue
            if (!line.any { it.isDigit() }) continue
            if (matchesKeyword(line, ITEM_SKIP_KEYWORDS)) continue
            if (matchesKeyword(line, PROMOTIONAL_HINTS)) continue

            tryParseItemLine(line)?.let { items.add(it) }
        }

        return items
    }

    private fun tryParseItemLine(line: String): ParsedReceiptItem? {
        // Skip lines that look like dates or times
        if (line.matches(Regex(".*\\b\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}\\b.*")) ||
            line.matches(Regex(".*\\b\\d{4}[/-]\\d{1,2}[/-]\\d{1,2}\\b.*")) ||
            line.matches(Regex(".*\\b\\d{2}:\\d{2}\\b.*"))
        ) {
            return null
        }

        // "8 Juni 2026" is a date, but "1 Croissant 25.000" is an item: only reject the
        // digit-word-digit shape when the middle word is an actual month name.
        val wordDate = Regex("\\b(\\d{1,2})\\s+([a-zA-Z]{3,9})\\s+(\\d{2,4})\\b").find(line)
        if (wordDate != null && MONTH_MAP.containsKey(wordDate.groupValues[2].lowercase())) {
            return null
        }

        // Pattern 1: [Name] [Qty] x/X/@ [UnitPrice] [Subtotal]
        // E.g. "AQUA 2 x 5.000 10.000" or "INDOMIE 3 @ 3,000 9,000"
        val matcher1 = Pattern
            .compile("^(.*?)\\s+(\\d+(?:[.,]\\d+)?)\\s*[@xX]\\s*([\\d.,]+)\\s+([\\d.,]+)\\s*$")
            .matcher(line)
        if (matcher1.find()) {
            val name = cleanItemName(matcher1.group(1))
            val qty = parseQty(matcher1.group(2))
            val price = cleanAndParseAmount(matcher1.group(3)) ?: 0L
            val subtotal = cleanAndParseAmount(matcher1.group(4)) ?: (qty * price).toLong()
            if (name.isNotEmpty() && subtotal >= 100L && isPlausibleQty(qty)) {
                return ParsedReceiptItem(name, qty, price, subtotal)
            }
        }

        // Pattern 1B: [Name] [Qty] x/X/@ [UnitPrice] (without trailing subtotal)
        // E.g. "INDOMIE 2 x 3500" or "TELUR 1 x 25000"
        val matcher1B = Pattern
            .compile("^(.*?)\\s+(\\d+(?:[.,]\\d+)?)\\s*[@xX]\\s*([\\d.,]+)\\s*$")
            .matcher(line)
        if (matcher1B.find()) {
            val name = cleanItemName(matcher1B.group(1))
            val qty = parseQty(matcher1B.group(2))
            val price = cleanAndParseAmount(matcher1B.group(3)) ?: 0L
            val subtotal = (qty * price).toLong()
            if (name.isNotEmpty() && subtotal >= 100L && isPlausibleQty(qty)) {
                return ParsedReceiptItem(name, qty, price, subtotal)
            }
        }

        // Pattern 4: [Qty] [Name] [Subtotal] (restaurant / QRIS format)
        // E.g. "1 NASI GORENG 25.000" or "2 ES TEH 10000"
        val matcher4 = Pattern
            .compile("^(\\d+(?:[.,]\\d+)?)\\s+(.*?)\\s+([\\d.,]+)\\s*$")
            .matcher(line)
        if (matcher4.find()) {
            val qty = parseQty(matcher4.group(1))
            val name = cleanItemName(matcher4.group(2))
            val subtotal = cleanAndParseAmount(matcher4.group(3)) ?: 0L
            val price = if (qty > 0) (subtotal / qty).toLong() else subtotal
            // "600 ML AQUA 5.000" - the leading number is a measurement, not a quantity.
            val leadsWithMeasurement = firstToken(name)?.let { it in MEASUREMENT_TOKENS } ?: false
            if (name.isNotEmpty() && subtotal >= 100L && name.any { it.isLetter() } &&
                isPlausibleQty(qty) && !leadsWithMeasurement
            ) {
                return ParsedReceiptItem(name, qty, price, subtotal)
            }
        }

        // Pattern 2: [Name] [Qty] [UnitPrice] [Subtotal] (without separator)
        // E.g. "AQUA 2 5.000 10.000"
        val matcher2 = Pattern
            .compile("^(.*?)\\s+(\\d+)\\s+([\\d.,]+)\\s+([\\d.,]+)\\s*$")
            .matcher(line)
        if (matcher2.find()) {
            val name = cleanItemName(matcher2.group(1))
            val qty = parseQty(matcher2.group(2))
            val price = cleanAndParseAmount(matcher2.group(3)) ?: 0L
            val subtotal = cleanAndParseAmount(matcher2.group(4)) ?: (qty * price).toLong()
            if (name.isNotEmpty() && subtotal >= 100L && price > 0L && isPlausibleQty(qty)) {
                val diff = Math.abs(subtotal - (qty * price).toLong())
                if (diff < 100 || subtotal == (qty * price).toLong()) {
                    return ParsedReceiptItem(name, qty, price, subtotal)
                }
            }
        }

        // Pattern 2B: [Name] [UnitPrice] [Subtotal] (implicit Qty 1, when UnitPrice == Subtotal)
        // E.g. "ROTI TAWAR SARI    15.000  15.000"
        val matcher2B = Pattern
            .compile("^(.*?)\\s+([\\d.,]+)\\s+([\\d.,]+)\\s*$")
            .matcher(line)
        if (matcher2B.find()) {
            val name = cleanItemName(matcher2B.group(1))
            val price = cleanAndParseAmount(matcher2B.group(2)) ?: 0L
            val subtotal = cleanAndParseAmount(matcher2B.group(3)) ?: 0L
            if (name.isNotEmpty() && price >= 100L && price == subtotal) {
                return ParsedReceiptItem(name, 1.0, price, subtotal)
            }
        }

        // Pattern 3: Simple item line: [Name] [Subtotal]
        // E.g. "TEH KOTAK 6.500" or "KOPI KAPAL API Rp12.000"
        val lastSpaceIndex = line.lastIndexOf(' ')
        if (lastSpaceIndex > 0) {
            val name = cleanItemName(line.substring(0, lastSpaceIndex))
            val lastPart = line.substring(lastSpaceIndex + 1)
            val amount = cleanAndParseAmount(lastPart)
            val looksLikeAddress = firstToken(name)?.let { it in ADDRESS_HINTS } ?: false
            if (name.isNotEmpty() && amount != null && amount >= 100L && !looksLikeAddress) {
                if (name.replace(Regex("[^a-zA-Z]"), "").length >= 2) {
                    return ParsedReceiptItem(name, null, amount, amount)
                }
            }
        }

        return null
    }

    private fun cleanItemName(raw: String?): String =
        raw?.trim()?.trim('-', '*', ':', '.', ',')?.trim() ?: ""

    private fun firstToken(text: String): String? = tokenize(text).firstOrNull()

    private fun parseQty(raw: String?): Double =
        raw?.replace(',', '.')?.toDoubleOrNull() ?: 1.0

    private fun isPlausibleQty(qty: Double): Boolean = qty in MIN_QTY..MAX_QTY

    // ---------------------------------------------------------------- helpers

    private fun tokenize(line: String): List<String> =
        line.lowercase().split(Regex("[^a-z0-9]+")).filter { it.isNotEmpty() }

    /**
     * Digits that OCR commonly substitutes for letters, so "T0TAL" still matches "total".
     * Cheaper and far safer than a pure edit-distance match, which happily confuses
     * "Bakar" with "bayar".
     */
    private fun normalizeOcrDigits(token: String): String = token
        .replace('0', 'o')
        .replace('1', 'l')
        .replace('5', 's')
        .replace('8', 'b')

    /**
     * Token based keyword matching. Unlike a plain `contains`, this never drops an item
     * such as "BISCUIT" because of the "sc" keyword. Multi word keywords are matched
     * against the normalized token stream.
     *
     * @param fuzzy additionally allows a single edit on long tokens. Restricted to
     *              tokens and keywords of at least [FUZZY_MIN_LENGTH] characters: on
     *              shorter words a single edit collides with ordinary item names.
     */
    private fun matchesKeyword(line: String, keywords: List<String>, fuzzy: Boolean = false): Boolean {
        val tokens = tokenize(line)
        if (tokens.isEmpty()) return false
        val normalized = tokens.joinToString(" ")
        val ocrTokens = tokens.map { normalizeOcrDigits(it) }

        for (keyword in keywords) {
            val key = keyword.lowercase().trim()
            if (key.isEmpty()) continue

            if (key.contains(' ')) {
                if (normalized.contains(key)) return true
            } else {
                if (tokens.any { it == key } || ocrTokens.any { it == key }) return true
                if (fuzzy && key.length >= FUZZY_MIN_LENGTH) {
                    val hit = ocrTokens.any {
                        it.length >= FUZZY_MIN_LENGTH && FuzzyMatcher.matchesAny(it, listOf(key), 1)
                    }
                    if (hit) return true
                }
            }
        }
        return false
    }

    private fun extractLastNumber(line: String): Long? {
        val parts = line.split(Regex("\\s+"))
        for (i in parts.indices.reversed()) {
            val amount = cleanAndParseAmount(parts[i])
            if (amount != null && amount > 0) {
                return amount
            }
        }
        return null
    }

    /**
     * Normalizes an Indonesian currency token into a plain Long.
     *
     * Handles the "Rp" prefix, thousand separators, a trailing ",-" / ".-", cents
     * ("125,000.00" and "5.000,00" both mean the same thing here) and negative amounts
     * written either with a leading minus or wrapped in parentheses.
     */
    fun cleanAndParseAmount(str: String?): Long? {
        if (str.isNullOrBlank()) return null

        var cleaned = str.trim()
        val isNegative = cleaned.startsWith("-") || (cleaned.startsWith("(") && cleaned.endsWith(")"))

        cleaned = cleaned.replace(Regex("(?i)rp\\.?"), "")
        cleaned = cleaned.replace(Regex("[\\s()]"), "")
        cleaned = cleaned.replace(Regex("^[-+]+"), "")
        // "37.400,-" and "37.400.-" are written on most Indonesian receipts.
        cleaned = cleaned.replace(Regex("[,.]-+$"), "")
        cleaned = cleaned.replace(Regex("-+$"), "")
        if (cleaned.isEmpty()) return null

        // Drop cents, but only when digits remain in front of them.
        val withoutCents = cleaned.replace(Regex("[,.](\\d{2})$"), "")
        if (withoutCents.any { it.isDigit() }) {
            cleaned = withoutCents
        }

        cleaned = cleaned.replace(Regex("[,.]"), "")

        val value = cleaned.toLongOrNull() ?: return null
        return if (isNegative) -value else value
    }
}
