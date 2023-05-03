package com.test_task.phone_sharing_service.dtos

import java.time.LocalDateTime
import java.util.*

data class PhoneDto(
        val id: UUID,
        val name: String,
        val available: Boolean,
        val bookerName: String?,
        val bookingTime: LocalDateTime?,
        val technology: String,
        val _2g: String?,
        val _3g: String?,
        val _4g: String?,
)
