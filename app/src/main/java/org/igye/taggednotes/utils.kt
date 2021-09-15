package org.igye.taggednotes

import android.content.Context
import com.google.gson.Gson
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import java.io.File

object Utils {
    private val gson = Gson()

    fun <E> isEmpty(col: Collection<E>?): Boolean = col?.isEmpty()?:true
    fun <E> isNotEmpty(col: Collection<E>?): Boolean = !isEmpty(col)
    fun getBackupsDir(context: Context): File = createDirIfNotExists(File(context.filesDir, "backup"))
    fun getKeystoreDir(context: Context): File = createDirIfNotExists(File(context.filesDir, "keystore"))

    fun createMethodMap(jsInterface: Any): Map<String, (String) -> String> {
        val resultMap = HashMap<String, (String) -> String>()
        jsInterface.javaClass.methods.asSequence()
            .filter { it.getAnnotation(BeMethod::class.java) != null }
            .forEach { method ->
                resultMap.put(method.name) { argStr ->
                    runBlocking(Dispatchers.Default) {
                        var deffered: Deferred<*>? = null
                        val parameterTypes = method.parameterTypes
                        if (parameterTypes.isNotEmpty()) {
                            val argsDto = gson.fromJson(argStr, parameterTypes[0])
                            deffered = method.invoke(jsInterface, argsDto) as Deferred<*>
                        } else {
                            deffered = method.invoke(jsInterface) as Deferred<*>
                        }
                        gson.toJson(deffered.await())
                    }
                }
            }
        return resultMap
    }

    private fun createDirIfNotExists(dir: File): File {
        if (!dir.exists()) {
            dir.mkdir()
        }
        return dir
    }
}