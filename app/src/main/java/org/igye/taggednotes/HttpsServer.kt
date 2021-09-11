package org.igye.taggednotes

import android.content.Context
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.gson.*
import io.ktor.network.tls.certificates.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import java.io.File
import java.security.KeyStore

class HttpsServer(applicationContext: Context) {
    private val keyStoreFile = File(applicationContext.filesDir, "ktor-keystore.bks")
    private val keyAlias = ""
    private val privateKeyPassword = ""
    private val keyStorePassword = ""

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
            install(ContentNegotiation) {
                gson {}
            }
            routing {
                get("/") {
                    call.respond("Hello, world!")
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
