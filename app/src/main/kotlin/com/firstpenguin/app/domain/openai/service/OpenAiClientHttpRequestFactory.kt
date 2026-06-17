package com.firstpenguin.app.domain.openai.service

import org.springframework.http.client.ClientHttpRequestFactory
import org.springframework.http.client.JdkClientHttpRequestFactory
import java.net.http.HttpClient
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap

private val openAiHttpClientByConnectTimeout = ConcurrentHashMap<Duration, HttpClient>()

internal fun openAiClientHttpRequestFactory(
    connectTimeout: Duration,
    readTimeout: Duration,
): ClientHttpRequestFactory =
    JdkClientHttpRequestFactory(openAiHttpClient(connectTimeout)).apply {
        setReadTimeout(readTimeout)
    }

private fun openAiHttpClient(connectTimeout: Duration): HttpClient =
    openAiHttpClientByConnectTimeout.computeIfAbsent(connectTimeout) {
        HttpClient
            .newBuilder()
            .connectTimeout(connectTimeout)
            .version(HttpClient.Version.HTTP_2)
            .build()
    }
