/*
 * This file is part of LiquidBounce (https://github.com/CCBlueX/LiquidBounce)
 *
 * Copyright (c) 2015 - 2026 CCBlueX
 *
 * LiquidBounce is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * LiquidBounce is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with LiquidBounce. If not, see <https://www.gnu.org/licenses/>.
 */
package net.ccbluex.liquidbounce.api.core

import okhttp3.Interceptor
import okhttp3.MediaType
import okhttp3.Response
import okhttp3.ResponseBody
import okio.Buffer
import okio.BufferedSource
import okio.ForwardingSource
import okio.Source
import okio.buffer

fun interface ProgressListener {
    fun update(bytesRead: Long, contentLength: Long, done: Boolean)
}

class ProgressInterceptor(private val listener: ProgressListener) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalResponse = chain.proceed(chain.request())
        return originalResponse.newBuilder()
            .body(ProgressResponseBody(originalResponse.body, listener))
            .build()
    }

    private class ProgressResponseBody(
        private val responseBody: ResponseBody?,
        private val progressListener: ProgressListener
    ) : ResponseBody() {
        override fun contentType(): MediaType? = responseBody?.contentType()
        override fun contentLength(): Long = responseBody?.contentLength() ?: -1L
        override fun source(): BufferedSource {
            val source = responseBody?.source() ?: return Buffer()
            return source.let { bodySource ->
                object : ForwardingSource(bodySource) {
                    var totalBytesRead = 0L

                    override fun read(sink: Buffer, byteCount: Long): Long {
                        val bytesRead = super.read(sink, byteCount)
                        totalBytesRead += if (bytesRead != -1L) bytesRead else 0
                        progressListener.update(totalBytesRead, contentLength(), bytesRead == -1L)
                        return bytesRead
                    }
                }
            }.buffer()
        }
    }
}
