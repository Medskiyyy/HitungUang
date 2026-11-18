package com.hitunguang.core.ocr

import android.graphics.Point
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class VisionTextFormatterTest {

    @Test
    fun `reconstructs skewed two column receipt matching Alfamart layout`() {
        // Simulates a -3.5 deg tilted receipt (dy/dx = -0.055): right column sits higher in raw Y.
        fun tiltedBox(text: String, left: Int, baseTop: Int, right: Int, height: Int): LineBox {
            val slope = -0.055f
            val dyLeft = (slope * left).toInt()
            val dyRight = (slope * right).toInt()
            val top = baseTop + ((dyLeft + dyRight) / 2)
            return LineBox(
                text = text,
                left = left,
                top = top,
                right = right,
                bottom = top + height,
                cornerPoints = arrayOf(
                    Point(left, baseTop + dyLeft),
                    Point(right, baseTop + dyRight),
                    Point(right, baseTop + dyRight + height),
                    Point(left, baseTop + dyLeft + height)
                )
            )
        }

        // ML Kit often returns left column text block first, then right column prices block.
        val boxes = listOf(
            tiltedBox("Cimory", 125, 300, 195, 14),
            tiltedBox("1x 2.000", 132, 316, 200, 14),
            tiltedBox("Cimory hazelnut", 125, 333, 250, 14),
            tiltedBox("1x 9.000", 132, 349, 200, 14),
            tiltedBox("Total", 125, 480, 180, 15),
            tiltedBox("Tunai", 125, 498, 180, 15),
            // Right column block out of order with respect to left column
            tiltedBox("2.000", 275, 316, 320, 14),
            tiltedBox("9.000", 275, 349, 320, 14),
            tiltedBox("Rp38.000", 255, 480, 320, 15),
            tiltedBox("Rp38.000", 255, 498, 320, 15)
        )

        val formatted = VisionTextFormatter.formatLines(boxes)
        val expected = """
            Cimory
            1x 2.000 2.000
            Cimory hazelnut
            1x 9.000 9.000
            Total Rp38.000
            Tunai Rp38.000
        """.trimIndent()

        assertEquals(expected, formatted)
    }

    @Test
    fun `merges straight two column rows left to right and returns fallback when empty`() {
        val boxes = listOf(
            LineBox("Subtotal", 20, 100, 120, 120),
            LineBox("Total", 20, 130, 100, 150),
            LineBox("Rp15.000", 240, 100, 320, 120),
            LineBox("Rp15.000", 240, 130, 320, 150)
        )

        val formatted = VisionTextFormatter.formatLines(boxes)
        assertEquals("Subtotal Rp15.000\nTotal Rp15.000", formatted)
        assertEquals("fallback", VisionTextFormatter.formatLines(emptyList(), "fallback"))
    }

    @Test
    fun `groups curved row price sitting 1_2x line height lower along local baseline slope`() {
        fun slopedBox(text: String, left: Int, cy: Int, right: Int, height: Int, slope: Float): LineBox {
            val halfW = (right - left) * 0.5f
            val yLeft = (cy - slope * halfW).toInt()
            val yRight = (cy + slope * halfW).toInt()
            val top = cy - height / 2
            return LineBox(
                text = text,
                left = left,
                top = top,
                right = right,
                bottom = top + height,
                cornerPoints = arrayOf(
                    Point(left, yLeft - height / 2),
                    Point(right, yRight - height / 2),
                    Point(right, yRight + height / 2),
                    Point(left, yLeft + height / 2)
                )
            )
        }

        // Most lines are flat (median slope ~0), while the curved item row slopes downward at +0.08
        // so its right-hand price sits 24px (1.2x line height) lower than the item label.
        val boxes = listOf(
            slopedBox("INDOMARET", 20, 100, 300, 20, 0f),
            slopedBox("AQUA BOTOL 600ML", 20, 200, 220, 20, 0.08f),
            slopedBox("TOTAL", 20, 280, 140, 20, 0f),
            slopedBox("10.000", 380, 224, 460, 20, 0.08f),
            slopedBox("10.000", 380, 280, 460, 20, 0f)
        )

        val formatted = VisionTextFormatter.formatLines(boxes)
        assertEquals("INDOMARET\nAQUA BOTOL 600ML 10.000\nTOTAL 10.000", formatted)
    }
}
