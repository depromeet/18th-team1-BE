@file:Suppress("MagicNumber")

package com.firstpenguin.app.domain.image.helper

import java.awt.Color
import java.awt.Font
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

internal object SharedCalendarImageHeaderHelper {
    fun generate(
        month: LocalDate,
        books: Map<Int, List<String>>,
    ): ByteArray {
        val template = readTemplate()
        val image = BufferedImage(template.width, template.height, BufferedImage.TYPE_INT_ARGB)
        val graphics = image.createGraphics()
        val loadedBooks = books.mapValues { (_, urls) -> urls.map(::readImage) }

        try {
            graphics.configureRendering()
            graphics.drawImage(template, 0, 0, null)
            graphics.drawHeader(month)
            graphics.drawCalendar(month, loadedBooks)
        } finally {
            graphics.dispose()
        }

        return image.toPngByteArray()
    }

    private fun Graphics2D.drawHeader(month: LocalDate) {
        color = Color.WHITE
        font = font(HEADER_FONT_PATH, HEADER_FONT_SIZE)
        drawString(month.format(HEADER_FORMATTER), HEADER_X, HEADER_BASELINE_Y)
    }

    private fun Graphics2D.drawCalendar(
        month: LocalDate,
        books: Map<Int, List<BufferedImage>>,
    ) {
        val firstDay = month.withDayOfMonth(1)
        val lastDay = firstDay.lengthOfMonth()
        // Java DayOfWeek: MON=1..SUN=7 → Korean: 일=0, 월=1, ..., 토=6
        val startCol = firstDay.dayOfWeek.value % 7

        for (day in 1..lastDay) {
            val offset = startCol + day - 1
            val col = offset % 7
            val row = offset / 7
            val cellX = COLUMN_CENTERS[col] - (CELL_WIDTH / 2).toInt()
            val cellY = FIRST_CELL_TOP + row * SLOT_HEIGHT
            drawCell(cellX, cellY)
            drawDayNumber(day, cellX, cellY)
        }

        books.forEach { (day, coverImages) ->
            val offset = startCol + day - 1
            val col = offset % 7
            val row = offset / 7
            val coverCenterX = COLUMN_CENTERS[col]
            val cellCenterY = FIRST_CELL_TOP + row * SLOT_HEIGHT + (CELL_HEIGHT / 2).toInt()
            val coverY = cellCenterY - COVER_HEIGHT / 2
            drawStackedCovers(coverImages, coverCenterX, coverY)
        }
    }

    private fun Graphics2D.drawStackedCovers(
        coverImages: List<BufferedImage>,
        centerX: Int,
        baseY: Int,
    ) {
        if (coverImages.isEmpty())
            return

        val count = coverImages.size
        val stackCount = count.coerceAtMost(MAX_STACK_COUNT)
        for (i in stackCount - 1 downTo 0) {
            val offsetX = (stackCount - 1 - i) * STACK_OFFSET_X
            val offsetY = (stackCount - 1 - i) * STACK_OFFSET_Y
            drawCover(coverImages[i], centerX - COVER_WIDTH / 2 + offsetX, baseY + offsetY)
        }
        if (count >= 2) {
            val frontCoverX = centerX - COVER_WIDTH / 2 + (stackCount - 1) * STACK_OFFSET_X
            val frontCoverY = baseY + (stackCount - 1) * STACK_OFFSET_Y
            drawCountBadge(count, frontCoverX, frontCoverY, coverImages.first())
        }
    }

    private fun Graphics2D.drawCountBadge(
        count: Int,
        coverX: Int,
        coverY: Int,
        coverImage: BufferedImage,
    ) {
        val badgeCenterX = coverX + COVER_WIDTH - BADGE_RADIUS - BADGE_INSET
        val badgeCenterY = coverY + BADGE_RADIUS + BADGE_INSET
        val isLightCover = coverImage.isLightNear(COVER_WIDTH - BADGE_RADIUS - BADGE_INSET, BADGE_RADIUS + BADGE_INSET)
        color = if (isLightCover) BADGE_BG_COLOR_DARK else BADGE_BG_COLOR_LIGHT
        fillOval(badgeCenterX - BADGE_RADIUS, badgeCenterY - BADGE_RADIUS, BADGE_RADIUS * 2, BADGE_RADIUS * 2)
        color = Color.WHITE
        font = font(DATE_FONT_PATH, BADGE_FONT_SIZE)
        val text = count.toString()
        val textWidth = fontMetrics.stringWidth(text)
        val baselineY = badgeCenterY + (fontMetrics.ascent - fontMetrics.descent) / 2
        drawString(text, badgeCenterX - textWidth / 2, baselineY)
    }

    private fun Graphics2D.drawCell(
        x: Int,
        y: Int,
    ) {
        color = CELL_BG_COLOR
        fill(RoundRectangle2D.Float(x.toFloat(), y.toFloat(), CELL_WIDTH, CELL_HEIGHT, CELL_ARC, CELL_ARC))
    }

    private fun Graphics2D.drawDayNumber(
        day: Int,
        cellX: Int,
        cellY: Int,
    ) {
        color = DAY_TEXT_COLOR
        font = font(DATE_FONT_PATH, DATE_FONT_SIZE)
        val text = day.toString()
        val spacing = DATE_FONT_SIZE * LETTER_SPACING_RATIO
        val textWidth = text.sumOf { fontMetrics.stringWidth(it.toString()) } + (spacing * (text.length - 1)).toInt()
        val startX = cellX + ((CELL_WIDTH - textWidth) / 2).toInt()
        val baselineY = cellY + ((CELL_HEIGHT + fontMetrics.ascent - fontMetrics.descent) / 2).toInt()
        var currentX = startX.toFloat()
        text.forEach { char ->
            drawString(char.toString(), currentX, baselineY.toFloat())
            currentX += fontMetrics.stringWidth(char.toString()) + spacing
        }
    }

    private fun Graphics2D.drawCover(
        coverImage: BufferedImage,
        x: Int,
        y: Int,
    ) {
        val savedClip = clip
        clip =
            RoundRectangle2D.Float(
                x.toFloat(),
                y.toFloat(),
                COVER_WIDTH.toFloat(),
                COVER_HEIGHT.toFloat(),
                COVER_ARC,
                COVER_ARC,
            )
        drawImage(coverImage, x, y, COVER_WIDTH, COVER_HEIGHT, null)
        clip = savedClip
    }
}

    private fun Graphics2D.configureRendering() {
        setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON)
        setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON)
        setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, RenderingHints.VALUE_FRACTIONALMETRICS_ON)
        setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY)
        setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC)
    }

    private fun BufferedImage.isLightNear(x: Int, y: Int): Boolean {
        val sampleSize = 10
        val scaleX = width.toFloat() / COVER_WIDTH
        val scaleY = height.toFloat() / COVER_HEIGHT
        val srcX = (x * scaleX).toInt().coerceIn(0, width - 1)
        val srcY = (y * scaleY).toInt().coerceIn(0, height - 1)
        var total = 0L
        var count = 0
        for (dy in -sampleSize..sampleSize) {
            for (dx in -sampleSize..sampleSize) {
                val px = (srcX + dx).coerceIn(0, width - 1)
                val py = (srcY + dy).coerceIn(0, height - 1)
                val rgb = getRGB(px, py)
                total += ((rgb shr 16) and 0xFF) + ((rgb shr 8) and 0xFF) + (rgb and 0xFF)
                count++
            }
        }
        return total / (count * 3) >= LIGHT_COVER_THRESHOLD
    }

    private fun readTemplate(): BufferedImage =
        ImageIO.read(SharedCalendarImageHeaderHelper::class.java.classLoader.getResourceAsStream(TEMPLATE_PATH))
            ?: throw IllegalStateException("공유 이미지 템플릿을 읽을 수 없습니다: $TEMPLATE_PATH")

    private fun readImage(url: String): BufferedImage =
        ImageIO.read(URI(url).toURL())
            ?: throw IllegalStateException("이미지를 읽을 수 없습니다: $url")

    private fun font(
        path: String,
        size: Float,
    ): Font = resourceFont(path, size) ?: Font(Font.SANS_SERIF, Font.PLAIN, size.toInt())

    private fun resourceFont(
        path: String,
        size: Float,
    ): Font? =
        runCatching {
            val stream =
                SharedCalendarImageHeaderHelper::class.java
                    .classLoader
                    .getResourceAsStream(path)
                    ?: return null
            stream.useFont(size)
        }.getOrNull()

    private fun InputStream.useFont(size: Float): Font =
        use { stream ->
            Font.createFont(Font.TRUETYPE_FONT, stream).deriveFont(size)
        }

    private fun BufferedImage.toPngByteArray(): ByteArray =
        ByteArrayOutputStream().use { output ->
            ImageIO.write(this, IMAGE_FORMAT, output)
            output.toByteArray()
        }

    private val COLUMN_CENTERS = intArrayOf(160, 285, 410, 535, 660, 785, 910)

    private const val IMAGE_FORMAT = "png"
    private const val TEMPLATE_PATH = "images/shareviews/shareview_5.png"

    private const val HEADER_X = 87
    private const val HEADER_BASELINE_Y = 190
    private const val HEADER_FONT_SIZE = 90F
    private const val HEADER_FONT_PATH = "fonts/GT-Pressura-Bold-Trial.otf"

    private const val FIRST_CELL_TOP = 585
    private const val SLOT_HEIGHT = 240
    private const val CELL_WIDTH = 110F
    private const val CELL_HEIGHT = 110F
    private const val CELL_ARC = 20F

    private const val DATE_FONT_PATH = "fonts/Pretendard-Medium.otf"
    private const val DATE_FONT_SIZE = 30F
    private const val LETTER_SPACING_RATIO = -0.02F

    private const val COVER_WIDTH = 100
    private const val COVER_HEIGHT = 155
    private const val COVER_ARC = 12F
    private const val MAX_STACK_COUNT = 2
    private const val STACK_OFFSET_X = 6
    private const val STACK_OFFSET_Y = -6
    private const val BADGE_RADIUS = 18
    private const val BADGE_INSET = 6
    private const val BADGE_FONT_SIZE = 22F
    private const val LIGHT_COVER_THRESHOLD = 220
    private val BADGE_BG_COLOR_DARK = Color(0x5C, 0x5C, 0x5C, 128)
    private val BADGE_BG_COLOR_LIGHT = Color(0xFF, 0xFF, 0xFF, 51)

    private val CELL_BG_COLOR = Color(0xF7, 0xF7, 0xF7)
    private val DAY_TEXT_COLOR = Color(0xDC, 0xDC, 0xDC)

    private val HEADER_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("MMMM", Locale.ENGLISH)
