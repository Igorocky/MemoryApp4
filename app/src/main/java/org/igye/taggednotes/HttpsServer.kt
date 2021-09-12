package org.igye.taggednotes

import android.content.Context
import android.webkit.JavascriptInterface
import com.google.gson.Gson
import io.ktor.application.*
import io.ktor.http.*
import io.ktor.network.tls.certificates.*
import io.ktor.request.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.File
import java.security.KeyStore

class HttpsServer(applicationContext: Context, javascriptInterface: Any) {
    private val logger = LoggerImpl(">>>>>")
    protected val gson = Gson()
    private val keyStoreFile = File(Utils.getKeystoreDir(applicationContext), "ktor-keystore.bks")
    private val keyAlias = "key0"
    private val privateKeyPassword = ""
    private val keyStorePassword = ""

    private val beFuncs = createMethodMap(javascriptInterface)

    private val assetsPathHandler: CustomAssetsPathHandler = CustomAssetsPathHandler(
        appContext = applicationContext,
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
                            call.respondOutputStream(contentType = ContentType.parse(response.mimeType), status = HttpStatusCode.OK) {
                                response.data.use { it.copyTo(this) }
                            }
                        }
                    } else {
                        call.respondText("Hello, world!")
                    }
                }
                post("/be/{funcName}") {
                    val funcName = call.parameters["funcName"]
                    logger.debug("func name = $funcName")
                    withContext(Dispatchers.Default) {
                        call.respondText(contentType = ContentType.Application.Json, status = HttpStatusCode.OK) {
                            beFuncs.get(funcName)?.invoke(call.receiveText())?:"backend method '$funcName' was not found"
                        }
                    }
                }
            }
        }
    }

    private val httpsServer = embeddedServer(Netty, environment).start(wait = false)

    private fun createMethodMap(jsInterface: Any): Map<String, (String) -> String> {
        val resultMap = HashMap<String, (String) -> String>()
        jsInterface.javaClass.methods.asSequence()
            .filter { it.getAnnotation(JavascriptInterface::class.java) != null }
            .forEach { method ->
                resultMap.put(method.name) { argStr ->
                    runBlocking(Dispatchers.Default) {
                        val deffered = method.invoke(jsInterface, -1L, argStr) as Deferred<*>
                        gson.toJson(deffered.await())
                    }
                }
            }
        return resultMap
    }

    private fun generateCertificate() {
        generateCertificate(
            file = File(".../android/key-stores/ktor-keystore.jks"),
            keyAlias = "",
            keyPassword = "",
            jksPassword = ""
        )
    }
}
