package com.mhss.app.otpreader.model

data class InstalledApp(
    val name: String,
    val packageName: String,
    val iconUri: String?,
    val included: Boolean = false
)
