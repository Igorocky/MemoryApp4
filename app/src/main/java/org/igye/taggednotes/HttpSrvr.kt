package org.igye.taggednotes

import io.ktor.network.tls.certificates.*
import java.io.File

fun main() {
    val keyStoreFile = File(".../android/key-stores/ktor-keystore.jks")
    val keystore = generateCertificate(
        file = keyStoreFile,
        keyAlias = "",
        keyPassword = "",
        jksPassword = ""
    )
}