package com.firstpenguin.app.global.exception

class CustomException(
    val errorCode: ErrorCode,
) : RuntimeException(errorCode.message)
