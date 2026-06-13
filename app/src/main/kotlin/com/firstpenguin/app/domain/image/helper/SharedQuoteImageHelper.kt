package com.firstpenguin.app.domain.image.helper

import java.awt.Color
import java.awt.Font
import java.awt.FontMetrics
import java.awt.Graphics2D
import java.awt.LinearGradientPaint
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.imageio.ImageIO

object SharedQuoteImageHelper {
    fun generate(
        type: Int,
        createdAt: LocalDate,
        quote: String,
        title: String,
        author: String,
        coverImageUrl: String?,
    ): ByteArray =
        when (type) {
            SHARE_VIEW_1_TYPE -> {
                generateShareView1(
                    createdAt = createdAt,
                    quote = quote,
                    title = title,
                    author = author,
                )
            }

            SHARE_VIEW_2_TYPE -> {
                generateShareView2(
                    createdAt = createdAt,
                    quote = quote,
                    title = title,
                    author = author,
                )
            }

            SHARE_VIEW_3_TYPE -> {
                generateShareView3(
                    createdAt = createdAt,
                    quote = quote,
                    title = title,
                    author = author,
                    coverImageUrl =
                        coverImageUrl?.takeIf { it.isNotBlank() }
                            ?: throw IllegalArgumentException("type 3은 coverImageUrl이 필요합니다."),
                )
            }

            else -> {
                throw IllegalArgumentException("지원하지 않는 공유 이미지 type입니다: $type")
            }
        }

    fun generateShareView1(
        createdAt: LocalDate,
        quote: String,
        title: String,
        author: String,
    ): ByteArray =
        ShareView1QuoteImageHelper.generate(
            createdAt = createdAt,
            quote = quote,
            title = title,
            author = author,
        )

    fun generateShareView2(
        createdAt: LocalDate,
        quote: String,
        title: String,
        author: String,
    ): ByteArray =
        ShareView2QuoteImageHelper.generate(
            createdAt = createdAt,
            quote = quote,
            title = title,
            author = author,
        )

    fun generateShareView3(
        createdAt: LocalDate,
        quote: String,
        title: String,
        author: String,
        coverImageUrl: String,
    ): ByteArray =
        SharedQuoteImageCoverHelper.generate(
            createdAt = createdAt,
            quote = quote,
            title = title,
            author = author,
            coverImageUrl = coverImageUrl,
        )

    private const val SHARE_VIEW_1_TYPE = 1
    private const val SHARE_VIEW_2_TYPE = 2
    private const val SHARE_VIEW_3_TYPE = 3
}

private object ShareView1QuoteImageHelper {
    fun generate(
        createdAt: LocalDate,
        quote: String,
        title: String,
        author: String,
    ): ByteArray {
        val image = readShareViewTemplate(SHARE_VIEW_1_TEMPLATE_PATH)
        val graphics = image.createGraphics()

        try {
            graphics.configureShareViewRendering()
            graphics.scale(image.width.toDouble() / SHARE_VIEW_1_WIDTH, image.height.toDouble() / SHARE_VIEW_1_HEIGHT)
            graphics.drawShareView1Date(createdAt)
            graphics.drawShareView1Quote(quote)
            graphics.drawShareView1Source(title, author)
        } finally {
            graphics.dispose()
        }

        return image.toPngByteArray()
    }

    private fun Graphics2D.drawShareView1Date(createdAt: LocalDate) {
        color = HEADER_TEXT_COLOR
        font = font(DATE_FONT_PATH, SHARE_VIEW_1_DATE_FONT_SIZE)
        drawString(createdAt.format(SHARE_VIEW_1_DATE_FORMATTER), SHARE_VIEW_1_DATE_X, SHARE_VIEW_1_DATE_BASELINE_Y)
    }

    private fun Graphics2D.drawShareView1Quote(quote: String) {
        color = BRAND_COLOR
        font = font(QUOTE_FONT_PATH, SHARE_VIEW_1_QUOTE_FONT_SIZE)
        drawWrappedText(
            text = quote,
            x = SHARE_VIEW_1_QUOTE_X,
            firstBaselineY = SHARE_VIEW_1_QUOTE_BASELINE_Y,
            maxWidth = SHARE_VIEW_1_QUOTE_MAX_WIDTH,
            lineHeight = SHARE_VIEW_1_QUOTE_LINE_HEIGHT,
            maxLines = SHARE_VIEW_1_QUOTE_MAX_LINES,
        )
    }

    private fun Graphics2D.drawShareView1Source(
        title: String,
        author: String,
    ) {
        color = BRAND_COLOR
        font = font(SOURCE_FONT_PATH, SHARE_VIEW_1_SOURCE_FONT_SIZE)
        drawWrappedText(
            text = "『$title』, $author",
            x = SHARE_VIEW_1_SOURCE_X,
            firstBaselineY = SHARE_VIEW_1_SOURCE_BASELINE_Y,
            maxWidth = SHARE_VIEW_1_SOURCE_MAX_WIDTH,
            lineHeight = SHARE_VIEW_1_SOURCE_LINE_HEIGHT,
            maxLines = SHARE_VIEW_1_SOURCE_MAX_LINES,
        )
    }
}

private object ShareView2QuoteImageHelper {
    fun generate(
        createdAt: LocalDate,
        quote: String,
        title: String,
        author: String,
    ): ByteArray {
        val image = readShareViewTemplate(SHARE_VIEW_2_TEMPLATE_PATH)
        val graphics = image.createGraphics()

        try {
            graphics.configureShareViewRendering()
            graphics.scale(image.width.toDouble() / BASE_WIDTH, image.height.toDouble() / BASE_HEIGHT)
            graphics.drawShareView2Date(createdAt)
            graphics.drawShareView2Quote(quote)
            graphics.drawShareView2Source(title, author)
        } finally {
            graphics.dispose()
        }

        return image.toPngByteArray()
    }

    private fun Graphics2D.drawShareView2Date(createdAt: LocalDate) {
        font = font(MONTH_FONT_PATH, MONTH_FONT_SIZE)
        val month = createdAt.format(MONTH_FORMATTER)
        paint = monthGradientPaint(fontMetrics)
        drawCenteredStringWithLetterSpacing(
            text = month,
            centerX = CANVAS_CENTER_X,
            baselineY = MONTH_BASELINE_Y,
            spacing = font.size2D * MONTH_LETTER_SPACING_RATIO,
        )

        color = HEADER_TEXT_COLOR
        font = font(DATE_FONT_PATH, DATE_FONT_SIZE)
        drawString(createdAt.format(DATE_FORMATTER), DATE_X, DATE_BASELINE_Y)
    }

    private fun Graphics2D.drawShareView2Quote(quote: String) {
        color = BRAND_COLOR
        font = font(QUOTE_FONT_PATH, QUOTE_FONT_SIZE)
        drawWrappedText(
            text = quote,
            x = QUOTE_X,
            firstBaselineY = QUOTE_BASELINE_Y,
            maxWidth = QUOTE_MAX_WIDTH,
            lineHeight = QUOTE_LINE_HEIGHT,
            maxLines = QUOTE_MAX_LINES,
        )
    }

    private fun Graphics2D.drawShareView2Source(
        title: String,
        author: String,
    ) {
        color = BRAND_COLOR
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

    private fun Graphics2D.drawCenteredStringWithLetterSpacing(
        text: String,
        centerX: Int,
        baselineY: Int,
        spacing: Float,
    ) {
        var x = centerX - text.widthWithLetterSpacing(fontMetrics, spacing) / 2
        text.forEach { char ->
            val value = char.toString()
            drawString(value, x, baselineY.toFloat())
            x += fontMetrics.stringWidth(value) + spacing
        }
    }

    private fun monthGradientPaint(metrics: FontMetrics): LinearGradientPaint {
        val top = MONTH_BASELINE_Y - metrics.ascent
        val bottom = MONTH_BASELINE_Y + metrics.descent
        return LinearGradientPaint(
            CANVAS_CENTER_X.toFloat(),
            top.toFloat(),
            CANVAS_CENTER_X.toFloat(),
            bottom.toFloat(),
            MONTH_GRADIENT_FRACTIONS,
            MONTH_GRADIENT_COLORS,
        )
    }
}

private fun readShareViewTemplate(path: String): BufferedImage {
    val stream =
        SharedQuoteImageHelper::class
            .java
            .classLoader
            .getResourceAsStream(path)
            ?: throw IllegalStateException("공유 이미지 템플릿을 찾을 수 없습니다: $path")

    return stream.use { ImageIO.read(it) }
        ?: throw IllegalStateException("공유 이미지 템플릿을 읽을 수 없습니다: $path")
}

private fun BufferedImage.toPngByteArray(): ByteArray =
    ByteArrayOutputStream().use { output ->
        ImageIO.write(this, SHARE_VIEW_IMAGE_FORMAT, output)
        output.toByteArray()
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

            if (lines.size == maxLines) {
                return lines.withEllipsis(metrics, maxWidth)
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

private fun String.widthWithLetterSpacing(
    metrics: FontMetrics,
    spacing: Float,
): Float {
    if (isEmpty()) return 0F
    val textWidth = sumOf { char -> metrics.stringWidth(char.toString()) }
    return textWidth + spacing * (length - 1)
}

private fun Graphics2D.configureShareViewRendering() {
    setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
    setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
    setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON)
    setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
}

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
            SharedQuoteImageHelper::class
                .java
                .classLoader
                .getResourceAsStream(path)
                ?: return null
        stream.useFont(size)
    }.getOrNull()

private fun InputStream.useFont(size: Float): Font =
    use { stream ->
        Font.createFont(Font.TRUETYPE_FONT, stream).deriveFont(size)
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

private const val SHARE_VIEW_1_TEMPLATE_PATH = "images/shareviews/shareview_1.png"
private const val SHARE_VIEW_2_TEMPLATE_PATH = "images/shareviews/shareview_2.png"
private const val SHARE_VIEW_IMAGE_FORMAT = "png"
private const val ELLIPSIS = "..."

private const val SHARE_VIEW_1_WIDTH = 1080
private const val SHARE_VIEW_1_HEIGHT = 1920
private const val SHARE_VIEW_1_DATE_X = 84
private const val SHARE_VIEW_1_DATE_BASELINE_Y = 179
private const val SHARE_VIEW_1_QUOTE_X = 78
private const val SHARE_VIEW_1_QUOTE_BASELINE_Y = 455
private const val SHARE_VIEW_1_QUOTE_MAX_WIDTH = 925
private const val SHARE_VIEW_1_QUOTE_LINE_HEIGHT = 112
private const val SHARE_VIEW_1_QUOTE_MAX_LINES = 3
private const val SHARE_VIEW_1_SOURCE_X = 78
private const val SHARE_VIEW_1_SOURCE_BASELINE_Y = 1800
private const val SHARE_VIEW_1_SOURCE_MAX_WIDTH = 925
private const val SHARE_VIEW_1_SOURCE_LINE_HEIGHT = 60
private const val SHARE_VIEW_1_SOURCE_MAX_LINES = 1
private const val SHARE_VIEW_1_DATE_FONT_SIZE = 92F
private const val SHARE_VIEW_1_QUOTE_FONT_SIZE = 72F
private const val SHARE_VIEW_1_SOURCE_FONT_SIZE = 48F

private const val BASE_WIDTH = 375
private const val BASE_HEIGHT = 667
private const val CANVAS_CENTER_X = 187
private const val MONTH_BASELINE_Y = 161
private const val DATE_X = 76
private const val DATE_BASELINE_Y = 218
private const val QUOTE_X = 76
private const val QUOTE_BASELINE_Y = 276
private const val QUOTE_MAX_WIDTH = 230
private const val QUOTE_LINE_HEIGHT = 30
private const val QUOTE_MAX_LINES = 4
private const val SOURCE_X = 76
private const val SOURCE_BASELINE_Y = 543
private const val SOURCE_MAX_WIDTH = 230
private const val SOURCE_LINE_HEIGHT = 18
private const val SOURCE_MAX_LINES = 1

private const val MONTH_FONT_SIZE = 69.48F
private const val MONTH_LETTER_SPACING_RATIO = -0.04F
private const val DATE_FONT_SIZE = 16.01F
private const val QUOTE_FONT_SIZE = 18.01F
private const val SOURCE_FONT_SIZE = 14.01F

private const val MONTH_FONT_PATH = "fonts/GT-Pressura-Bold-Trial.otf"
private const val DATE_FONT_PATH = "fonts/GT-Pressura-Bold-Trial.otf"
private const val QUOTE_FONT_PATH = "fonts/Pretendard-Bold.otf"
private const val SOURCE_FONT_PATH = "fonts/Pretendard-SemiBold.otf"

private val SHARE_VIEW_1_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM d", Locale.ENGLISH)
private val MONTH_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM", Locale.ENGLISH)
private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE d", Locale.ENGLISH)
private const val MONTH_GRADIENT_END_FRACTION = 1F

private val MONTH_GRADIENT_FRACTIONS = floatArrayOf(0F, MONTH_GRADIENT_END_FRACTION)
private val MONTH_GRADIENT_COLORS =
    arrayOf(
        colorWithAlpha("#242424", OPAQUE_ALPHA),
        colorWithAlpha("#242424", HALF_ALPHA),
    )
private const val OPAQUE_ALPHA = 255
private const val HALF_ALPHA = 128
private val HEADER_TEXT_COLOR: Color = Color.WHITE
private val BRAND_COLOR: Color = Color.decode("#7A1B43")

private fun colorWithAlpha(
    hex: String,
    alpha: Int,
): Color {
    val color = Color.decode(hex)
    return Color(color.red, color.green, color.blue, alpha)
}
