package com.test_task.phone_sharing_service.services.phone_sharing_service

import com.test_task.phone_sharing_service.exceptions.InvalidBookerNameException
import com.test_task.phone_sharing_service.exceptions.PhoneAlreadyBookedException
import com.test_task.phone_sharing_service.exceptions.PhoneNotFoundException
import com.test_task.phone_sharing_service.repositories.PhoneRepository
import com.test_task.phone_sharing_service.services.DateTimeService
import com.test_task.phone_sharing_service.services.PhoneInfoService
import com.test_task.phone_sharing_service.services.PhoneSharingService
import org.junit.jupiter.api.*
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.repository.findByIdOrNull
import org.springframework.jdbc.core.JdbcTemplate
import java.time.LocalDateTime
import java.util.*


@SpringBootTest
class BookPhoneTest(
        @Autowired val jdbcTemplate: JdbcTemplate,
        @Autowired val phoneInfoService: PhoneInfoService,
        @Autowired val phoneRepository: PhoneRepository,
) {

    val dateTimeService: DateTimeService = Mockito.mock(DateTimeService::class.java)
    val phoneSharingService: PhoneSharingService = PhoneSharingService(dateTimeService, phoneInfoService, phoneRepository)

    @BeforeEach
    fun beforeEach() {
        Mockito.`when`(dateTimeService.now()).thenReturn(LocalDateTime.of(2023, 5, 2, 10, 40))
    }

    @AfterEach
    fun afterEach() {
        setBookersToNull(jdbcTemplate)
    }

    @Test
    fun `book a phone`() {
        val phoneId = UUID.fromString("2524f734-e876-11ed-a05b-0242ac120003")

        phoneRepository.findByIdOrNull(phoneId)!!.run {
            Assertions.assertNull(bookerName)
            Assertions.assertNull(bookingTime)
        }

        phoneSharingService.bookPhone(phoneId, "John Doe")

        phoneRepository.findByIdOrNull(phoneId)!!.run {
            Assertions.assertEquals("John Doe", bookerName)
            Assertions.assertEquals(LocalDateTime.of(2023, 5, 2, 10, 40), bookingTime)
        }
    }

    @Test()
    fun `id not found`() {
        try {
            phoneSharingService.bookPhone(UUID.fromString("013203c4-e887-11ed-a05b-0242ac120003"), "John Doe")
        } catch (e: PhoneNotFoundException) {
            Assertions.assertEquals("No phone found for id 013203c4-e887-11ed-a05b-0242ac120003", e.message)
            return
        }
        fail("This line shouldn't be reached")
    }

    @Test
    fun `phone is already booked`() {
        phoneSharingService.bookPhone(UUID.fromString("2524f734-e876-11ed-a05b-0242ac120003"), "John Doe")
        try {
            phoneSharingService.bookPhone(UUID.fromString("2524f734-e876-11ed-a05b-0242ac120003"), "Jack Black")
        } catch (e: PhoneAlreadyBookedException) {
            Assertions.assertEquals("The phone is already booked by John Doe", e.message)
            return
        }
        fail("This line shouldn't be reached")
    }

    @Test
    fun `phone is already booked by the same person`() {
        phoneSharingService.bookPhone(UUID.fromString("2524f734-e876-11ed-a05b-0242ac120003"), "John Doe")
        phoneSharingService.bookPhone(UUID.fromString("2524f734-e876-11ed-a05b-0242ac120003"), "John Doe")
        phoneSharingService.bookPhone(UUID.fromString("2524f734-e876-11ed-a05b-0242ac120003"), "John Doe")
    }

    @Test
    fun `booker name validation`() {
        val phoneId = UUID.fromString("2524f734-e876-11ed-a05b-0242ac120003")

        val firstErrorThrown = try {
            phoneSharingService.bookPhone(phoneId, generateStringForSize(256))
            false
        } catch (e: InvalidBookerNameException) {
            true
        }

        Assertions.assertTrue(firstErrorThrown)

        val secondErrorThrown = try {
            phoneSharingService.bookPhone(phoneId, "")
            false
        } catch (e: InvalidBookerNameException) {
            true
        }

        Assertions.assertTrue(secondErrorThrown)

        // 255 is the max allowed length for a booker name
        phoneSharingService.bookPhone(phoneId, generateStringForSize(255))

        phoneRepository.findByIdOrNull(phoneId)!!.run {
            Assertions.assertEquals(255, bookerName!!.length)
            Assertions.assertEquals(LocalDateTime.of(2023, 5, 2, 10, 40), bookingTime)
        }

    }

    private fun generateStringForSize(size: Int): String = "".padStart(size, 'a')

}