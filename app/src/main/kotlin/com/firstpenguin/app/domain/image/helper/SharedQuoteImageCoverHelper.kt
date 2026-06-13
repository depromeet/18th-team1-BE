package com.firstpenguin.app.domain.image.helper

import java.awt.Color
import java.awt.Font
import java.awt.FontMetrics
import java.awt.Graphics2D
import java.awt.RenderingHints
import java.awt.geom.RoundRectangle2D
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.URI
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.imageio.ImageIO

internal object SharedQuoteImageCoverHelper {
    fun generate(
        createdAt: LocalDate,
        quote: String,
        title: String,
        author: String,
        coverImageUrl: String,
    ): ByteArray {
        val template = readTemplate()
        val image = BufferedImage(template.width, template.height, BufferedImage.TYPE_INT_ARGB)
        val graphics = image.createGraphics()

        try {
            graphics.configureRendering()
            graphics.drawImage(template, 0, 0, null)
            graphics.drawCover(readImage(coverImageUrl))
            graphics.drawQuote(quote)
            graphics.drawSource(title, author)
            graphics.drawDate(createdAt)
        } finally {
            graphics.dispose()
        }

        return image.toPngByteArray()
    }

    private fun Graphics2D.drawCover(coverImage: BufferedImage) {
        withRotated(COVER_CENTER_X, COVER_CENTER_Y, COVER_ROTATION_DEGREES) {
            clip =
                RoundRectangle2D.Float(
                    COVER_X,
                    COVER_Y,
                    COVER_WIDTH,
                    COVER_HEIGHT,
                    COVER_ARC,
                    COVER_ARC,
                )
            drawImage(coverImage, COVER_X.toInt(), COVER_Y.toInt(), COVER_WIDTH.toInt(), COVER_HEIGHT.toInt(), null)
            clip = null
        }
    }

    private fun Graphics2D.drawQuote(quote: String) {
        withRotated(QUOTE_CARD_CENTER_X, QUOTE_CARD_CENTER_Y, CARD_ROTATION_DEGREES) {
            color = DARK_TEXT_COLOR
            font = font(QUOTE_FONT_PATH, QUOTE_FONT_SIZE)
            drawWrappedText(quote, QUOTE_X, QUOTE_BASELINE_Y, QUOTE_MAX_WIDTH, QUOTE_LINE_HEIGHT, QUOTE_MAX_LINES)
        }
    }

    private fun Graphics2D.drawSource(
        title: String,
        author: String,
    ) {
        withRotated(QUOTE_CARD_CENTER_X, QUOTE_CARD_CENTER_Y, CARD_ROTATION_DEGREES) {
            color = GRAY_TEXT_COLOR
            font = font(SOURCE_FONT_PATH, SOURCE_FONT_SIZE)
            drawWrappedText(
                text = "『$title』, $author",
                x = SOURCE_X,
                firstBaselineY = SOURCE_BASELINE_Y,
                maxWidth = SOURCE_MAX_WIDTH,
                lineHeight = SOURCE_LINE_HEIGHT,
                maxLines = SOURCE_MAX_LINES,
            )
        }
    }

    private fun Graphics2D.drawDate(createdAt: LocalDate) {
        withRotated(DATE_CARD_CENTER_X, DATE_CARD_CENTER_Y, DATE_ROTATION_DEGREES) {
            color = Color.WHITE
            font = font(DATE_FONT_PATH, WEEKDAY_FONT_SIZE)
            drawString(createdAt.format(WEEKDAY_FORMATTER), WEEKDAY_X, WEEKDAY_BASELINE_Y)
            color = DARK_TEXT_COLOR
            font = font(DATE_FONT_PATH, MONTH_DAY_FONT_SIZE)
            drawCenteredStringWithLetterSpacing(
                text = createdAt.format(MONTH_DAY_FORMATTER),
                centerX = MONTH_DAY_CENTER_X.toFloat(),
                baselineY = MONTH_DAY_BASELINE_Y.toFloat(),
                spacing = font.size2D * MONTH_DAY_LETTER_SPACING_RATIO,
            )
        }
    }

    private fun Graphics2D.drawCenteredStringWithLetterSpacing(
        text: String,
        centerX: Float,
        baselineY: Float,
        spacing: Float,
    ) {
        var currentX = centerX - text.widthWithLetterSpacing(fontMetrics, spacing) / 2
        text.forEach { char ->
            val value = char.toString()
            drawString(value, currentX, baselineY)
            currentX += fontMetrics.stringWidth(value) + spacing
        }
    }

    private fun String.widthWithLetterSpacing(
        metrics: FontMetrics,
        spacing: Float,
    ): Float {
        if (isEmpty()) return 0F
        val textWidth = sumOf { char -> metrics.stringWidth(char.toString()) }
        return textWidth + spacing * (length - 1)
    }
}

private fun Graphics2D.drawWrappedText(
    text: String,
    x: Int,
    firstBaselineY: Int,
    maxWidth: Int,
    lineHeight: Int,
    maxLines: Int,
) {
    text
        .toWrappedLines(fontMetrics, maxWidth, maxLines)
        .forEachIndexed { index, line ->
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
            if (lines.size == maxLines) return lines.withEllipsis(metrics, maxWidth)
            continue
        }
        currentLine = candidate
    }

    if (currentLine.isNotBlank() && lines.size < maxLines) lines += currentLine.trimEnd()
    return lines
}

private fun List<String>.withEllipsis(
    metrics: FontMetrics,
    maxWidth: Int,
): List<String> {
    if (isEmpty()) return this
    val shortenedLines = toMutableList()
    var shortened = shortenedLines.last()

    while (shortened.isNotEmpty() && metrics.stringWidth("$shortened$ELLIPSIS") > maxWidth) {
        shortened = shortened.dropLast(1)
    }

    shortenedLines[shortenedLines.lastIndex] = "$shortened$ELLIPSIS"
    return shortenedLines
}

private fun Graphics2D.withRotated(
    centerX: Double,
    centerY: Double,
    degrees: Double,
    block: Graphics2D.() -> Unit,
) {
    val originalTransform = transform
    val originalClip = clip
    rotate(Math.toRadians(degrees), centerX, centerY)
    block()
    clip = originalClip
    transform = originalTransform
}

private fun Graphics2D.configureRendering() {
    setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
    setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON)
    setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
}

private fun BufferedImage.toPngByteArray(): ByteArray =
    ByteArrayOutputStream().use { output ->
        ImageIO.write(this, IMAGE_FORMAT, output)
        output.toByteArray()
    }

private fun readTemplate(): BufferedImage =
    ImageIO.read(SharedQuoteImageCoverHelper::class.java.classLoader.getResourceAsStream(TEMPLATE_PATH))
        ?: throw IllegalStateException("공유 이미지 템플릿을 읽을 수 없습니다: $TEMPLATE_PATH")

private fun readImage(url: String): BufferedImage =
    ImageIO.read(URI(url).toURL())
        ?: throw IllegalStateException("이미지를 읽을 수 없습니다: $url")

private fun font(
    path: String,
    size: Float,
): Font = resourceFont(path, size) ?: Font(Font.SANS_SERIF, Font.BOLD, size.toInt())

private fun resourceFont(
    path: String,
    size: Float,
): Font? =
    runCatching {
        val stream =
            SharedQuoteImageCoverHelper::class.java
                .classLoader
                .getResourceAsStream(path)
                ?: return null
        stream.useFont(size)
    }.getOrNull()

private fun InputStream.useFont(size: Float): Font =
    use { stream ->
        Font.createFont(Font.TRUETYPE_FONT, stream).deriveFont(size)
    }

private const val TEMPLATE_PATH = "images/shareviews/shareview_3.png"
private const val IMAGE_FORMAT = "png"
private const val ELLIPSIS = "..."
private const val QUOTE_FONT_PATH = "fonts/Pretendard-Bold.otf"
private const val SOURCE_FONT_PATH = "fonts/Pretendard-SemiBold.otf"
private const val DATE_FONT_PATH = "fonts/GT-Pressura-Bold-Trial.otf"

private const val QUOTE_CARD_CENTER_X = 356.0
private const val QUOTE_CARD_CENTER_Y = 760.0
private const val CARD_ROTATION_DEGREES = -5.11
private const val QUOTE_X = 177
private const val QUOTE_BASELINE_Y = 625
private const val QUOTE_MAX_WIDTH = 390
private const val QUOTE_LINE_HEIGHT = 58
private const val QUOTE_MAX_LINES = 4
private const val QUOTE_FONT_SIZE = 46.08F

private const val SOURCE_X = 175
private const val SOURCE_BASELINE_Y = 1010
private const val SOURCE_MAX_WIDTH = 390
private const val SOURCE_LINE_HEIGHT = 42
private const val SOURCE_MAX_LINES = 1
private const val SOURCE_FONT_SIZE = 34.56F

private const val COVER_X = 584F
private const val COVER_Y = 750F
private const val COVER_WIDTH = 424F
private const val COVER_HEIGHT = 632F
private const val COVER_ARC = 30F
private const val COVER_CENTER_X = 801.0
private const val COVER_CENTER_Y = 1057.0
private const val COVER_ROTATION_DEGREES = 5.11

private const val DATE_CARD_CENTER_X = 407.0
private const val DATE_CARD_CENTER_Y = 1480.0
private const val DATE_ROTATION_DEGREES = -2.0
private const val WEEKDAY_X = 250
private const val WEEKDAY_BASELINE_Y = 1370
private const val MONTH_DAY_CENTER_X = 409
private const val MONTH_DAY_BASELINE_Y = 1570
private const val MONTH_DAY_LETTER_SPACING_RATIO = -0.04F
private const val WEEKDAY_FONT_SIZE = 58F
private const val MONTH_DAY_FONT_SIZE = 103.68F

private val WEEKDAY_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE", Locale.ENGLISH)
private val MONTH_DAY_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.ENGLISH)
private val DARK_TEXT_COLOR: Color = Color.decode("#242424")
private val GRAY_TEXT_COLOR: Color = Color.decode("#646464")
