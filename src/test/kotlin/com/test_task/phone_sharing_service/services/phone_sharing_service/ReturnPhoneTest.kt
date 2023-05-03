package com.test_task.phone_sharing_service.services.phone_sharing_service

import com.test_task.phone_sharing_service.exceptions.PhoneNotFoundException
import com.test_task.phone_sharing_service.repositories.PhoneRepository
import com.test_task.phone_sharing_service.services.PhoneSharingService
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.jdbc.core.JdbcTemplate
import java.time.LocalDateTime
import java.util.*

@SpringBootTest
class ReturnPhoneTest(
        @Autowired val phoneSharingService: PhoneSharingService,
        @Autowired val jdbcTemplate: JdbcTemplate,
        @Autowired val phoneRepository: PhoneRepository,
) {

    @AfterEach
    fun afterEach() {
        setBookersToNull(jdbcTemplate)
    }

    @Test
    fun `return a phone`() {
        val phoneId = UUID.fromString("2524f734-e876-11ed-a05b-0242ac120003")

        jdbcTemplate.update("""update PHONE set BOOKER_NAME = 'John Doe', BOOKING_TIME = '2023-05-01 18:56' 
            |where ID = ?;""".trimMargin(), phoneId)

        phoneRepository.findByIdOrNull(phoneId)!!.run {
            Assertions.assertEquals("John Doe", bookerName)
            Assertions.assertEquals(LocalDateTime.of(2023, 5, 1, 18, 56), bookingTime)
        }

        phoneSharingService.returnPhone(phoneId)

        phoneRepository.findByIdOrNull(phoneId)!!.run {
            Assertions.assertNull(bookerName)
            Assertions.assertNull(bookingTime)
        }
    }

    @Test
    fun `id not found`() {
        try {
            phoneSharingService.returnPhone(UUID.fromString("013203c4-e887-11ed-a05b-0242ac120003"))
        } catch (e: PhoneNotFoundException) {
            Assertions.assertEquals("No phone found for id 013203c4-e887-11ed-a05b-0242ac120003", e.message)
            return
        }
        fail("This line shouldn't be reached")
    }

    @Test
    fun `phone is already returned`() {
        val phoneId = UUID.fromString("2524f734-e876-11ed-a05b-0242ac120003")
        phoneSharingService.returnPhone(phoneId)
        phoneSharingService.returnPhone(phoneId)
        phoneSharingService.returnPhone(phoneId)
    }

}