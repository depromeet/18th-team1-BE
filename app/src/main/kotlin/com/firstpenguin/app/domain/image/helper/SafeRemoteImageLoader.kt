package com.firstpenguin.app.domain.image.helper

import java.awt.image.BufferedImage
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URI
import java.util.Locale
import javax.imageio.ImageIO

internal object SafeRemoteImageLoader {
    fun readOrNull(url: String): BufferedImage? =
        runCatching {
            val uri = URI(url)
            if (!uri.isAllowedImageUri()) return null

            val connection = uri.toURL().openConnection() as? HttpURLConnection ?: return null
            connection.connectTimeout = CONNECT_TIMEOUT_MILLIS
            connection.readTimeout = READ_TIMEOUT_MILLIS
            connection.instanceFollowRedirects = false

            try {
                if (connection.responseCode != HttpURLConnection.HTTP_OK) return null
                if (!connection.contentType
                        .orEmpty()
                        .lowercase(Locale.ROOT)
                        .startsWith(IMAGE_CONTENT_TYPE_PREFIX)
                ) {
                    return null
                }
                if (connection.contentLengthLong > MAX_IMAGE_BYTES) return null

                val bytes = connection.inputStream.use { input -> input.readLimitedBytes() } ?: return null
                ImageIO.read(ByteArrayInputStream(bytes))
            } finally {
                connection.disconnect()
            }
        }.getOrNull()

    private fun URI.isAllowedImageUri(): Boolean =
        scheme.equals(ALLOWED_SCHEME, ignoreCase = true) &&
            host?.lowercase(Locale.ROOT) in ALLOWED_HOSTS

    private fun java.io.InputStream.readLimitedBytes(): ByteArray? {
        val output = ByteArrayOutputStream()
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        var totalBytes = 0L

        while (true) {
            val readBytes = read(buffer)
            if (readBytes == -1) break

            totalBytes += readBytes
            if (totalBytes > MAX_IMAGE_BYTES) return null
            output.write(buffer, 0, readBytes)
        }

        return output.toByteArray()
    }

    private val ALLOWED_HOSTS =
        setOf(
            "image.aladin.co.kr",
            "storage.googleapis.com",
        )

    private const val ALLOWED_SCHEME = "https"
    private const val IMAGE_CONTENT_TYPE_PREFIX = "image/"
    private const val CONNECT_TIMEOUT_MILLIS = 2_000
    private const val READ_TIMEOUT_MILLIS = 3_000
    private const val MAX_IMAGE_BYTES = 5L * 1024L * 1024L
}
