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
package net.ccbluex.liquidbounce.api.services.auth

import com.sun.net.httpserver.HttpServer
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import net.ccbluex.liquidbounce.api.core.ApiConfig.Companion.AUTH_AUTHORIZE_URL
import net.ccbluex.liquidbounce.api.core.ApiConfig.Companion.AUTH_CLIENT_ID
import net.ccbluex.liquidbounce.api.models.auth.ClientAccount
import net.ccbluex.liquidbounce.api.models.auth.OAuthSession
import net.ccbluex.liquidbounce.event.EventListener
import net.ccbluex.liquidbounce.utils.client.logger
import java.net.InetSocketAddress
import java.util.UUID
import java.util.concurrent.Executors
import java.util.function.Consumer
import kotlin.time.Duration.Companion.seconds

/**
 * OAuth client for handling the authentication flow
 */
object OAuthClient : EventListener {

    @Volatile
    private var serverPort: Int? = null

    @Volatile
    private var authCodeDeferred: CompletableDeferred<String>? = null

    @Volatile
    private var server: HttpServer? = null

    /**
     * Start the OAuth authentication flow
     *
     * @param onUrl Callback for when the authorization URL is ready
     * @return Client account with the authenticated session
     */
    suspend fun startAuth(onUrl: Consumer<String>): ClientAccount {
        val (codeVerifier, codeChallenge) = PKCEUtils.generatePKCE()
        val state = UUID.randomUUID().toString()

        if (serverPort == null) {
            serverPort = startCallbackServer()
        }

        val redirectUri = "http://127.0.0.1:$serverPort/"
        logger.info("OAuth server started on port $serverPort.")
        val authUrl = buildAuthUrl(codeChallenge, state, redirectUri)

        onUrl.accept(authUrl)
        val code = waitForAuthCode()
        val tokenResponse = AuthenticationApi.exchangeToken(AUTH_CLIENT_ID, code, codeVerifier, redirectUri)

        serverPort = null

        return ClientAccount(session = tokenResponse.toAuthSession())
    }

    /**
     * Renew an expired session using its refresh token
     */
    suspend fun renewToken(session: OAuthSession): OAuthSession {
        val tokenResponse = AuthenticationApi.refreshToken(AUTH_CLIENT_ID, session.refreshToken)
        return tokenResponse.toAuthSession()
    }

    private suspend fun startCallbackServer(): Int {
        val deferred = CompletableDeferred<String>()
        authCodeDeferred = deferred

        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.executor = Executors.newSingleThreadExecutor { runnable ->
            Thread(runnable, "OAuth-Callback-Server").apply { isDaemon = true }
        }

        server.createContext("/") { exchange ->
            try {
                val query = exchange.requestURI.query
                val code = query
                    ?.split("&")
                    ?.map { it.split("=", limit = 2) }
                    ?.firstOrNull { it.size == 2 && it[0] == "code" }
                    ?.get(1)

                val responseBody = if (code != null) {
                    deferred.complete(code)
                    SUCCESS_HTML
                } else {
                    deferred.completeExceptionally(
                        IllegalArgumentException("No code found in the redirect URL")
                    )
                    ERROR_HTML
                }

                val bytes = responseBody.toByteArray(Charsets.UTF_8)
                exchange.responseHeaders.add("Content-Type", "text/html; charset=utf-8")
                exchange.sendResponseHeaders(200, bytes.size.toLong())
                exchange.responseBody.use { it.write(bytes) }
            } catch (e: Exception) {
                logger.error("Failed to handle OAuth callback", e)
                exchange.sendResponseHeaders(500, -1)
                exchange.responseBody.close()
            }
        }

        server.start()
        this.server = server

        val port = server.address.port

        deferred.invokeOnCompletion {
            this.server = null
            server.stop(1)
        }

        return port
    }

    private fun buildAuthUrl(codeChallenge: String, state: String, redirectUri: String): String {
        return "$AUTH_AUTHORIZE_URL?client_id=$AUTH_CLIENT_ID&redirect_uri=$redirectUri&" +
            "response_type=code&state=$state&code_challenge=$codeChallenge&code_challenge_method=S256"
    }

    private suspend fun waitForAuthCode(): String = withTimeout(120.seconds) {
        while (true) {
            val current = authCodeDeferred
            if (current != null) {
                return@withTimeout current.await()
            }
            delay(50)
        }
        @Suppress("UNREACHABLE_CODE")
        error("Authorization timed out")
    }

    private const val SUCCESS_HTML = """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <meta name="viewport" content="width=device-width, initial-scale=1.0">
            <title>Authentication Successful</title>
            <style>
                body { font-family: Arial, sans-serif; background-color: #121212; color: #ffffff; text-align: center; padding: 50px; }
                .container { background-color: #1E1E1E; padding: 20px; border-radius: 8px; box-shadow: 0 0 10px rgba(0, 0, 0, 0.5); display: inline-block; }
                h1 { color: #4CAF50; }
            </style>
        </head>
        <body>
            <div class="container">
                <h1>Authentication Successful</h1>
                <p>You have successfully authenticated. You can close this tab now.</p>
            </div>
        </body>
        </html>
    """

    private const val ERROR_HTML = """
        <!DOCTYPE html>
        <html lang="en">
        <head>
            <meta charset="UTF-8">
            <title>Authentication Failed</title>
        </head>
        <body>
            <h1>Authentication Failed</h1>
            <p>No authorization code was received. Please try again.</p>
        </body>
        </html>
    """
}
