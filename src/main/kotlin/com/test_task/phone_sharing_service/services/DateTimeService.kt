package com.test_task.phone_sharing_service.services

import org.springframework.stereotype.Service
import java.time.LocalDateTime

@Service
class DateTimeService {

    fun now(): LocalDateTime = LocalDateTime.now()

}