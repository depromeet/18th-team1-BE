package com.firstpenguin.app.global.exception

import com.firstpenguin.app.global.response.ErrorResponse
import org.slf4j.LoggerFactory
import org.springframework.http.ResponseEntity
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.resource.NoResourceFoundException

@RestControllerAdvice
class GlobalExceptionHandler {
    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(CustomException::class)
    fun handleCustomException(e: CustomException): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(e.errorCode.status).body(ErrorResponse.of(e.errorCode))

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleValidationException(e: MethodArgumentNotValidException): ResponseEntity<ErrorResponse> {
        val message =
            e.bindingResult.fieldErrors
                .firstOrNull()
                ?.defaultMessage
                ?: ErrorCode.INVALID_INPUT.message
        return ResponseEntity.badRequest().body(ErrorResponse.of(message))
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun handleMethodNotAllowed(): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(ErrorCode.METHOD_NOT_ALLOWED.status).body(ErrorResponse.of(ErrorCode.METHOD_NOT_ALLOWED))

    @ExceptionHandler(NoResourceFoundException::class)
    fun handleNoResourceFound(): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(ErrorCode.NOT_FOUND.status).body(ErrorResponse.of(ErrorCode.NOT_FOUND))

    @ExceptionHandler(Exception::class)
    fun handleException(e: Exception): ResponseEntity<ErrorResponse> {
        log.error("Unhandled exception occurred", e)
        return ResponseEntity.internalServerError().body(ErrorResponse.of(ErrorCode.INTERNAL_SERVER_ERROR))
    }
}
