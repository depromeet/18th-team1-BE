package com.firstpenguin.app.domain.image.helper

import java.time.LocalDate

object SharedCalendarImageHelper {
    fun generateShareView4(
        month: LocalDate,
        books: Map<Int, List<String>>,
    ): ByteArray =
        SharedCalendarImageMonthTitleHelper.generate(
            month = month,
            books = books,
        )

    fun generateShareView5(
        month: LocalDate,
        books: Map<Int, List<String>>,
    ): ByteArray =
        SharedCalendarImageHeaderHelper.generate(
            month = month,
            books = books,
        )
}
