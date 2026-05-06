package com.firstpenguin.app.domain.diary.service

import org.springframework.stereotype.Component
import java.awt.Font
import java.awt.GraphicsEnvironment
import java.io.InputStream

@Component
class DiaryShareImageFontProvider {
    fun date(size: Int): Font = resourceFont(DATE_FONT_PATH, size) ?: boldFont(size)

    fun quote(size: Int): Font = resourceFont(QUOTE_FONT_PATH, size) ?: boldFont(size)

    fun book(size: Int): Font = resourceFont(BOOK_FONT_PATH, size) ?: boldFont(size)

    private fun resourceFont(
        path: String,
        size: Int,
    ): Font? =
        runCatching {
            val stream =
                javaClass
                    .classLoader
                    .getResourceAsStream(path)
                    ?: return null
            stream.useFont(size)
        }.getOrNull()

    private fun InputStream.useFont(size: Int): Font =
        use { stream ->
            Font.createFont(Font.TRUETYPE_FONT, stream).deriveFont(size.toFloat())
        }

    private fun boldFont(size: Int): Font = Font(fontFamily(), Font.BOLD, size)

    private fun fontFamily(): String {
        val availableFonts = GraphicsEnvironment.getLocalGraphicsEnvironment().availableFontFamilyNames.toSet()
        return FONT_CANDIDATES.firstOrNull(availableFonts::contains) ?: Font.SANS_SERIF
    }

    private companion object {
        const val DATE_FONT_PATH = "fonts/GT-Pressura-Extended-Bold-Trial.otf"
        const val QUOTE_FONT_PATH = "fonts/Pretendard-ExtraBold.otf"
        const val BOOK_FONT_PATH = "fonts/Pretendard-Bold.otf"

        val FONT_CANDIDATES =
            listOf(
                "Apple SD Gothic Neo",
                "Noto Sans CJK KR",
                "Noto Sans KR",
                "NanumGothic",
                "Malgun Gothic",
                Font.SANS_SERIF,
            )
    }
}
