package org.igye.taggednotes

import android.content.Context
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import java.io.File
import java.net.InetAddress
import java.net.NetworkInterface
import java.util.*
import kotlin.collections.HashMap

object Utils {
    private val gson = Gson()

    fun <E> isEmpty(col: Collection<E>?): Boolean = col?.isEmpty()?:true
    fun <E> isNotEmpty(col: Collection<E>?): Boolean = !isEmpty(col)
    fun getBackupsDir(context: Context): File = createDirIfNotExists(File(context.filesDir, "backup"))
    fun getKeystoreDir(context: Context): File = createDirIfNotExists(File(context.filesDir, "keystore"))
    fun <T> strToObj(str:String, classOfT: Class<T>): T = gson.fromJson(str, classOfT)
    fun objToStr(obj:Any): String = gson.toJson(obj)

    fun createMethodMap(jsInterfaces: List<Any>): Map<String, (defaultDispatcher:CoroutineDispatcher, String) -> Deferred<String>> {
        val resultMap = HashMap<String, (defaultDispatched:CoroutineDispatcher, String) -> Deferred<String>>()
        jsInterfaces.forEach{ jsInterface ->
            jsInterface.javaClass.methods.asSequence()
                .filter { it.getAnnotation(BeMethod::class.java) != null }
                .forEach { method ->
                    if (resultMap.containsKey(method.name)) {
                        throw TaggedNotesException("resultMap.containsKey('${method.name}')")
                    } else {
                        resultMap.put(method.name) { defaultDispatcher,argStr ->
                            CoroutineScope(defaultDispatcher).async {
                                var deferred: Deferred<*>? = null
                                val parameterTypes = method.parameterTypes
                                if (parameterTypes.isNotEmpty()) {
                                    val argsDto = gson.fromJson(argStr, parameterTypes[0])
                                    deferred = method.invoke(jsInterface, argsDto) as Deferred<*>
                                } else {
                                    deferred = method.invoke(jsInterface) as Deferred<*>
                                }
                                gson.toJson(deferred.await())
                            }
                        }
                    }
                }
        }
        return resultMap.toMap()
    }

    private fun createDirIfNotExists(dir: File): File {
        if (!dir.exists()) {
            dir.mkdir()
        }
        return dir
    }

    fun getIpAddress(useIPv4: Boolean): String? {
        val interfaces: List<NetworkInterface> = Collections.list(NetworkInterface.getNetworkInterfaces())
        for (intf in interfaces) {
            val addrs: List<InetAddress> = Collections.list(intf.getInetAddresses())
            for (addr in addrs) {
                if (!addr.isLoopbackAddress()) {
                    val sAddr: String = addr.getHostAddress()
                    val isIPv4 = sAddr.indexOf(':') < 0
                    if (useIPv4) {
                        if (isIPv4) return sAddr
                    } else {
                        if (!isIPv4) {
                            val delim = sAddr.indexOf('%')
                            return if (delim < 0)
                                sAddr.uppercase(Locale.getDefault())
                            else
                                sAddr.substring(0, delim).uppercase(Locale.getDefault())
                        }
                    }
                }
            }
        }
        return "???.???.???.???"
    }
}