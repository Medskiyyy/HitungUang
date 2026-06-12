package com.hitunguang.core.ocr

import java.text.SimpleDateFormat
import java.util.*
import java.util.regex.Pattern

object ReceiptParser {

    private val DATE_FORMATS = listOf(
        "dd/MM/yyyy",
        "dd-MM-yyyy",
        "dd/MM/yy",
        "dd-MM-yy",
        "yyyy-MM-dd",
        "yyyy/MM/dd"
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

    fun parse(rawText: String?): ParsedReceipt {
        if (rawText.isNullOrBlank()) {
            return ParsedReceipt(null, null, null, null, 0L, emptyList())
        }

        val lines = rawText.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val merchantName = parseMerchantName(lines)
        val date = parseDate(rawText)
        val tax = parseTax(lines)
        val total = parseTotal(lines)
        val subtotal = parseSubtotal(lines) ?: (total - (tax ?: 0L)).coerceAtLeast(0L)
        val items = parseItems(lines, merchantName)

        return ParsedReceipt(
            merchantName = merchantName,
            date = date,
            subtotal = subtotal,
            tax = tax,
            total = total,
            items = items
        )
    }

    private fun parseMerchantName(lines: List<String>): String? {
        for (line in lines) {
            if (line.length < 3) continue
            if (line.matches(Regex(".*\\b\\d{4}\\b.*"))) continue
            if (line.replace(Regex("[^0-9]"), "").length > line.length * 0.5) continue
            if (line.all { it == '-' || it == '=' || it == '*' || it == '.' || it == '_' }) continue
            
            return line.trim()
        }
        return null
    }

    private fun parseDate(rawText: String): Long? {
        val normalized = rawText.lowercase()

        val numericPattern = Pattern.compile("\\b(\\d{1,2})[/-](\\d{1,2})[/-](\\d{2,4})\\b")
        val numericMatcher = numericPattern.matcher(normalized)
        if (numericMatcher.find()) {
            val day = numericMatcher.group(1)?.toIntOrNull() ?: 0
            val month = numericMatcher.group(2)?.toIntOrNull() ?: 0
            val yearStr = numericMatcher.group(3) ?: ""
            var year = yearStr.toIntOrNull() ?: 0
            if (year < 100) {
                year += 2000
            }
            if (day in 1..31 && month in 1..12) {
                val cal = Calendar.getInstance()
                cal.set(year, month - 1, day, 12, 0, 0)
                cal.set(Calendar.MILLISECOND, 0)
                return cal.timeInMillis
            }
        }

        val wordMonthPattern = Pattern.compile("\\b(\\d{1,2})\\s+([a-z]{3,9})\\s+(\\d{2,4})\\b")
        val wordMatcher = wordMonthPattern.matcher(normalized)
        if (wordMatcher.find()) {
            val day = wordMatcher.group(1)?.toIntOrNull() ?: 0
            val monthName = wordMatcher.group(2) ?: ""
            val yearStr = wordMatcher.group(3) ?: ""
            var year = yearStr.toIntOrNull() ?: 0
            if (year < 100) {
                year += 2000
            }
            val monthIndex = MONTH_MAP[monthName]
            if (day in 1..31 && monthIndex != null) {
                val cal = Calendar.getInstance()
                cal.set(year, monthIndex, day, 12, 0, 0)
                cal.set(Calendar.MILLISECOND, 0)
                return cal.timeInMillis
            }
        }

        for (fmt in DATE_FORMATS) {
            val sdf = SimpleDateFormat(fmt, Locale.US)
            sdf.isLenient = false
            val pattern = Pattern.compile("\\b\\d{1,4}[/-]\\d{1,2}[/-]\\d{1,4}\\b")
            val matcher = pattern.matcher(rawText)
            while (matcher.find()) {
                try {
                    val parsed = sdf.parse(matcher.group())
                    if (parsed != null) {
                        return parsed.time
                    }
                } catch (e: Exception) {
                    // Ignore
                }
            }
        }

        return null
    }

    private fun parseTotal(lines: List<String>): Long {
        val totalKeywords = listOf("total", "grand total", "jumlah", "total bayar", "total belanja", "netto", "net", "bayar", "total:")
        val excludeKeywords = listOf("subtotal", "sub total", "pajak", "ppn", "tax", "kembali", "kembalian", "cash", "tunai", "change")

        var foundValue: Long? = null

        for (i in lines.indices.reversed()) {
            val line = lines[i].lowercase()
            if (totalKeywords.any { line.contains(it) } && excludeKeywords.none { line.contains(it) }) {
                val amount = extractLastNumber(lines[i])
                if (amount != null && amount > 0) {
                    foundValue = amount
                    break
                }
            }
        }

        if (foundValue == null) {
            for (i in lines.indices.reversed()) {
                val line = lines[i].lowercase()
                if (line.contains("total") || line.contains("jumlah")) {
                    val amount = extractLastNumber(lines[i])
                    if (amount != null && amount > 0) {
                        foundValue = amount
                        break
                    }
                }
            }
        }

        return foundValue ?: 0L
    }

    private fun parseTax(lines: List<String>): Long? {
        val taxKeywords = listOf("ppn", "tax", "pajak", "pb1", "vat")
        val excludeKeywords = listOf("before", "excluding", "tanpa")

        for (i in lines.indices.reversed()) {
            val line = lines[i].lowercase()
            if (taxKeywords.any { line.contains(it) } && excludeKeywords.none { line.contains(it) }) {
                val amount = extractLastNumber(lines[i])
                if (amount != null) {
                    return amount
                }
            }
        }
        return null
    }

    private fun parseSubtotal(lines: List<String>): Long? {
        val subtotalKeywords = listOf("subtotal", "sub total", "jumlah sebelum")

        for (i in lines.indices.reversed()) {
            val line = lines[i].lowercase()
            if (subtotalKeywords.any { line.contains(it) }) {
                val amount = extractLastNumber(lines[i])
                if (amount != null) {
                    return amount
                }
            }
        }
        return null
    }

    private fun parseItems(lines: List<String>, merchantName: String?): List<ParsedReceiptItem> {
        val items = mutableListOf<ParsedReceiptItem>()
        val skipKeywords = listOf(
            "total", "jumlah", "bayar", "tunai", "cash", "kembali", "change",
            "ppn", "tax", "pajak", "debit", "kredit", "card", "tanggal", "date",
            "time", "jam", "telp", "npwp", "alamat", "subtotal", "sub total",
            "promo", "diskon", "discount", "kembalian", "kembali", "ovo", "gopay",
            "shopeepay", "dana", "bca", "mandiri", "bri", "bni", "visa", "mastercard",
            "pb1", "service", "charge", "sc", "tlp", "phone", "tgl", "payment"
        )

        for (i in lines.indices) {
            val line = lines[i]
            val lowerLine = line.lowercase()
            
            if (line == merchantName) continue
            if (line.length < 3) continue
            if (line.all { it == '-' || it == '=' || it == '*' || it == '.' || it == '_' }) continue
            if (skipKeywords.any { lowerLine.contains(it) }) continue
            if (!line.any { it.isDigit() }) continue

            val parsedItem = tryParseItemLine(line)
            if (parsedItem != null) {
                items.add(parsedItem)
            }
        }

        return items
    }

    private fun tryParseItemLine(line: String): ParsedReceiptItem? {
        // Skip lines that look like dates or times
        if (line.matches(Regex(".*\\b\\d{1,2}[/-]\\d{1,2}[/-]\\d{2,4}\\b.*")) ||
            line.matches(Regex(".*\\b\\d{4}[/-]\\d{1,2}[/-]\\d{1,2}\\b.*")) ||
            line.matches(Regex(".*\\b\\d{1,2}\\s+[a-zA-Z]{3,9}\\s+\\d{2,4}\\b.*", RegexOption.IGNORE_CASE)) ||
            line.matches(Regex(".*\\b\\d{2}:\\d{2}\\b.*"))
        ) {
            return null
        }

        // Pattern 1: [Name] [Qty] x/X/@ [UnitPrice] [Subtotal]
        // E.g. "AQUA 2 x 5.000 10.000" or "INDOMIE 3 @ 3,000 9,000"
        val pattern1 = Pattern.compile("^(.*?)\\s+(\\d+(?:[.,]\\d+)?)\\s*[@xX]\\s*([\\d.,]+)\\s+([\\d.,]+)\\s*$")
        val matcher1 = pattern1.matcher(line)
        if (matcher1.find()) {
            val name = matcher1.group(1)?.trim() ?: ""
            val qty = matcher1.group(2)?.replace(',', '.')?.toDoubleOrNull() ?: 1.0
            val price = cleanAndParseAmount(matcher1.group(3)) ?: 0L
            val subtotal = cleanAndParseAmount(matcher1.group(4)) ?: (qty * price).toLong()
            if (name.isNotEmpty() && subtotal >= 100L) {
                return ParsedReceiptItem(name, qty, price, subtotal)
            }
        }

        // Pattern 1B: [Name] [Qty] x/X/@ [UnitPrice] (without trailing subtotal)
        // E.g. "INDOMIE 2 x 3500" or "TELUR 1 x 25000"
        val pattern1B = Pattern.compile("^(.*?)\\s+(\\d+(?:[.,]\\d+)?)\\s*[@xX]\\s*([\\d.,]+)\\s*$")
        val matcher1B = pattern1B.matcher(line)
        if (matcher1B.find()) {
            val name = matcher1B.group(1)?.trim() ?: ""
            val qty = matcher1B.group(2)?.replace(',', '.')?.toDoubleOrNull() ?: 1.0
            val price = cleanAndParseAmount(matcher1B.group(3)) ?: 0L
            val subtotal = (qty * price).toLong()
            if (name.isNotEmpty() && subtotal >= 100L) {
                return ParsedReceiptItem(name, qty, price, subtotal)
            }
        }

        // Pattern 4: [Qty] [Name] [Subtotal] (restaurant / QRIS format)
        // E.g. "1 NASI GORENG 25.000" or "2 ES TEH 10000"
        val pattern4 = Pattern.compile("^(\\d+(?:[.,]\\d+)?)\\s+(.*?)\\s+([\\d.,]+)\\s*$")
        val matcher4 = pattern4.matcher(line)
        if (matcher4.find()) {
            val qty = matcher4.group(1)?.replace(',', '.')?.toDoubleOrNull() ?: 1.0
            val name = matcher4.group(2)?.trim() ?: ""
            val subtotal = cleanAndParseAmount(matcher4.group(3)) ?: 0L
            val price = if (qty > 0) (subtotal / qty).toLong() else subtotal
            if (name.isNotEmpty() && subtotal >= 100L && name.any { it.isLetter() }) {
                return ParsedReceiptItem(name, qty, price, subtotal)
            }
        }

        // Pattern 2: [Name] [Qty] [UnitPrice] [Subtotal] (without separator)
        // E.g. "AQUA 2 5.000 10.000"
        val pattern2 = Pattern.compile("^(.*?)\\s+(\\d+)\\s+([\\d.,]+)\\s+([\\d.,]+)\\s*$")
        val matcher2 = pattern2.matcher(line)
        if (matcher2.find()) {
            val name = matcher2.group(1)?.trim() ?: ""
            val qty = matcher2.group(2)?.toDoubleOrNull() ?: 1.0
            val price = cleanAndParseAmount(matcher2.group(3)) ?: 0L
            val subtotal = cleanAndParseAmount(matcher2.group(4)) ?: (qty * price).toLong()
            if (name.isNotEmpty() && subtotal >= 100L && price > 0L) {
                val diff = Math.abs(subtotal - (qty * price).toLong())
                if (diff < 100 || subtotal == (qty * price).toLong()) {
                    return ParsedReceiptItem(name, qty, price, subtotal)
                }
            }
        }

        // Pattern 2B: [Name] [UnitPrice] [Subtotal] (implicit Qty 1, when UnitPrice == Subtotal)
        // E.g. "ROTI TAWAR SARI    15.000  15.000"
        val pattern2B = Pattern.compile("^(.*?)\\s+([\\d.,]+)\\s+([\\d.,]+)\\s*$")
        val matcher2B = pattern2B.matcher(line)
        if (matcher2B.find()) {
            val name = matcher2B.group(1)?.trim() ?: ""
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
            val name = line.substring(0, lastSpaceIndex).trim()
            val lastPart = line.substring(lastSpaceIndex + 1)
            val amount = cleanAndParseAmount(lastPart)
            if (name.isNotEmpty() && amount != null && amount >= 100L) {
                if (name.replace(Regex("[^a-zA-Z]"), "").length >= 2) {
                    return ParsedReceiptItem(name, null, amount, amount)
                }
            }
        }

        return null
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

    fun cleanAndParseAmount(str: String?): Long? {
        if (str.isNullOrBlank()) return null
        
        var cleaned = str.replace(Regex("(?i)rp|rp\\.?|\\s+"), "")
        cleaned = cleaned.replace(Regex("[,.]00$"), "")
        cleaned = cleaned.replace(Regex("[,.]"), "")

        return cleaned.toLongOrNull()
    }
}
