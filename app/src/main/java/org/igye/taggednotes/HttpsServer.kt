package org.igye.taggednotes

import android.content.Context
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.network.tls.certificates.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.util.*
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.igye.taggednotes.Utils.createMethodMap
import java.io.File
import java.security.KeyStore
import kotlin.text.toCharArray

class HttpsServer(
    appContext: Context,
    keyStoreFile: File,
    keyAlias: String,
    privateKeyPassword: String,
    keyStorePassword: String,
    portNum: Int,
    javascriptInterface: List<Any>,
    private val defaultDispatcher: CoroutineDispatcher = Dispatchers.Default,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {
    private val logger = LoggerImpl("http-server")
    private val beMethods = createMethodMap(javascriptInterface)

    private val assetsPathHandler: CustomAssetsPathHandler = CustomAssetsPathHandler(
        appContext = appContext,
        feBeBridge = "js/http-fe-be-bridge.js"
    )

    private val keyStore = KeyStore.getInstance(KeyStore.getDefaultType()).also {keyStore->
        keyStoreFile.inputStream().use { keyStore.load(it, keyStorePassword.toCharArray()) }
    }

    private val environment = applicationEngineEnvironment {
        log = LoggerImpl("ktor-app")
        sslConnector(
            keyStore = keyStore,
            keyAlias = keyAlias,
            keyStorePassword = { keyStorePassword.toCharArray() },
            privateKeyPassword = { privateKeyPassword.toCharArray() }
        ) {
            port = portNum
            keyStorePath = keyStoreFile
        }
        module {
            routing {
                get("/{...}") {
                    val path = call.request.path()
                    if ("/" == path || path.startsWith("/css/") || path.startsWith("/js/")) {
                        withContext(ioDispatcher) {
                            val response = assetsPathHandler.handle(if ("/" == path) "index.html" else path)!!
                            call.respondOutputStream(contentType = ContentType.parse(response.mimeType), status = HttpStatusCode.OK) {
                                response.data.use { it.copyTo(this) }
                            }
                        }
                    } else {
                        log.error("Path not found: $path")
                        call.respond(status = HttpStatusCode.NotFound, message = "Not found.")
                    }
                }
                post("/be/{funcName}") {
                    val funcName = call.parameters["funcName"]
                    withContext(defaultDispatcher) {
                        val beMethod = beMethods.get(funcName)
                        if (beMethod == null) {
                            val msg = "backend method '$funcName' was not found"
                            log.error(msg)
                            call.respond(status = HttpStatusCode.NotFound, message = msg)
                        } else {
                            call.respondText(contentType = ContentType.Application.Json, status = HttpStatusCode.OK) {
                                beMethod.invoke(defaultDispatcher, call.receiveText()).await()
                            }
                        }
                    }
                }
            }
        }
    }

    private val httpsServer = embeddedServer(Netty, environment).start(wait = false)

    fun stop() {
        httpsServer.stop(0,0)
    }
}
