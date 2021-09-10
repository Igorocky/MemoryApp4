package org.igye.taggednotes

import android.os.Bundle
import android.util.Log
import android.view.ViewGroup
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.gson.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import org.slf4j.Logger
import org.slf4j.Marker
import java.io.File
import java.security.KeyStore

class MainActivity : AppCompatActivity() {
    private val viewModel: MainActivityViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.d(Constants.LOG_TAG, "Starting WebViewActivity")
        super.onCreate(savedInstanceState)
        // TODO: 8/25/2021 log warning if supportActionBar == null
        supportActionBar?.hide()

        setContentView(viewModel.getWebView(applicationContext))

        val keyStoreFile = File(applicationContext.filesDir, "ktor-keystore.bks")
        val keyAlias = ""
        val privateKeyPassword = ""
        val keyStorePassword = ""

        val keyStore = KeyStore.getInstance(KeyStore.getDefaultType())
        keyStoreFile.inputStream().use { keyStore.load(it, keyStorePassword.toCharArray()) }

        val environment = applicationEngineEnvironment {
            log = logger
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


        val httpServer = embeddedServer(Netty, 8080) {
            install(ContentNegotiation) {
                gson {}
            }
            routing {
                get("/") {
                    call.respond(mapOf("message" to "Hello world"))
                }
            }
        }
        val httpsServer = embeddedServer(Netty, environment)
//        httpServer.start(wait = false)
//        httpsServer.start(wait = false)
    }

    override fun onDestroy() {
        val webView = viewModel.getWebView(null)
        (webView.parent as ViewGroup).removeView(webView)
        super.onDestroy()
    }

    val logger = object : Logger {
        val defaultFormat = "%s"
        override fun getName(): String {
            return "logger12345"
        }

        override fun isTraceEnabled(): Boolean {
            return true
        }

        override fun isTraceEnabled(marker: Marker?): Boolean {
            return true
        }

        override fun trace(msg: String?) {
            Log.v(Constants.LOG_TAG, msg?:"")
        }

        override fun trace(format: String?, arg: Any?) {
            Log.v(Constants.LOG_TAG, String.format(format?: defaultFormat, arg))
        }

        override fun trace(format: String?, arg1: Any?, arg2: Any?) {
            Log.v(Constants.LOG_TAG, String.format(format?:defaultFormat, arg1, arg2))
        }

        override fun trace(format: String?, vararg arguments: Any?) {
            Log.v(Constants.LOG_TAG, String.format(format?:defaultFormat, *arguments))
        }

        override fun trace(msg: String?, t: Throwable?) {
            Log.v(Constants.LOG_TAG, msg, t)
        }

        override fun trace(marker: Marker?, msg: String?) {
            trace(msg)
        }

        override fun trace(marker: Marker?, format: String?, arg: Any?) {
            trace(format, arg)
        }

        override fun trace(marker: Marker?, format: String?, arg1: Any?, arg2: Any?) {
            trace(format, arg1, arg2)
        }

        override fun trace(marker: Marker?, format: String?, vararg argArray: Any?) {
            trace(format, *argArray)
        }

        override fun trace(marker: Marker?, msg: String?, t: Throwable?) {
            trace(msg, t)
        }

        override fun isDebugEnabled(): Boolean {
            return true
        }

        override fun isDebugEnabled(marker: Marker?): Boolean {
            return true
        }

        override fun debug(msg: String?) {
            Log.d(Constants.LOG_TAG, msg?:"")
        }

        override fun debug(format: String?, arg: Any?) {
            Log.d(Constants.LOG_TAG, String.format(format?: defaultFormat, arg))
        }

        override fun debug(format: String?, arg1: Any?, arg2: Any?) {
            Log.d(Constants.LOG_TAG, String.format(format?:defaultFormat, arg1, arg2))
        }

        override fun debug(format: String?, vararg arguments: Any?) {
            Log.d(Constants.LOG_TAG, String.format(format?:defaultFormat, *arguments))
        }

        override fun debug(msg: String?, t: Throwable?) {
            Log.d(Constants.LOG_TAG, msg, t)
        }

        override fun debug(marker: Marker?, msg: String?) {
            debug(msg)
        }

        override fun debug(marker: Marker?, format: String?, arg: Any?) {
            debug(format, arg)
        }

        override fun debug(marker: Marker?, format: String?, arg1: Any?, arg2: Any?) {
            debug(format, arg1, arg2)
        }

        override fun debug(marker: Marker?, format: String?, vararg arguments: Any?) {
            debug(format, *arguments)
        }

        override fun debug(marker: Marker?, msg: String?, t: Throwable?) {
            debug(msg, t)
        }

        override fun isInfoEnabled(): Boolean {
            return true
        }

        override fun isInfoEnabled(marker: Marker?): Boolean {
            return true
        }

        override fun info(msg: String?) {
            Log.i(Constants.LOG_TAG, msg?:"")
        }

        override fun info(format: String?, arg: Any?) {
            Log.i(Constants.LOG_TAG, String.format(format?: defaultFormat, arg))
        }

        override fun info(format: String?, arg1: Any?, arg2: Any?) {
            Log.i(Constants.LOG_TAG, String.format(format?:defaultFormat, arg1, arg2))
        }

        override fun info(format: String?, vararg arguments: Any?) {
            Log.i(Constants.LOG_TAG, String.format(format?:defaultFormat, *arguments))
        }

        override fun info(msg: String?, t: Throwable?) {
            Log.i(Constants.LOG_TAG, msg, t)
        }

        override fun info(marker: Marker?, msg: String?) {
            info(msg)
        }

        override fun info(marker: Marker?, format: String?, arg: Any?) {
            info(format, arg)
        }

        override fun info(marker: Marker?, format: String?, arg1: Any?, arg2: Any?) {
            info(format, arg1, arg2)
        }

        override fun info(marker: Marker?, format: String?, vararg arguments: Any?) {
            info(format, *arguments)
        }

        override fun info(marker: Marker?, msg: String?, t: Throwable?) {
            info(msg, t)
        }

        override fun isWarnEnabled(): Boolean {
            return true
        }

        override fun isWarnEnabled(marker: Marker?): Boolean {
            return true
        }

        override fun warn(msg: String?) {
            Log.w(Constants.LOG_TAG, msg?:"")
        }

        override fun warn(format: String?, arg: Any?) {
            Log.w(Constants.LOG_TAG, String.format(format?: defaultFormat, arg))
        }

        override fun warn(format: String?, vararg arguments: Any?) {
            Log.w(Constants.LOG_TAG, String.format(format?:defaultFormat, *arguments))
        }

        override fun warn(format: String?, arg1: Any?, arg2: Any?) {
            Log.w(Constants.LOG_TAG, String.format(format?:defaultFormat, arg1, arg2))
        }

        override fun warn(msg: String?, t: Throwable?) {
            Log.w(Constants.LOG_TAG, msg, t)
        }

        override fun warn(marker: Marker?, msg: String?) {
            warn(msg)
        }

        override fun warn(marker: Marker?, format: String?, arg: Any?) {
            warn(format, arg)
        }

        override fun warn(marker: Marker?, format: String?, arg1: Any?, arg2: Any?) {
            warn(format, arg1, arg2)
        }

        override fun warn(marker: Marker?, format: String?, vararg arguments: Any?) {
            warn(format, *arguments)
        }

        override fun warn(marker: Marker?, msg: String?, t: Throwable?) {
            warn(msg, t)
        }

        override fun isErrorEnabled(): Boolean {
            return true
        }

        override fun isErrorEnabled(marker: Marker?): Boolean {
            return true
        }

        override fun error(msg: String?) {
            Log.e(Constants.LOG_TAG, msg?:"")
        }

        override fun error(format: String?, arg: Any?) {
            Log.e(Constants.LOG_TAG, String.format(format?: defaultFormat, arg))
        }

        override fun error(format: String?, arg1: Any?, arg2: Any?) {
            Log.e(Constants.LOG_TAG, String.format(format?:defaultFormat, arg1, arg2))
        }

        override fun error(format: String?, vararg arguments: Any?) {
            Log.e(Constants.LOG_TAG, String.format(format?:defaultFormat, *arguments))
        }

        override fun error(msg: String?, t: Throwable?) {
            Log.e(Constants.LOG_TAG, msg, t)
        }

        override fun error(marker: Marker?, msg: String?) {
            error(msg)
        }

        override fun error(marker: Marker?, format: String?, arg: Any?) {
            error(format, arg)
        }

        override fun error(marker: Marker?, format: String?, arg1: Any?, arg2: Any?) {
            error(format, arg1, arg2)
        }

        override fun error(marker: Marker?, format: String?, vararg arguments: Any?) {
            error(format, *arguments)
        }

        override fun error(marker: Marker?, msg: String?, t: Throwable?) {
            error(msg, t)
        }
    }

}

