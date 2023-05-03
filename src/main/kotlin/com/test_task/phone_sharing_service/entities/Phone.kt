package com.test_task.phone_sharing_service.entities

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.*

@Entity
data class Phone(
        @Id
        @GeneratedValue(strategy = GenerationType.UUID)
        var id: UUID,
        @Column(nullable = false)
        var name: String,
        @Column
        var bookerName: String?,
        @Column
        var bookingTime: LocalDateTime?,
        @Version
        @Column(columnDefinition = "integer default 1")
        var version: Long,
)
