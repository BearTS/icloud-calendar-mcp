package org.onekash.mcp.calendar

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.engine.embeddedServer
import io.ktor.server.netty.Netty
import io.ktor.server.request.path
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.modelcontextprotocol.kotlin.sdk.server.Server
import io.modelcontextprotocol.kotlin.sdk.server.mcpStreamableHttp
import java.security.MessageDigest
import kotlin.system.exitProcess

/** HTTP is opt-in via `--http` or MCP_TRANSPORT=http; STDIO stays the default. */
internal fun httpModeRequested(args: Array<String>): Boolean =
    "--http" in args || System.getenv("MCP_TRANSPORT").equals("http", ignoreCase = true)

/** Constant-time check of an `Authorization: Bearer` header against the expected token. */
internal fun isAuthorized(header: String?, token: String): Boolean {
    val presented = header?.removePrefix("Bearer ")?.takeIf { header.startsWith("Bearer ") } ?: return false
    return MessageDigest.isEqual(presented.toByteArray(), token.toByteArray())
}

/**
 * Serves MCP over Streamable HTTP at /mcp, guarded by a bearer token.
 * Refuses to start without MCP_AUTH_TOKEN, since the server holds calendar write access.
 */
internal fun runHttpServer(newServer: () -> Server) {
    val token = System.getenv("MCP_AUTH_TOKEN")?.takeIf { it.isNotBlank() }
    if (token == null) {
        System.err.println("MCP_AUTH_TOKEN must be set for HTTP mode.")
        exitProcess(2)
    }
    val host = System.getenv("MCP_HOST") ?: "0.0.0.0"
    val port = System.getenv("MCP_PORT")?.toIntOrNull() ?: 8000
    // DNS-rebinding protection is on only when allowed hosts are listed; the token guards otherwise.
    val allowedHosts = System.getenv("MCP_ALLOWED_HOSTS")?.split(',')?.map { it.trim() }?.filter { it.isNotEmpty() }

    embeddedServer(Netty, port = port, host = host) {
        routing { get("/healthz") { call.respondText("ok") } }
        intercept(ApplicationCallPipeline.Plugins) {
            if (call.request.path() == "/healthz") return@intercept
            if (!isAuthorized(call.request.headers["Authorization"], token)) {
                call.respondText("Unauthorized", status = HttpStatusCode.Unauthorized)
                finish()
            }
        }
        mcpStreamableHttp(
            enableDnsRebindingProtection = allowedHosts != null,
            allowedHosts = allowedHosts,
        ) { newServer() }
    }.start(wait = true)
}
