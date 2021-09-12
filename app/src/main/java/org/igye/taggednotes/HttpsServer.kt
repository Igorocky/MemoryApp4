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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.security.KeyStore

class HttpsServer(applicationContext: Context) {
    private val logger = LoggerImpl(">>>>>")
    private val keyStoreFile = File(Utils.getKeystoreDir(applicationContext), "ktor-keystore.bks")
    private val keyAlias = ""
    private val privateKeyPassword = ""
    private val keyStorePassword = ""

    private val assetsPathHandler: CustomAssetsPathHandler = CustomAssetsPathHandler(applicationContext)

    private val keyStore = KeyStore.getInstance(KeyStore.getDefaultType()).also {keyStore->
        keyStoreFile.inputStream().use { keyStore.load(it, keyStorePassword.toCharArray()) }
    }

    private val environment = applicationEngineEnvironment {
        log = LoggerImpl("ktor-app")
        sslConnector(
            keyStore = keyStore,
            keyAlias = keyAlias,
            keyStorePassword = { keyStorePassword.toCharArray() },
            privateKeyPassword = { privateKeyPassword.toCharArray() }) {
            port = 8443
            keyStorePath = keyStoreFile
        }
        module {
            routing {
                get("/{...}") {
                    val path = call.request.path()
                    logger.debug("call.request.path() = '$path'")
                    if ("/" == path || path.startsWith("/css/") || path.startsWith("/js/")) {
                        withContext(Dispatchers.IO) {
                            val response = assetsPathHandler.handle(if ("/" == path) "index.html" else path)!!
                            call.respondOutputStream(contentType = ContentType.parse(response.mimeType), status = HttpStatusCode.OK) { ->
                                response.data.use { it.copyTo(this) }
                            }
                        }
                    } else {
                        call.respondText("Hello, world!")
                    }
                }
            }
        }
    }

    private val httpsServer = embeddedServer(Netty, environment).start(wait = false)

    private fun generateCertificate() {
        generateCertificate(
            file = File(".../android/key-stores/ktor-keystore.jks"),
            keyAlias = "",
            keyPassword = "",
            jksPassword = ""
        )
    }
}
