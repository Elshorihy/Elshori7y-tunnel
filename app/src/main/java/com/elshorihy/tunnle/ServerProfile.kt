package com.elshorihy.tunnle

data class ServerProfile(
    val id: String,
    val name: String,
    val country: String,
    val host: String,
    val port: Int,
    val protocol: String,
    val note: String = "",
    val wireGuardConfig: String? = null
)
