package com.hitunguang.feature.receipt

import com.hitunguang.core.ocr.ReceiptParser
import com.hitunguang.feature.receipt.domain.usecase.ParseReceiptUseCase
import org.junit.Assert.*
import org.junit.Test
import java.util.*

class ReceiptParserTest {

    private val parseReceiptUseCase = ParseReceiptUseCase()

    @Test
    fun `parse empty raw text returns empty receipt`() {
        val result = parseReceiptUseCase("")
        assertNull(result.merchantName)
        assertNull(result.date)
        assertEquals(0L, result.total)
        assertTrue(result.items.isEmpty())
    }

    @Test
    fun `parse typical Indomaret style receipt`() {
        val rawText = """
            INDOMARET COKROAMINOTO
            JL. COKROAMINOTO NO 12
            ==============================
            12/05/2026 14:30:15
            NPWP: 01.234.567.8-999.000
            ------------------------------
            AQUA AIR MINERAL 2 x 5.000  10.000
            MIE INSTAN GORENG 3 @ 3,000  9.000
            ROTI TAWAR SARI    15.000  15.000
            ------------------------------
            SUBTOTAL:                  34.000
            PPN 11%:                    3.400
            TOTAL BELANJA:             37.400
            CASH/TUNAI:                50.000
            KEMBALIAN:                 12.600
            ==============================
            TERIMA KASIH
        """.trimIndent()

        val result = parseReceiptUseCase(rawText)

        assertEquals("INDOMARET COKROAMINOTO", result.merchantName)
        assertEquals(37400L, result.total)
        assertEquals(34000L, result.subtotal)
        assertEquals(34000L, result.subtotal)
        assertEquals(3400L, result.tax)

        // Date check: 12/05/2026 (May 12, 2026)
        val cal = Calendar.getInstance()
        cal.timeInMillis = result.date ?: 0L
        assertEquals(2026, cal.get(Calendar.YEAR))
        assertEquals(Calendar.MAY, cal.get(Calendar.MONTH))
        assertEquals(12, cal.get(Calendar.DAY_OF_MONTH))

        // Items check
        assertEquals(3, result.items.size)
        
        val item1 = result.items[0]
        assertEquals("AQUA AIR MINERAL", item1.name)
        assertEquals(2.0, item1.quantity ?: 1.0, 0.0)
        assertEquals(5000L, item1.unitPrice)
        assertEquals(10000L, item1.subtotal)

        val item2 = result.items[1]
        assertEquals("MIE INSTAN GORENG", item2.name)
        assertEquals(3.0, item2.quantity ?: 1.0, 0.0)
        assertEquals(3000L, item2.unitPrice)
        assertEquals(9000L, item2.subtotal)

        val item3 = result.items[2]
        assertEquals("ROTI TAWAR SARI", item3.name)
        assertEquals(1.0, item3.quantity ?: 0.0, 0.0)
        assertEquals(15000L, item3.unitPrice)
        assertEquals(15000L, item3.subtotal)
    }

    @Test
    fun `parse restaurant receipt with service tax`() {
        val rawText = """
            WARUNG SUNDA ENTAH
            Tlp: 021-999999
            
            8 Juni 2026
            -------------------------
            Nasi Timbel    2 @ 25,000   50.000
            Ayam Bakar     2 @ 20.000   40.000
            Es Teh Manis   2 @  5.000   10.000
            -------------------------
            Sub Total:                 100.000
            PB1 (10%):                  10.000
            Service Charge:              5.000
            Grand Total:               115.000
            -------------------------
            Thank You
        """.trimIndent()

        val result = parseReceiptUseCase(rawText)

        assertEquals("WARUNG SUNDA ENTAH", result.merchantName)
        assertEquals(115000L, result.total)
        assertEquals(100000L, result.subtotal)
        assertEquals(10000L, result.tax) // PB1 matches tax

        // Date check: 8 Juni 2026
        val cal = Calendar.getInstance()
        cal.timeInMillis = result.date ?: 0L
        assertEquals(2026, cal.get(Calendar.YEAR))
        assertEquals(Calendar.JUNE, cal.get(Calendar.MONTH))
        assertEquals(8, cal.get(Calendar.DAY_OF_MONTH))

        // Items check
        assertEquals(3, result.items.size)
        assertEquals("Nasi Timbel", result.items[0].name)
        assertEquals(2.0, result.items[0].quantity ?: 1.0, 0.0)
        assertEquals(25000L, result.items[0].unitPrice)
        assertEquals(50000L, result.items[0].subtotal)
    }

    @Test
    fun `parse clean amount helper handles different separators`() {
        assertEquals(15000L, ReceiptParser.cleanAndParseAmount("Rp 15.000"))
        assertEquals(125000L, ReceiptParser.cleanAndParseAmount("125,000.00"))
        assertEquals(5000L, ReceiptParser.cleanAndParseAmount("5.000,00"))
        assertEquals(0L, ReceiptParser.cleanAndParseAmount("0"))
        assertNull(ReceiptParser.cleanAndParseAmount(""))
    }
}
