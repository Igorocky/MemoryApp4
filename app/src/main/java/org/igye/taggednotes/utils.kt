package org.igye.taggednotes

object Utils {
    fun <E> isEmpty(col: Collection<E>?): Boolean = col?.isEmpty()?:true
    fun <E> isNotEmpty(col: Collection<E>?): Boolean = !isEmpty(col)
}