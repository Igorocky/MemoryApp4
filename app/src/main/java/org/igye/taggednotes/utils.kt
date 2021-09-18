package org.igye.taggednotes

import android.content.Context
import com.google.gson.Gson
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import java.io.File

object Utils {
    private val gson = Gson()

    fun <E> isEmpty(col: Collection<E>?): Boolean = col?.isEmpty()?:true
    fun <E> isNotEmpty(col: Collection<E>?): Boolean = !isEmpty(col)
    fun getBackupsDir(context: Context): File = createDirIfNotExists(File(context.filesDir, "backup"))
    fun getKeystoreDir(context: Context): File = createDirIfNotExists(File(context.filesDir, "keystore"))

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
}