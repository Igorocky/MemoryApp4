package org.igye.memoryapp4

data class BeErr(val code:Int, val msg: String)
data class BeRespose<T>(val data: T? = null, val err: BeErr? = null)
data class Tag(val id:Long, val createdAt:Long, val name:String)
data class Note(val id:Long, val createdAt:Long, val text:String, val tagIds: List<Long>)