package com.test_task.phone_sharing_service.dtos

data class FonoapiPhoneDto(
        val DeviceName: String? = "<unknown>",
        val Brand: String? = "<unknown>",
        val cpu: String? = "<unknown>",
        val status: String? = "<unknown>",
        val dimensions: String? = "<unknown>",
        val _2g_bands: String? = "<unknown>",
        val _3g_bands: String? = "<unknown>",
        val _4g_bands: String? = "<unknown>",
) {
    val technology: String by lazy {
        if (_4g_bands != null && _4g_bands != "<unknown>") "4G"
        else if (_3g_bands != null && _3g_bands != "<unknown>") "3G"
        else if (_2g_bands != null && _2g_bands != "<unknown>") "2G"
        else "<unknown>"
    }
}
