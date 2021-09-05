package org.igye.memoryapp4

data class BeErr(val code:Int, val msg: String)
data class BeRespose<T>(val data: T? = null, val err: BeErr? = null)
data class ListOfItems<T>(val partialResult: Boolean, val items: List<T>)
data class Tag(val id:Long, val createdAt:Long, val name:String)
data class Note(val id:Long, val createdAt:Long, val isDeleted:Boolean = false, val text:String, val tagIds: List<Long>)
data class Backup(val name: String, val size: Long)