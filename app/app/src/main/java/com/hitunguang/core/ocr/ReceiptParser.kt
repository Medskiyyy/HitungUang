package com.hitunguang.core.ocr

import java.text.Normalizer
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
        "roll", "set", "pasang", "klg", "kig", "kaleng", "pak", "bal", "sak",
        "krg", "karung", "ikat", "lbr", "lembar", "cup", "porsi", "prs",
        "gln", "galon", "rtg", "renteng", "slop", "krt", "karton",
        "bnd", "band", "pls", "fcs", "fc", "pac"
    )

    private val HEADER_KEYWORDS = listOf(
        "nama barang", "deskripsi", "item", "produk", "nama produk",
        "qty", "jumlah", "harga", "subtotal", "price", "amount",
        "rp", "barang", "nama", "unit", "satuan", "kts", "disc", "diskon"
    )

    private val FOOTER_KEYWORDS = listOf(
        "total", "total belanja", "subtotal", "sub total", "ppn", "pajak",
        "pb1", "pb 1", "tax", "diskon", "discount", "tunai", "cash", "kembalian",
        "kembali", "grand total", "bayar", "payment", "tender", "change",
        "kartu", "debit", "kredit", "qris", "card", "service", "charge",
        "biaya", "admin", "delivery", "ongkir", "biaya kirim", "shopee pay",
        "gopay", "ovo", "dana", "linkaja", "ovo points", "promo",
        "voucher", "kupon", "reward", "member", "point", "loyalty",
        "donation", "bulatkan", "pembulatan", "rounded", "saving", "tabungan",
        "refund", "pengembalian dana", "biaya penanganan", "handling fee", "lunas"
    )

    private val PROMOTIONAL_HINTS = listOf(
        "belanja hemat", "setiap hari", "promo", "member", "diskon spesial",
        "terima kasih", "thank you", "thankyou", "terimakasih", "selamat datang",
        "kunjungi", "download", "aplikasi", "website", "fb", "ig", "instagram",
        "facebook", "twitter", "gratis", "free", "undian", "hadiah",
        "matur nuwun", "anda memperoleh", "poin member", "klikindomaret"
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
        "total belanja", "total item", "grand total", "total", "sub total", "subtotal",
        "ppn", "pb1", "pb 1", "pajak", "tax", "tunai", "cash", "kembali", "kembalian",
        "bayar", "service charge", "serv", "biaya admin", "handling fee", "harga jual",
        "jumlah item", "total pembayaran", "jumlah bayar", "total tagihan", "tagihan",
        "total disc", "total discount", "pembulatan", "anda hemat", "non tunai", "cukai"
    )

    private val TOTAL_PRIMARY_KEYWORDS = listOf(
        "total belanja", "grand total", "total bayar", "total pembayaran",
        "jumlah bayar", "total tagihan", "total nett", "total net",
        "total bell", "total beli", "netto", "tagihan", "total"
    )

    private val TOTAL_ITEM_KEYWORDS = listOf("total item")

    private val TOTAL_SECONDARY_KEYWORDS = listOf("harga jual", "jumlah", "net")

    private val TOTAL_KEYWORDS = TOTAL_PRIMARY_KEYWORDS + TOTAL_SECONDARY_KEYWORDS

    private val TOTAL_EXCLUDE_KEYWORDS = listOf(
        "subtotal", "sub total", "pajak", "ppn", "tax", "kembali", "kembalian",
        "cash", "tunai", "change", "item", "items", "qty", "diskon", "discount",
        "disc", "hemat", "potongan", "saving", "poin", "point"
    )

    /** Payment lines: the amount handed over or returned, never the amount spent. */
    private val PAYMENT_KEYWORDS = listOf(
        "tunai", "cash", "bayar", "kembali", "kembalian", "change", "payment", "tender", "jumlah uang"
    )

    /** Header lines describing the transaction rather than a purchase. */
    private val HEADER_INFO_KEYWORDS = listOf(
        "kasir", "cashier", "no", "tgl", "tanggal", "jam", "struk", "nota", "trx",
        "transaksi", "ref", "invoice", "check", "kode", "waktu", "table", "guest", "order"
    )

    private val DATE_LABELS = listOf("tanggal", "tgl", "date", "waktu", "time")

    private val TAX_KEYWORDS = listOf("ppn", "tax", "pajak", "pb1", "pb 1", "vat")

    private val TAX_EXCLUDE_KEYWORDS = listOf(
        "before", "excluding", "tanpa", "dpp", "termasuk", "incl", "included",
        "dibebaskan", "bebas", "nihil", "item ppn"
    )

    private val SUBTOTAL_KEYWORDS = listOf("subtotal", "sub total", "jumlah sebelum")

    private val TENDER_KEYWORDS = listOf("tunai", "cash", "bayar", "payment", "tender", "jumlah uang")

    private val CHANGE_KEYWORDS = listOf("kembali", "kembalian", "change")

    private val HEMAT_KEYWORDS = listOf("anda hemat", "hemat")

    private val DISCOUNT_KEYWORDS = listOf("diskon", "discount", "potongan", "voucher", "vc", "disc")

    private data class KnownBrand(
        val displayName: String,
        val brandTokens: List<String>,
        val extraHints: List<String>
    )

    private val KNOWN_BRANDS = listOf(
        KnownBrand(
            "Indomaret",
            listOf("indomaret"),
            listOf(
                "indomarco", "indoarco", "prismatama", "pris4atama",
                "indomare", "ndomaret", "ndomaet", "ndsmaet", "hdamuet", "indomanet",
                "indomatet", "jndomatet", "jndomaret",
                "klikindomaret", "1500280", "1500 280", "1500580", "1500 580"
            )
        ),
        KnownBrand(
            "Alfamart",
            listOf("alfamart"),
            listOf("alfanart", "alfahart", "sumber alfaria", "alfaria trijaya", "alfagift", "1500959")
        ),
        KnownBrand("Alfamidi", listOf("alfamidi"), listOf("midi utama")),
        KnownBrand("BreadTalk", listOf("breadtalk"), listOf("btdelivery")),
        KnownBrand("Super Indo", listOf("superindo", "super indo"), listOf("lion super")),
        KnownBrand("Hypermart", listOf("hypermart"), listOf("matahari putra prima")),
        KnownBrand("FamilyMart", listOf("familymart", "family mart"), listOf("fajar mitra indah")),
        KnownBrand("Lawson", listOf("lawson"), listOf("lancar wiguna")),
        KnownBrand("Yomart", listOf("yomart"), listOf("griya pratama")),
        KnownBrand("Circle K", listOf("circle k"), emptyList())
    )

    /** Tokens that never belong to a purchasable item line. */
    private val ITEM_SKIP_KEYWORDS = listOf(
        "total", "jumlah", "bayar", "tunai", "cash", "kembali", "kembalian",
        "change", "ppn", "tax", "pajak", "pb1", "pb 1", "debit", "kredit", "card",
        "kartu", "jenis kartu", "nomor kartu", "no kartu", "lard",
        "tanggal", "tgl", "date", "time", "jam", "telp", "tlp",
        "phone", "npwp", "alamat", "subtotal", "sub total", "promo", "diskon",
        "discount", "disc", "voucher", "potongan", "kasir", "cashier", "struk", "nota", "invoice",
        "check no", "no struk", "no nota", "no bon", "no transaksi", "kode trx", "order id",
        "cukai", "dpp", "bkp", "nbkp", "pembulatan", "round", "serv",
        "ovo", "gopay", "shopeepay", "dana", "linkaja", "qris", "bca",
        "mandiri", "bri", "bni", "visa", "mastercard", "service", "charge",
        "sc", "payment", "member", "point", "poin", "telepon", "lunas"
    )

    /** Tokens that mark an address / contact line, used when picking the merchant name. */
    private val ADDRESS_HINTS = listOf(
        "jl", "jln", "jalan", "no", "rt", "rw", "kel", "kec", "blok", "perum",
        "komplek", "kompleks", "telp", "tlp", "telepon", "phone", "npwp",
        "email", "www", "kab", "kecamatan", "kelurahan", "kodepos",
        "banjar", "kota", "provinsi", "desa", "tbk", "persero"
    )

    /** Address/legal tokens unambiguous anywhere in a line (never a product name). */
    private val FULL_ADDRESS_TOKENS = listOf(
        "jl", "jln", "jalan", "kel", "kec", "kab", "kecamatan", "kelurahan",
        "kodepos", "komplek", "kompleks", "telp", "tlp", "telepon", "phone",
        "npwp", "email", "www", "banjar", "kota", "provinsi", "desa"
    )

    private val SEPARATOR_CHARS = setOf('-', '=', '*', '.', '_', '~', '#', '+')

    /** Quantities outside this range are OCR noise, not a real purchase count. */
    private const val MIN_QTY = 0.01
    private const val MAX_QTY = 999.0
    private const val MAX_VERIFIED_QTY = 100_000.0

    /** Below this length a single edit distance produces false positives. */
    private const val FUZZY_MIN_LENGTH = 7

    private const val OCR_GROUPED_AMOUNT =
        "[0-9OoIl|DdBb]{1,3}(?:[.,][0-9OoIl|DdBb]{3})+(?:[.,]\\d{1,2})?"

    private const val AMOUNT_TOKEN =
        "(?:(?i)rp\\.?\\s*)?(?:\\d[\\d.,]*|$OCR_GROUPED_AMOUNT)(?:,-|\\.-|-\\.)?"

    /** "25 000" printed with a space as thousands separator. */
    private val SPACED_THOUSANDS = Regex("(?<![\\d.,])\\d{1,3}(?: \\d{3})+(?![\\d.,])")

    /** "18, 000" or "50. 000. 000" where OCR inserts whitespace after the thousands separator. */
    private val SPACED_AMOUNT_SEP = Regex("(\\d)([.,])\\s+(?=\\d{3}(?!\\d))")

    private val THOUSANDS_GROUP_QTY = Regex("^\\d{1,3}(?:\\.\\d{3})+$")

    private val BARCODE_PREFIX_BEFORE_QTY = Regex(
        "^\\d{5,14}\\s+(?=\\d+(?:[.,]\\d+)?(?:\\s+[a-zA-Z]{1,10})?\\s*[@xX])"
    )

    /** Second line of a two-line item: "2 x 3.500 7.000", "4.000 Kg X 12.500 Rp. 50.000.000", "1 x 5.500 = 5.500", "2 PCS 14.500 0 29.000", "1x 2.000", or "2 3.500 7.000". */
    private val QTY_PRICE_LINE = Regex(
        "^\\d+(?:[.,]\\d+)?(?:\\s+([a-zA-Z]{1,10}))?\\s*[@xX]\\s*$AMOUNT_TOKEN(?:(?:\\s*=\\s*|\\s+)$AMOUNT_TOKEN)?\\s*$|^\\d+(?:[.,]\\d+)?\\s+$AMOUNT_TOKEN\\s+$AMOUNT_TOKEN\\s*$|^\\d+(?:[.,]\\d+)?\\s+([a-zA-Z]{1,10})\\s+$AMOUNT_TOKEN(?:\\s+0)?\\s+$AMOUNT_TOKEN\\s*$"
    )

    private val PHONE_TOKEN = Regex("^\\+?\\d[\\d-]*$")
    private val TIME_TOKEN = Regex("\\b\\d{1,2}:\\d{2}\\b")
    private val DATE_TIME_HYPHEN = Regex("(\\d{2,4})\\s*-\\s*(\\d{1,2}:\\d{2})")
    private val SPACED_DATE_SEP = Regex("\\b(\\d{1,2})\\s*([./-])\\s*(\\d{1,2})\\s*\\2\\s*(\\d{2,4})\\b")
    private val SLASH_DATE_TOKEN = Regex("\\b\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}\\b")
    private val ISO_DATE_TOKEN = Regex("\\b\\d{4}[/-]\\d{1,2}[/-]\\d{1,2}\\b")
    private val DOT_DATE_4Y_TOKEN = Regex("\\b\\d{1,2}\\.\\d{1,2}\\.20\\d{2}\\b")
    private val ISO_DOT_DATE_TOKEN = Regex("\\b20\\d{2}\\.\\d{1,2}\\.\\d{1,2}\\b")
    private val DOT_DATE_2Y_TOKEN = Regex("\\b\\d{1,2}\\.\\d{1,2}\\.\\d{2}\\b")

    fun parse(rawText: String?): ParsedReceipt {
        if (rawText.isNullOrBlank()) {
            return ParsedReceipt(null, null, null, null, 0L, emptyList())
        }

        val lines = rawText.lines()
            .map { line ->
                line.trim()
                    .replace(SPACED_THOUSANDS) { m -> m.value.replace(" ", "") }
                    .replace(SPACED_AMOUNT_SEP, "$1$2")
            }
            .filter { it.isNotEmpty() }

        if (lines.isEmpty()) {
            return ParsedReceipt(null, null, null, null, 0L, emptyList())
        }

        val footerStart = findFooterStart(lines)
        val delimitedBodyStart = findDelimitedBodyStart(lines, footerStart)
        val implicitBodyStart = if (delimitedBodyStart == null) findImplicitBodyStart(lines, footerStart) else null
        val bodyStart = delimitedBodyStart ?: implicitBodyStart ?: if (footerStart > 1) 1 else 0

        val headerLines = lines.subList(0, bodyStart.coerceIn(0, lines.size))
        val bodyLines = if (bodyStart < footerStart) lines.subList(bodyStart, footerStart) else emptyList()
        // When no totals block is detected, fall back to scanning the whole receipt.
        val footerLines = if (footerStart < lines.size) lines.subList(footerStart, lines.size) else lines

        // Without a header the first line is only a merchant candidate when it is not an item.
        val headerMerchant = parseMerchantName(
            if (headerLines.isEmpty()) lines.take(1).filter { tryParseItemLine(it) == null } else headerLines
        )
        val merchantName = resolveMerchantName(headerMerchant, lines)
        val date = parseDate(lines)
        val tax = parseTax(footerLines)
        val printedSubtotal = parseSubtotal(footerLines)
        val items = parseItems(
            lines = bodyLines,
            merchantName = merchantName,
            leadingName = if (delimitedBodyStart == null && implicitBodyStart == null) headerLines.lastOrNull() else null,
            skipHeaderInfo = delimitedBodyStart == null
        )
        val itemsSum = items.sumOf { it.subtotal }
        val total = reconcileTotal(
            parsedTotal = parseTotal(footerLines),
            printedSubtotal = printedSubtotal,
            tax = tax,
            itemsSum = itemsSum,
            allLines = bodyLines + footerLines,
            footerLines = footerLines
        )
            ?: printedSubtotal?.let { it + (tax ?: 0L) }
            ?: itemsSum.takeIf { it > 0L }
            ?: parseLargestAmount(footerLines)
        val subtotal = printedSubtotal ?: (total - (tax ?: 0L)).coerceAtLeast(0L)

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
     * receipt has no recognizable totals. Skips date/time stamps and trailing column headers
     * such as "11-05-19 (08:35:01) 712 04 TOTAL".
     */
    private fun findFooterStart(lines: List<String>): Int {
        for (i in lines.indices) {
            val line = lines[i]
            if (isSeparatorLine(line) || isDateOrTimeLine(line)) continue
            if (matchesKeyword(line, TOTAL_BLOCK_KEYWORDS, fuzzy = true)) {
                val amount = extractAmountOnOrNextLine(lines, i, allowZero = true) ?: continue
                val endsAsHeaderCol = normalizeLabelStream(line).endsWith("total") &&
                    amount < 100L && !line.contains(':') &&
                    lines.getOrNull(i + 1)?.let { isStandaloneAmountLine(it) } != true
                if (!endsAsHeaderCol) {
                    return i
                }
            }
        }
        return lines.size
    }

    /**
     * Index of the first line that may contain items: right after the first separator
     * or column header that still leaves item-like content before the totals block.
     * When multiple header separators enclose transaction metadata (e.g. Check No / date)
     * with no purchasable item between them, advance past the last header delimiter.
     * Returns null when the receipt has no usable delimiter.
     */
    private fun findDelimitedBodyStart(lines: List<String>, footerStart: Int): Int? {
        var bodyStart: Int? = null
        var i = 0
        while (i < footerStart) {
            val isHeaderRow = !matchesKeyword(lines[i], SUBTOTAL_KEYWORDS) &&
                countKeywordHits(lines[i], HEADER_KEYWORDS) >= 2 &&
                extractLastNumber(lines[i]) == null
            val isDelimiter = isSeparatorLine(lines[i]) || isHeaderRow
            if (isDelimiter && hasItemLikeContent(lines, i + 1, footerStart)) {
                if (bodyStart == null) {
                    bodyStart = i + 1
                } else if (!containsRealItem(lines, bodyStart, i)) {
                    bodyStart = i + 1
                } else {
                    break
                }
            }
            i++
        }
        return bodyStart
    }

    /**
     * On receipts without a dashed delimiter, skip leading store/address/NPWP/date/table lines
     * while stopping before the first real purchase item. Keeps single-item headerless snippets safe.
     */
    private fun findImplicitBodyStart(lines: List<String>, footerStart: Int): Int? {
        if (footerStart <= 1) return null
        var idx = 0
        while (idx < footerStart) {
            val line = lines[idx]
            val norm = normalizeOcrQtyLine(line)
            val isExplicitMeta = isSeparatorLine(line) ||
                isDateOrTimeLine(line) ||
                matchesKeyword(line, FULL_ADDRESS_TOKENS) ||
                matchesKeyword(line, HEADER_INFO_KEYWORDS) ||
                matchesKeyword(line, PROMOTIONAL_HINTS) ||
                firstToken(line)?.let { it in ADDRESS_HINTS } == true ||
                line.split(Regex("\\s+")).any { isPhoneNumber(it) } ||
                line.count { it.isLetter() } < 2 ||
                (line.replace(Regex("[^0-9]"), "").length > line.length * 0.5)

            if (isExplicitMeta) {
                idx++
                continue
            }

            if (idx == 0) {
                val nextNorm = lines.getOrNull(1)?.let { normalizeOcrQtyLine(it) }
                if (tryParseItemLine(norm) != null || (nextNorm != null && isQtyPriceLine(nextNorm))) {
                    return null
                }
                idx++
                continue
            }

            if (tryParseItemLine(norm) != null) break
            val nextNorm = lines.getOrNull(idx + 1)?.let { normalizeOcrQtyLine(it) }
            if (nextNorm != null && isQtyPriceLine(nextNorm)) break

            // Keep skipping secondary branch/tagline header lines when an anchor (date/address/NPWP) appears shortly below.
            val hasHeaderAnchorAhead = ((idx + 1) until minOf(footerStart, idx + 8)).any { k ->
                val ahead = lines[k]
                isDateOrTimeLine(ahead) ||
                    matchesKeyword(ahead, FULL_ADDRESS_TOKENS) ||
                    matchesKeyword(ahead, HEADER_INFO_KEYWORDS) ||
                    ahead.split(Regex("\\s+")).any { isPhoneNumber(it) }
            }
            if (hasHeaderAnchorAhead) {
                idx++
            } else {
                break
            }
        }
        return idx.takeIf { it > 0 && it < footerStart }
    }

    private fun containsRealItem(lines: List<String>, from: Int, until: Int): Boolean {
        for (k in from until until) {
            val norm = normalizeOcrQtyLine(lines[k])
            if (norm.length < 2 ||
                isSeparatorLine(norm) ||
                isDateOrTimeLine(norm) ||
                matchesKeyword(norm, HEADER_INFO_KEYWORDS) ||
                matchesKeyword(norm, ITEM_SKIP_KEYWORDS) ||
                matchesKeyword(norm, PROMOTIONAL_HINTS) ||
                matchesKeyword(norm, ADDRESS_HINTS)
            ) {
                continue
            }
            if (isQtyPriceLine(norm) || tryParseItemLine(norm) != null) {
                return true
            }
        }
        return false
    }

    private fun hasItemLikeContent(lines: List<String>, from: Int, until: Int): Boolean =
        (from until until).any { !isSeparatorLine(lines[it]) && lines[it].any { c -> c.isDigit() } }

    private fun isSeparatorLine(line: String): Boolean {
        val compact = line.filterNot { it.isWhitespace() }
        if (compact.length < 3) return false
        return compact.all { it in SEPARATOR_CHARS }
    }

    // ---------------------------------------------------------------- merchant

    private fun cleanMerchantText(raw: String): String =
        raw.replace("®", "").replace("™", "").trim()

    private fun detectKnownBrand(lines: List<String>): KnownBrand? {
        val fullLower = lines.joinToString(" ").lowercase()
        for (brand in KNOWN_BRANDS) {
            if (brand.brandTokens.any { fullLower.contains(it) } ||
                brand.extraHints.any { fullLower.contains(it) }
            ) {
                return brand
            }
        }
        return null
    }

    private fun resolveMerchantName(headerMerchant: String?, lines: List<String>): String? {
        val cleanedHeader = headerMerchant?.let { cleanMerchantText(it) }?.takeIf { it.isNotEmpty() }
        val brand = detectKnownBrand(lines) ?: return cleanedHeader
        if (cleanedHeader == null) return brand.displayName
        val headerLower = cleanedHeader.lowercase()
        return if (brand.brandTokens.any { headerLower.contains(it) }) cleanedHeader else brand.displayName
    }

    private fun parseMerchantName(headerLines: List<String>): String? {
        for (line in headerLines) {
            val candidate = cleanMerchantText(line)
            if (candidate.length < 3) continue
            if (isSeparatorLine(candidate)) continue
            if (candidate.matches(Regex(".*\\b\\d{4}\\b.*"))) continue
            if (candidate.replace(Regex("[^0-9]"), "").length > candidate.length * 0.5) continue
            if (matchesKeyword(candidate, ADDRESS_HINTS)) continue
            if (matchesKeyword(candidate, PROMOTIONAL_HINTS)) continue
            if (candidate.count { it.isLetter() } < 3) continue

            return candidate
        }
        return null
    }

    // ---------------------------------------------------------------- date

    private fun normalizeDateText(line: String): String = line
        .lowercase()
        .replace(DATE_TIME_HYPHEN, "$1 $2")
        .replace(SPACED_DATE_SEP, "$1$2$3$2$4")

    /**
     * Collects every date-like candidate and prefers one sitting on a line that is
     * explicitly labelled as a date. Candidates are validated strictly, so an OCR
     * misread such as 31/02/2026 is rejected instead of silently rolling over.
     */
    private fun parseDate(lines: List<String>): Long? {
        var firstValid: Long? = null

        for (line in lines) {
            val candidate = extractDateFromLine(line) ?: continue
            if (matchesKeyword(line, DATE_LABELS)) {
                return candidate
            }
            if (firstValid == null) {
                firstValid = candidate
            }
        }
        return firstValid
    }

    private fun hasTimeOrDateHint(line: String): Boolean =
        TIME_TOKEN.containsMatchIn(line) || matchesKeyword(line, DATE_LABELS)

    private fun extractDateFromLine(line: String): Long? {
        val normalized = normalizeDateText(line)

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

        val dotFourYearMatcher = Pattern
            .compile("\\b(\\d{1,2})\\.(\\d{1,2})\\.(20\\d{2})\\b")
            .matcher(normalized)
        while (dotFourYearMatcher.find()) {
            val day = dotFourYearMatcher.group(1)?.toIntOrNull()
            val month = dotFourYearMatcher.group(2)?.toIntOrNull()
            val year = dotFourYearMatcher.group(3)?.toIntOrNull()
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

        val isoDotMatcher = Pattern
            .compile("\\b(20\\d{2})\\.(\\d{1,2})\\.(\\d{1,2})\\b")
            .matcher(normalized)
        while (isoDotMatcher.find()) {
            val year = isoDotMatcher.group(1)?.toIntOrNull()
            val month = isoDotMatcher.group(2)?.toIntOrNull()
            val day = isoDotMatcher.group(3)?.toIntOrNull()
            if (day != null && month != null && year != null) {
                toTimestamp(year, month - 1, day)?.let { return it }
            }
        }

        if (hasTimeOrDateHint(normalized)) {
            val dotTwoYearMatcher = Pattern
                .compile("\\b(\\d{1,2})\\.(\\d{1,2})\\.(\\d{2})\\b")
                .matcher(normalized)
            while (dotTwoYearMatcher.find()) {
                val day = dotTwoYearMatcher.group(1)?.toIntOrNull()
                val month = dotTwoYearMatcher.group(2)?.toIntOrNull()
                val year = normalizeYear(dotTwoYearMatcher.group(3)?.toIntOrNull())
                if (day != null && month != null && year != null) {
                    toTimestamp(year, month - 1, day)?.let { return it }
                }
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

    private fun reconcileTotal(
        parsedTotal: Long?,
        printedSubtotal: Long?,
        tax: Long?,
        itemsSum: Long,
        allLines: List<String>,
        footerLines: List<String>
    ): Long? {
        val tender = parseLatestMatchingAmount(footerLines, TENDER_KEYWORDS, CHANGE_KEYWORDS, allowZero = false)
        val change = parseLatestMatchingAmount(footerLines, CHANGE_KEYWORDS, emptyList(), allowZero = true)
        val cashNet = if (tender != null && change != null && tender > change) tender - change else null
        val savings = parseSavingsAmount(allLines, footerLines)
        val grossBase = printedSubtotal ?: itemsSum.takeIf { it > 0L }
        val discountedGross = if (grossBase != null && savings > 0L && grossBase > savings) grossBase - savings else null

        if (parsedTotal == null) {
            if (cashNet != null) {
                val maxPlausible = if (itemsSum > 0L) itemsSum + (tax ?: 0L) + 100L else Long.MAX_VALUE
                if ((discountedGross != null && cashNet == discountedGross) ||
                    (printedSubtotal != null && cashNet == printedSubtotal + (tax ?: 0L)) ||
                    (itemsSum > 0L && cashNet <= maxPlausible)
                ) {
                    return cashNet
                }
            }
            if (discountedGross != null) return discountedGross
            return null
        }

        // Case 0: OCR dropped a trailing zero (e.g. "47.00" parsed as 47 instead of 47.000) or read ruler edge (< 1000).
        if (parsedTotal in 1L..999L) {
            val scaled = parsedTotal * 1000L
            if ((printedSubtotal != null && scaled == printedSubtotal) ||
                scaled == itemsSum ||
                (cashNet != null && scaled == cashNet)
            ) {
                return scaled
            }
            if (cashNet != null && cashNet >= 1000L && (itemsSum == 0L || cashNet <= itemsSum + (tax ?: 0L) + 100L)) {
                return cashNet
            }
        }

        // Case 1: Printed total exists on paper, but leading digits washed out or misread (e.g. "TOTAL : ,800" vs 12,800 or "B7,100" vs 67,100).
        val upperBound = maxOf(itemsSum, printedSubtotal ?: 0L) + (tax ?: 0L) + 100L
        val cashNearDiscounted = cashNet != null && discountedGross != null && kotlin.math.abs(cashNet - discountedGross) <= 100L
        val cashNetConsistent = cashNet != null && cashNet in 100L..upperBound &&
            (cashNearDiscounted || matchesTrailingDigits(cashNet, parsedTotal))
        if (itemsSum > 0L && (parsedTotal < itemsSum / 2 || (cashNetConsistent && parsedTotal != cashNet && parsedTotal > upperBound))) {
            val recovery = when {
                cashNearDiscounted && cashNet!! in 1L..upperBound -> cashNet
                cashNetConsistent && parsedTotal > upperBound -> cashNet
                cashNet != null && cashNet > parsedTotal && cashNet <= itemsSum + (tax ?: 0L) &&
                    matchesTrailingDigits(cashNet, parsedTotal) -> cashNet
                discountedGross != null && discountedGross > parsedTotal && discountedGross <= itemsSum + (tax ?: 0L) &&
                    matchesTrailingDigits(discountedGross, parsedTotal) -> discountedGross
                else -> null
            }
            if (recovery != null) return recovery
        }

        // Case 2: Total line completely unreadable so parseTotal fell back onto gross "HARGA JUAL" while net is lower.
        if (cashNet != null && discountedGross != null && cashNet == discountedGross &&
            (parsedTotal == itemsSum || parsedTotal == grossBase) && cashNet < parsedTotal
        ) {
            return cashNet
        }

        return parsedTotal
    }

    private fun parseLatestMatchingAmount(
        lines: List<String>,
        keywords: List<String>,
        excludeKeywords: List<String>,
        allowZero: Boolean
    ): Long? {
        for (i in lines.indices.reversed()) {
            val line = lines[i]
            if (matchesKeyword(line, keywords, fuzzy = true) &&
                (excludeKeywords.isEmpty() || !matchesKeyword(line, excludeKeywords, fuzzy = true))
            ) {
                val amount = extractAmountOnOrNextLine(lines, i, allowZero = allowZero)
                if (amount != null && (if (allowZero) amount >= 0L else amount > 0L)) {
                    return amount
                }
            }
        }
        return null
    }

    private fun parseSavingsAmount(allLines: List<String>, footerLines: List<String>): Long {
        var explicitHemat = 0L
        for (line in footerLines) {
            if (matchesKeyword(line, HEMAT_KEYWORDS)) {
                val amt = extractLastNumber(line)?.let { kotlin.math.abs(it) } ?: 0L
                if (amt >= 100L) {
                    explicitHemat = amt
                }
            }
        }
        var discountSum = 0L
        for (line in allLines) {
            if (matchesKeyword(line, TOTAL_KEYWORDS) ||
                matchesKeyword(line, HEMAT_KEYWORDS) ||
                matchesKeyword(line, TAX_KEYWORDS) ||
                matchesKeyword(line, listOf("pembulatan", "bulatkan", "ppn"))
            ) continue
            val isDiscountLine = matchesKeyword(line, DISCOUNT_KEYWORDS) ||
                Regex("\\(\\s*(?:(?i)rp\\.?\\s*)?\\d[\\d.,]*\\s*\\)").containsMatchIn(line)
            if (isDiscountLine) {
                val rawAmt = line.split(Regex("\\s+")).reversed().firstNotNullOfOrNull { cleanAndParseAmount(it) }
                val absAmt = rawAmt?.let { kotlin.math.abs(it) } ?: 0L
                if (absAmt >= 100L) {
                    discountSum += absAmt
                }
            }
        }
        return maxOf(explicitHemat, discountSum)
    }

    private fun matchesTrailingDigits(fullAmount: Long, truncatedAmount: Long): Boolean {
        val fullStr = fullAmount.toString()
        val truncStr = truncatedAmount.toString()
        return fullStr.endsWith(truncStr) || (fullAmount >= 1000L && truncatedAmount >= 100L && fullAmount % 1000L == truncatedAmount % 1000L)
    }

    private fun parseTotal(lines: List<String>): Long? {
        for (i in lines.indices.reversed()) {
            val line = lines[i]
            if (matchesKeyword(line, TOTAL_PRIMARY_KEYWORDS, fuzzy = true) &&
                !matchesKeyword(line, TOTAL_EXCLUDE_KEYWORDS, fuzzy = true)
            ) {
                val amount = extractAmountOnOrNextLine(lines, i, allowZero = false)
                if (amount != null && amount > 0) return amount
            }
        }
        for (i in lines.indices.reversed()) {
            val line = lines[i]
            if (matchesKeyword(line, TOTAL_ITEM_KEYWORDS, fuzzy = true)) {
                val amount = extractAmountOnOrNextLine(lines, i, allowZero = false)
                if (amount != null && amount >= 100L) return amount
            }
        }
        for (i in lines.indices.reversed()) {
            val line = lines[i]
            if (matchesKeyword(line, TOTAL_SECONDARY_KEYWORDS, fuzzy = true) &&
                !matchesKeyword(line, TOTAL_EXCLUDE_KEYWORDS, fuzzy = true)
            ) {
                val amount = extractAmountOnOrNextLine(lines, i, allowZero = false)
                if (amount != null && amount > 0) return amount
            }
        }
        return null
    }

    /**
     * Statistical fallback: the largest amount is usually the total, except on payment
     * lines, where the cash handed over is larger than what was spent.
     */
    private fun parseLargestAmount(lines: List<String>): Long {
        val amounts = lines
            .filterNot { matchesKeyword(it, PAYMENT_KEYWORDS) }
            .mapNotNull { extractLastNumber(it) }
            .filter { it > 0 }
        return amounts.maxOrNull() ?: 0L
    }

    private fun parseTax(lines: List<String>): Long? {
        for (i in lines.indices.reversed()) {
            val line = lines[i]
            if (Regex("(?i)(?:ppn|pajak|tax).*\\(\\s*[\\d.,]+\\s*\\)").containsMatchIn(line)) continue
            if (matchesKeyword(line, TAX_KEYWORDS, fuzzy = true) &&
                !matchesKeyword(line, TAX_EXCLUDE_KEYWORDS)
            ) {
                val amount = extractAmountOnOrNextLine(lines, i, allowZero = true)
                if (amount != null) return amount
            }
        }
        return null
    }

    private fun parseSubtotal(lines: List<String>): Long? {
        for (i in lines.indices.reversed()) {
            val line = lines[i]
            if (matchesKeyword(line, SUBTOTAL_KEYWORDS, fuzzy = true)) {
                val amount = extractAmountOnOrNextLine(lines, i, allowZero = true)
                if (amount != null) return amount
            }
        }
        return null
    }

    private fun extractAmountOnOrNextLine(
        lines: List<String>,
        index: Int,
        allowZero: Boolean
    ): Long? {
        val onLine = extractLastNumber(lines[index], allowZero = allowZero)
        if (onLine != null) return onLine
        val next = lines.getOrNull(index + 1) ?: return null
        if (!isStandaloneAmountLine(next)) return null
        val amount = cleanAndParseAmount(next) ?: return null
        return if (allowZero) amount.takeIf { it >= 0L } else amount.takeIf { it > 0L }
    }

    private fun isStandaloneAmountLine(line: String): Boolean {
        val trimmed = line.trim()
        if (trimmed.isEmpty() || isPhoneNumber(trimmed)) return false
        val stripped = trimmed.replace(Regex("(?i)^rp\\.?\\s*"), "").trim()
        if (stripped.any { it.isLetter() }) return false
        val amount = cleanAndParseAmount(trimmed)
        return amount != null && amount >= 0L
    }

    // ---------------------------------------------------------------- items

    /**
     * @param leadingName the line just above the body; it can hold the name of an item
     *                    whose quantity and price are printed on the first body line.
     * @param skipHeaderInfo true when no delimiter separated the header from the body,
     *                       so cashier / transaction-number lines may still be present.
     */
    private fun parseItems(
        lines: List<String>,
        merchantName: String?,
        leadingName: String?,
        skipHeaderInfo: Boolean
    ): List<ParsedReceiptItem> {
        val items = mutableListOf<ParsedReceiptItem>()
        var pendingName = leadingName?.takeIf { isCandidateItemName(it) }

        for (idx in lines.indices) {
            val line = lines[idx]
            val normLine = normalizeOcrQtyLine(line)
            if (normLine.length < 2 ||
                isSeparatorLine(normLine) ||
                matchesKeyword(normLine, ITEM_SKIP_KEYWORDS) ||
                matchesKeyword(normLine, PROMOTIONAL_HINTS) ||
                matchesKeyword(normLine, FULL_ADDRESS_TOKENS) ||
                ((skipHeaderInfo || normLine.contains(':') || normLine.contains('#')) &&
                    matchesKeyword(normLine, HEADER_INFO_KEYWORDS))
            ) {
                pendingName = null
                continue
            }

            // Two-line item: either name above "2 x 3.500 7.000", or "[Name] [Subtotal]" above "1x 2.000".
            val name = pendingName
            pendingName = null
            if (isQtyPriceLine(normLine)) {
                if (name != null) {
                    tryParseItemLine("$name $normLine")?.let { items.add(it) }
                } else {
                    val prev = items.lastOrNull()
                    if (prev != null && prev.quantity == null) {
                        val parsedQty = tryParseItemLine("ITEM $normLine")
                        if (parsedQty != null) {
                            items[items.lastIndex] = prev.copy(
                                quantity = parsedQty.quantity,
                                unitPrice = parsedQty.unitPrice,
                                subtotal = if (prev.subtotal > 0L) prev.subtotal else parsedQty.subtotal
                            )
                        }
                    }
                }
                continue
            }

            if (line == merchantName || normLine == merchantName) continue

            // Hold wrapped prefix if next line is an explicit qty/price line whose amounts don't match trailing integer (e.g. "HYDRO COCO 330" / "ZEN SHISO 706").
            val nextNorm = lines.getOrNull(idx + 1)?.let { normalizeOcrQtyLine(it) }
            if (nextNorm != null && isQtyPriceLine(nextNorm)) {
                val trailingInt = extractLastNumber(normLine, allowZero = false)
                val nextPrices = extractAmountsFromQtyLine(nextNorm)
                if (trailingInt == null || nextPrices.isEmpty() || trailingInt !in nextPrices) {
                    if (isCandidateItemName(normLine)) {
                        pendingName = normLine
                        continue
                    }
                }
            }

            // Wrapped multi-line item continuation starting lowercase ("ew 58g 1 12.500") or leading measurement ("68g 2 10.000").
            if (name != null && isWrappedContinuationLine(normLine)) {
                val joined = tryParseItemLine("$name $normLine")
                if (joined != null) {
                    items.add(joined)
                    continue
                }
            }

            val parsed = tryParseItemLine(normLine)
            if (parsed != null) {
                items.add(parsed)
            } else if (isCandidateItemName(normLine)) {
                pendingName = if (name != null) "$name $normLine" else normLine
            }
        }

        return items
    }

    private fun extractAmountsFromQtyLine(line: String): List<Long> =
        line.split(Regex("\\s+")).mapNotNull { cleanAndParseAmount(it.trimStart('=')) }.filter { it >= 100L }

    private fun isWrappedContinuationLine(line: String): Boolean {
        if (line.firstOrNull()?.isLowerCase() == true) return true
        return Regex("^\\d+(?:[.,]\\d+)?\\s*(?:g|gr|gram|kg|ml|m\\||l|lt|pcs|pc|pack)(?![a-zA-Z])", RegexOption.IGNORE_CASE).containsMatchIn(line)
    }

    private fun isQtyPriceLine(line: String): Boolean {
        val match = QTY_PRICE_LINE.find(line) ?: return false
        val unit1 = match.groupValues.getOrNull(1).orEmpty()
        val unit2 = match.groupValues.getOrNull(2).orEmpty()
        val unit = unit1.ifEmpty { unit2 }
        return unit.isEmpty() || unit.lowercase() in MEASUREMENT_TOKENS
    }

    private fun normalizeOcrQtyLine(line: String): String = line
        .replace(BARCODE_PREFIX_BEFORE_QTY, "")
        .replace(Regex("(\\d\\s*[mM])\\|(?=\\s|$)"), "$1l")
        .replace(Regex("^([Il|])\\s*([xX@])(?=\\s|\\d)"), "1$2")
        .replace(Regex("(\\s)([Il|])\\s*([xX@])\\s+(?=(?:(?i)rp\\.?\\s*)?\\d)"), "$11$3 ")

    private fun isCandidateItemName(line: String): Boolean {
        if (line.length < 2 || isSeparatorLine(line)) return false
        if (line.count { it.isLetter() } < 2) return false
        if (line.split(Regex("\\s+")).any { isPhoneNumber(it) }) return false
        if (firstToken(line)?.let { it in ADDRESS_HINTS } == true) return false
        if (matchesKeyword(line, FULL_ADDRESS_TOKENS)) return false
        if (isDateOrTimeLine(line)) return false
        if (matchesKeyword(line, HEADER_INFO_KEYWORDS)) return false
        if (matchesKeyword(line, ITEM_SKIP_KEYWORDS)) return false
        if (matchesKeyword(line, PROMOTIONAL_HINTS)) return false
        return true
    }

    private fun isDateOrTimeLine(line: String): Boolean {
        val normalized = normalizeDateText(line)
        if (SLASH_DATE_TOKEN.containsMatchIn(normalized) ||
            ISO_DATE_TOKEN.containsMatchIn(normalized) ||
            TIME_TOKEN.containsMatchIn(normalized) ||
            DOT_DATE_4Y_TOKEN.containsMatchIn(normalized) ||
            ISO_DOT_DATE_TOKEN.containsMatchIn(normalized) ||
            (hasTimeOrDateHint(normalized) && DOT_DATE_2Y_TOKEN.containsMatchIn(normalized))
        ) {
            return true
        }
        // "8 Juni 2026" is a date, but "1 Croissant 25.000" is an item: only reject the
        // digit-word-digit shape when the middle word is an actual month name.
        val wordDate = Regex("\\b(\\d{1,2})\\s+([a-zA-Z]{3,9})\\s+(\\d{2,4})\\b").find(line)
        return wordDate != null && MONTH_MAP.containsKey(wordDate.groupValues[2].lowercase())
    }

    private fun tryParseItemLine(rawLine: String): ParsedReceiptItem? {
        val line = normalizeOcrQtyLine(rawLine)
        if (line.split(Regex("\\s+")).any { isPhoneNumber(it) }) return null
        if (matchesKeyword(line, FULL_ADDRESS_TOKENS)) return null
        if (isDateOrTimeLine(line)) return null

        // Pattern 1: [Name] [Qty] [OptionalUnit] x/X/@ [UnitPrice] [=/space] [Subtotal]
        // E.g. "AQUA 2 x 5.000 10.000", "INDOMIE 3 @ 3,000 9,000", "Beras 4.000 Kg X 12.500 Rp. 50.000.000", or "NISSIN 1 x 5.500 = 5.500"
        val matcher1 = Pattern
            .compile("^(.*?)\\s+(\\d+(?:[.,]\\d+)?)(?:\\s+([a-zA-Z]{1,10}))?\\s*[@xX]\\s*($AMOUNT_TOKEN)(?:\\s*=\\s*|\\s+)($AMOUNT_TOKEN)\\s*$")
            .matcher(line)
        if (matcher1.find()) {
            val unitToken = matcher1.group(3)
            if (unitToken == null || unitToken.lowercase() in MEASUREMENT_TOKENS) {
                val name = cleanItemName(matcher1.group(1))
                val price = cleanAndParseAmount(matcher1.group(4)) ?: 0L
                val rawSubtotal = cleanAndParseAmount(matcher1.group(5)) ?: 0L
                val qty = resolveQty(matcher1.group(2), price, rawSubtotal)
                val recovered = recoverTruncatedSubtotal(qty, price, rawSubtotal)
                val finalSubtotal = if (recovered > 0L) recovered else (qty * price).toLong()
                if (name.isNotEmpty() && finalSubtotal >= 100L && isPlausibleQty(qty, price, finalSubtotal)) {
                    return ParsedReceiptItem(name, qty, price, finalSubtotal)
                }
            }
        }

        // Pattern 1C: [Name] [Qty] [Unit] [UnitPrice] [0?] [Subtotal] (Dewi Kosmetik column table)
        val matcherFullUnitSpace = Pattern
            .compile("^(.*?)\\s+(\\d+(?:[.,]\\d+)?)\\s+([a-zA-Z]{1,10})\\s+($AMOUNT_TOKEN)(?:\\s+0)?\\s+($AMOUNT_TOKEN)\\s*$")
            .matcher(line)
        if (matcherFullUnitSpace.find()) {
            val unitToken = matcherFullUnitSpace.group(3)
            if (unitToken != null && unitToken.lowercase() in MEASUREMENT_TOKENS) {
                val name = cleanItemName(matcherFullUnitSpace.group(1))
                val price = cleanAndParseAmount(matcherFullUnitSpace.group(4)) ?: 0L
                val rawSubtotal = cleanAndParseAmount(matcherFullUnitSpace.group(5)) ?: 0L
                val qty = resolveQty(matcherFullUnitSpace.group(2), price, rawSubtotal)
                val subtotal = recoverTruncatedSubtotal(qty, price, rawSubtotal)
                if (name.isNotEmpty() && subtotal >= 100L && isPlausibleQty(qty, price, subtotal)) {
                    return ParsedReceiptItem(name, qty, price, subtotal)
                }
            }
        }

        // Pattern 1B: [Name] [Qty] [OptionalUnit] x/X/@ [UnitPrice] (without trailing subtotal)
        // E.g. "INDOMIE 2 x 3500" or "TELUR 1 x 25000"
        val matcher1B = Pattern
            .compile("^(.*?)\\s+(\\d+(?:[.,]\\d+)?)(?:\\s+([a-zA-Z]{1,10}))?\\s*[@xX]\\s*($AMOUNT_TOKEN)\\s*$")
            .matcher(line)
        if (matcher1B.find()) {
            val unitToken = matcher1B.group(3)
            if (unitToken == null || unitToken.lowercase() in MEASUREMENT_TOKENS) {
                val name = cleanItemName(matcher1B.group(1))
                val qty = parseQty(matcher1B.group(2))
                val price = cleanAndParseAmount(matcher1B.group(4)) ?: 0L
                val subtotal = (qty * price).toLong()
                if (name.isNotEmpty() && subtotal >= 100L && isPlausibleQty(qty)) {
                    return ParsedReceiptItem(name, qty, price, subtotal)
                }
            }
        }

        // Pattern 4: [Qty] [Name] [Subtotal] (restaurant / QRIS format)
        // E.g. "1 NASI GORENG 25.000" or "2 ES TEH 10000"
        val matcher4 = Pattern
            .compile("^(\\d+(?:[.,]\\d+)?)\\s+(.*?)\\s+($AMOUNT_TOKEN)\\s*$")
            .matcher(line)
        if (matcher4.find()) {
            val qty = parseQty(matcher4.group(1))
            val name = cleanItemName(matcher4.group(2))
            val subtotal = cleanAndParseAmount(matcher4.group(3)) ?: 0L
            val price = if (qty > 0) (subtotal / qty).toLong() else subtotal
            // "600 ML AQUA 5.000" - the leading number is a measurement, not a quantity.
            val leadsWithMeasurement = firstToken(name)?.let { it in MEASUREMENT_TOKENS } ?: false
            val isQtyMultiplierToken = name.matches(Regex("(?i)^[x@pcs]+$"))
            if (name.isNotEmpty() && subtotal >= 100L && name.count { it.isLetter() } >= 2 &&
                !isQtyMultiplierToken && isPlausibleQty(qty) && !leadsWithMeasurement
            ) {
                return ParsedReceiptItem(name, qty, price, subtotal)
            }
        }

        // Pattern 2: [Name] [Qty] [UnitPrice] [Subtotal] (without separator), optionally followed by a margin/ruler noise digit < 100.
        // E.g. "AQUA 2 5.000 10.000" or "ORONAMIN C DRINK 120 1 7500 7,500 40"
        val lineWithoutTrailingNoise = line.replace(Regex("\\s+\\d{1,2}$"), "")
        val pattern2Candidates = if (lineWithoutTrailingNoise != line) listOf(line, lineWithoutTrailingNoise) else listOf(line)
        for (candidateLine in pattern2Candidates) {
            val matcher2 = Pattern
                .compile("^(.*?)\\s+(\\d+)\\s+($AMOUNT_TOKEN)\\s+($AMOUNT_TOKEN)\\s*$")
                .matcher(candidateLine)
            if (matcher2.find()) {
                val name = cleanItemName(matcher2.group(1))
                val qty = parseQty(matcher2.group(2))
                val price = cleanAndParseAmount(matcher2.group(3)) ?: 0L
                val rawSubtotal = cleanAndParseAmount(matcher2.group(4)) ?: (qty * price).toLong()
                val subtotal = recoverTruncatedSubtotal(qty, price, rawSubtotal)
                if (name.isNotEmpty() && subtotal >= 100L && price > 0L && isPlausibleQty(qty)) {
                    val diff = Math.abs(subtotal - (qty * price).toLong())
                    if (diff < 100 || subtotal == (qty * price).toLong()) {
                        return ParsedReceiptItem(name, qty, price, subtotal)
                    }
                }
            }
        }

        // Pattern 2B: [Name] [UnitPrice] [Subtotal] (implicit Qty 1, when UnitPrice == Subtotal)
        // E.g. "ROTI TAWAR SARI    15.000  15.000"
        val matcher2B = Pattern
            .compile("^(.*?)\\s+($AMOUNT_TOKEN)\\s+($AMOUNT_TOKEN)\\s*$")
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
        // E.g. "TEH KOTAK 6.500", "KOPI KAPAL API Rp12.000", or "KOPI Rp 12.000"
        val matcher3 = Pattern
            .compile("^(.*?)\\s+($AMOUNT_TOKEN)\\s*$")
            .matcher(line)
        if (matcher3.find()) {
            val name = cleanItemName(matcher3.group(1))
            val amount = cleanAndParseAmount(matcher3.group(2))
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

    private fun resolveQty(rawQty: String?, unitPrice: Long, subtotal: Long?): Double {
        val decQty = parseQty(rawQty)
        if (rawQty != null && unitPrice > 0L && subtotal != null && subtotal > 0L &&
            THOUSANDS_GROUP_QTY.matches(rawQty)
        ) {
            val thouQty = rawQty.replace(".", "").toDoubleOrNull()
            if (thouQty != null) {
                val thouErr = kotlin.math.abs(subtotal - (thouQty * unitPrice).toLong())
                val decErr = kotlin.math.abs(subtotal - (decQty * unitPrice).toLong())
                if (thouErr < decErr) {
                    return thouQty
                }
            }
        }
        return decQty
    }

    private fun recoverTruncatedSubtotal(qty: Double, unitPrice: Long, parsedSubtotal: Long): Long {
        if (parsedSubtotal !in 1L..99L || unitPrice < 100L || qty <= 0.0) return parsedSubtotal
        val expected = (qty * unitPrice).toLong()
        if (expected >= 1000L &&
            expected.toString().startsWith(parsedSubtotal.toString()) &&
            kotlin.math.abs(expected - parsedSubtotal * 1000L) < 1000L
        ) {
            return expected
        }
        return parsedSubtotal
    }

    private fun isPlausibleQty(qty: Double, unitPrice: Long = 0L, subtotal: Long = 0L): Boolean {
        if (qty in MIN_QTY..MAX_QTY) return true
        if (qty in MIN_QTY..MAX_VERIFIED_QTY && unitPrice > 0L && subtotal > 0L) {
            val tolerance = maxOf(100L, subtotal / 100L)
            return kotlin.math.abs(subtotal - (qty * unitPrice).toLong()) <= tolerance
        }
        return false
    }

    // ---------------------------------------------------------------- helpers

    private fun normalizeLabelStream(line: String): String {
        val folded = Normalizer.normalize(line, Normalizer.Form.NFD)
            .replace(Regex("\\p{M}+"), "")
            .lowercase()
        return folded
            .replace(Regex("\\b(?:to|tu)\\s*tal\\b|\\b10tal\\b|\\blutal\\b|\\bluial\\b"), "total")
            .replace(Regex("\\bbel\\s*anja\\b|\\b[bh]?elanja\\b"), "belanja")
            .replace(Regex("\\btun(?:at|a!|gi|al)\\b"), "tunai")
            .replace(Regex("\\b(?:kemb\\s*[au]l?\\s*i|ker\\s*bal\\s*i|ken\\s*bal\\s*i|ren\\s*bal\\s*i|kembalk|kembalt|kembaul)\\b"), "kembali")
            .replace(Regex("\\b(?:kembal\\s*ian|kerbal\\s*ian|kenbal\\s*ian)\\b"), "kembalian")
            .replace(Regex("\\bo?tal\\s+(?:it[eo][mnr]|l\\s*tem)\\b|\\btotal\\s+(?:l\\s*tem|it[eo][nr]|itern)\\b"), "total item")
    }

    private fun tokenize(line: String): List<String> =
        normalizeLabelStream(line).split(Regex("[^a-z0-9]+")).filter { it.isNotEmpty() }

    private fun countKeywordHits(line: String, keywords: List<String>): Int {
        val tokens = tokenize(line)
        if (tokens.isEmpty()) return 0
        val normalized = tokens.joinToString(" ")
        return keywords.count { kw ->
            val k = kw.lowercase().trim()
            if (k.contains(' ')) normalized.contains(k) else tokens.any { it == k }
        }
    }

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

    /** "081234567890", "+6281234567890" or "0812-3456-7890": a phone number, never an amount. */
    private fun isPhoneNumber(token: String): Boolean {
        val trimmed = token.trim(':', ',', '(', ')')
        if (!PHONE_TOKEN.matches(trimmed)) return false
        val digits = trimmed.filter { it.isDigit() }
        return digits.length >= 10 || trimmed.startsWith("08") || trimmed.startsWith("+62")
    }

    private fun extractLastNumber(line: String, allowZero: Boolean = false): Long? {
        val parts = line.split(Regex("\\s+"))
        for (i in parts.indices.reversed()) {
            if (isPhoneNumber(parts[i])) continue
            val amount = cleanAndParseAmount(parts[i])
            if (amount != null && (if (allowZero) amount >= 0L else amount > 0L)) {
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
        // "37.400,-", "37.400.-", and "35,000-." appear on Indonesian receipts.
        cleaned = cleaned.replace(Regex("(?:[,.]-+|-[,.]+|-+)$"), "")
        if (cleaned.isEmpty()) return null

        // Repair clear thermal OCR glyph swaps only inside separated numeric tokens (e.g. "I1.000", "16.6DD", "B,500").
        val isGroupedShape = Regex("^[0-9OoIl|DdBb]{1,3}(?:[.,][0-9OoIl|DdBb]{3})+(?:[.,]\\d{1,2})?$").matches(cleaned)
        if ((isGroupedShape || Regex("^[0-9OoIl|Dd.,]+$").matches(cleaned)) &&
            cleaned.any { it.isDigit() } &&
            (cleaned.contains('.') || cleaned.contains(','))
        ) {
            cleaned = cleaned
                .replace('O', '0').replace('o', '0')
                .replace('D', '0').replace('d', '0')
                .replace('I', '1').replace('l', '1').replace('|', '1')
                .let { if (isGroupedShape) it.replace('B', '8') else it }
        }

        // Drop cents ("10.000,5" / "5.000,00"), but only when digits remain in front of
        // them. A three digit group is a thousands group, not cents.
        val withoutCents = cleaned.replace(Regex("[,.]\\d{1,2}$"), "")
        if (withoutCents.any { it.isDigit() }) {
            cleaned = withoutCents
        }

        cleaned = cleaned.replace(Regex("[,.]"), "")

        val value = cleaned.toLongOrNull() ?: return null
        return if (isNegative) -value else value
    }
}
