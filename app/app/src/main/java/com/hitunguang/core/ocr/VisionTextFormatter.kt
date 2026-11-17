package com.hitunguang.core.ocr

import android.graphics.Point
import com.google.mlkit.vision.text.Text
import kotlin.math.abs

data class LineBox(
    val text: String,
    val left: Int,
    val top: Int,
    val right: Int,
    val bottom: Int,
    val cornerPoints: Array<Point>? = null
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is LineBox) return false
        return text == other.text &&
            left == other.left &&
            top == other.top &&
            right == other.right &&
            bottom == other.bottom &&
            cornerPoints.contentEquals(other.cornerPoints)
    }

    override fun hashCode(): Int {
        var result = text.hashCode()
        result = 31 * result + left
        result = 31 * result + top
        result = 31 * result + right
        result = 31 * result + bottom
        result = 31 * result + (cornerPoints?.contentHashCode() ?: 0)
        return result
    }
}

/**
 * Reconstructs row order from ML Kit bounding boxes. ML Kit's default [Text.getText]
 * groups blocks vertically by column, detaching right-aligned prices from left-aligned
 * item names on receipts. Deskewing by the median line slope keeps tilted photos aligned.
 */
object VisionTextFormatter {

    private data class DeskewedLine(
        val box: LineBox,
        val yCorr: Float,
        val width: Float
    )

    fun format(visionText: Text): String {
        val boxes = visionText.textBlocks
            .flatMap { it.lines }
            .mapNotNull { line ->
                val rect = line.boundingBox ?: return@mapNotNull null
                if (line.text.isBlank()) return@mapNotNull null
                LineBox(
                    text = line.text,
                    left = rect.left,
                    top = rect.top,
                    right = rect.right,
                    bottom = rect.bottom,
                    cornerPoints = line.cornerPoints
                )
            }
        if (boxes.isEmpty()) return visionText.text
        return formatLines(boxes, visionText.text)
    }

    fun formatLines(lines: List<LineBox>, fallbackText: String = ""): String {
        val valid = lines.filter { it.text.isNotBlank() }
        if (valid.isEmpty()) return fallbackText

        val slopes = valid.mapNotNull { box ->
            val pts = box.cornerPoints
            if (pts == null || pts.size < 2) return@mapNotNull null
            val dx = (pts[1].x - pts[0].x).toFloat()
            if (dx < MIN_SLOPE_SPAN_PX) return@mapNotNull null
            val slope = (pts[1].y - pts[0].y).toFloat() / dx
            if (abs(slope) <= MAX_ABS_SLOPE) slope else null
        }.sorted()
        val slope = if (slopes.isEmpty()) 0f else median(slopes)

        val heights = valid.map { (it.bottom - it.top).coerceAtLeast(1).toFloat() }.sorted()
        val medianH = median(heights).coerceAtLeast(1f)
        val yThreshold = ROW_Y_TOLERANCE * medianH

        val ordered = valid.map { box ->
            val cx = (box.left + box.right) * 0.5f
            val cy = (box.top + box.bottom) * 0.5f
            DeskewedLine(
                box = box,
                yCorr = cy - slope * cx,
                width = (box.right - box.left).coerceAtLeast(1).toFloat()
            )
        }.sortedWith(compareBy<DeskewedLine>({ it.yCorr }, { it.box.left }))

        val rows = mutableListOf<MutableList<DeskewedLine>>()
        var currentRow = mutableListOf<DeskewedLine>()
        var rowYCorrSum = 0f

        for (cand in ordered) {
            if (currentRow.isEmpty()) {
                currentRow.add(cand)
                rowYCorrSum = cand.yCorr
                continue
            }
            val meanYCorr = rowYCorrSum / currentRow.size
            val yClose = abs(cand.yCorr - meanYCorr) <= yThreshold
            val noHorizOverlap = currentRow.all { existing ->
                val overlap = minOf(cand.box.right, existing.box.right) - maxOf(cand.box.left, existing.box.left)
                overlap <= MAX_HORIZ_OVERLAP * minOf(cand.width, existing.width)
            }
            if (yClose && noHorizOverlap) {
                currentRow.add(cand)
                rowYCorrSum += cand.yCorr
            } else {
                rows.add(currentRow)
                currentRow = mutableListOf(cand)
                rowYCorrSum = cand.yCorr
            }
        }
        if (currentRow.isNotEmpty()) {
            rows.add(currentRow)
        }

        return rows.joinToString("\n") { row ->
            row.sortedBy { it.box.left }.joinToString(" ") { it.box.text.trim() }
        }
    }

    private fun median(sorted: List<Float>): Float {
        val mid = sorted.size / 2
        return if (sorted.size % 2 == 1) sorted[mid] else (sorted[mid - 1] + sorted[mid]) * 0.5f
    }

    private const val MIN_SLOPE_SPAN_PX = 40f
    private const val MAX_ABS_SLOPE = 0.35f
    private const val ROW_Y_TOLERANCE = 0.55f
    private const val MAX_HORIZ_OVERLAP = 0.25f
}
