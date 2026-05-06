package com.firstpenguin.app.domain.diary.service

import com.firstpenguin.app.domain.diary.model.Diary
import org.springframework.stereotype.Service
import java.awt.Color
import java.awt.FontMetrics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.imageio.ImageIO

@Service
class DiaryShareImageService(
    private val fontProvider: DiaryShareImageFontProvider,
) {
    fun generate(diary: Diary): ByteArray {
        val image = BufferedImage(WIDTH, HEIGHT, BufferedImage.TYPE_INT_RGB)
        val graphics = image.createGraphics()

        try {
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
            graphics.drawBackground()
            graphics.drawDate(diary)
            graphics.drawQuote(diary)
            graphics.drawBook(diary)
        } finally {
            graphics.dispose()
        }

        return ByteArrayOutputStream().use { output ->
            ImageIO.write(image, IMAGE_FORMAT, output)
            output.toByteArray()
        }
    }

    private fun Graphics2D.drawBackground() {
        color = BURGUNDY
        fillRect(0, 0, WIDTH, HEADER_HEIGHT)
        color = Color.WHITE
        fillRect(0, HEADER_HEIGHT, WIDTH, BODY_HEIGHT)
        color = OLIVE
        fillRect(0, DIVIDER_Y, WIDTH, DIVIDER_HEIGHT)
        color = SKY_BLUE
        fillRect(0, FOOTER_Y, WIDTH, HEIGHT - FOOTER_Y)
    }

    private fun Graphics2D.drawDate(diary: Diary) {
        color = Color.WHITE
        font = fontProvider.date(DATE_FONT_SIZE)
        drawString(diary.createdAt.toLocalDate().format(DATE_FORMATTER), CONTENT_X, DATE_BASELINE_Y)
    }

    private fun Graphics2D.drawQuote(diary: Diary) {
        color = BURGUNDY
        font = fontProvider.quote(QUOTE_FONT_SIZE)
        drawWrappedText(
            text = diary.quoteContent,
            x = CONTENT_X,
            firstBaselineY = QUOTE_BASELINE_Y,
            maxWidth = QUOTE_MAX_WIDTH,
            lineHeight = QUOTE_LINE_HEIGHT,
            maxLines = QUOTE_MAX_LINES,
        )
    }

    private fun Graphics2D.drawBook(diary: Diary) {
        color = BURGUNDY
        font = fontProvider.book(BOOK_FONT_SIZE)
        drawWrappedText(
            text = "${diary.title}, ${diary.author}",
            x = CONTENT_X,
            firstBaselineY = BOOK_BASELINE_Y,
            maxWidth = BOOK_MAX_WIDTH,
            lineHeight = BOOK_LINE_HEIGHT,
            maxLines = BOOK_MAX_LINES,
        )
    }

    private fun Graphics2D.drawWrappedText(
        text: String,
        x: Int,
        firstBaselineY: Int,
        maxWidth: Int,
        lineHeight: Int,
        maxLines: Int,
    ) {
        val lines = text.toWrappedLines(fontMetrics, maxWidth, maxLines)
        lines.forEachIndexed { index, line ->
            drawString(line, x, firstBaselineY + lineHeight * index)
        }
    }

    private fun String.toWrappedLines(
        metrics: FontMetrics,
        maxWidth: Int,
        maxLines: Int,
    ): List<String> {
        val lines = mutableListOf<String>()
        var currentLine = ""

        for (char in this) {
            val candidate = currentLine + char
            if (currentLine.isNotEmpty() && metrics.stringWidth(candidate) > maxWidth) {
                lines += currentLine.trimEnd()
                currentLine = char.toString()

                if (lines.size == maxLines) {
                    return lines.withEllipsis(metrics, maxWidth, force = true)
                }
                continue
            }

            currentLine = candidate
        }

        if (currentLine.isNotBlank() && lines.size < maxLines) {
            lines += currentLine.trimEnd()
        }

        return lines
    }

    private fun List<String>.withEllipsis(
        metrics: FontMetrics,
        maxWidth: Int,
        force: Boolean,
    ): List<String> {
        if (isNotEmpty() && (force || metrics.stringWidth(last()) > maxWidth)) {
            val shortenedLines = toMutableList()
            var shortened = last()

            while (shortened.isNotEmpty() && metrics.stringWidth("$shortened$ELLIPSIS") > maxWidth) {
                shortened = shortened.dropLast(1)
            }

            shortenedLines[shortenedLines.lastIndex] = "$shortened$ELLIPSIS"
            return shortenedLines
        }

        return this
    }

    private companion object {
        const val WIDTH = 260
        const val HEADER_HEIGHT = 44
        const val BODY_HEIGHT = 186
        const val DIVIDER_HEIGHT = 12
        const val FOOTER_HEIGHT = 52
        const val HEIGHT = HEADER_HEIGHT + BODY_HEIGHT + DIVIDER_HEIGHT + FOOTER_HEIGHT
        const val DIVIDER_Y = HEADER_HEIGHT + BODY_HEIGHT
        const val FOOTER_Y = DIVIDER_Y + DIVIDER_HEIGHT
        const val CONTENT_X = 18
        const val DATE_BASELINE_Y = 26
        const val QUOTE_BASELINE_Y = 81
        const val BOOK_BASELINE_Y = 272
        const val DATE_FONT_SIZE = 13
        const val QUOTE_FONT_SIZE = 15
        const val BOOK_FONT_SIZE = 11
        const val QUOTE_MAX_WIDTH = 223
        const val BOOK_MAX_WIDTH = 223
        const val QUOTE_LINE_HEIGHT = 20
        const val BOOK_LINE_HEIGHT = 15
        const val QUOTE_MAX_LINES = 6
        const val BOOK_MAX_LINES = 2
        const val IMAGE_FORMAT = "png"
        const val ELLIPSIS = "..."

        val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM, dd", Locale.ENGLISH)
        val BURGUNDY: Color = Color(0x6B, 0x1E, 0x3C)
        val OLIVE: Color = Color(0x8E, 0x81, 0x00)
        val SKY_BLUE: Color = Color(0xA4, 0xD4, 0xD6)
    }
}
