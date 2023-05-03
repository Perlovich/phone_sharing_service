package com.test_task.phone_sharing_service.services.phone_sharing_service

import com.test_task.phone_sharing_service.dtos.PhonoapiPhoneDto
import com.test_task.phone_sharing_service.services.DateTimeService
import com.test_task.phone_sharing_service.services.PhoneInfoService
import com.test_task.phone_sharing_service.services.PhoneSharingService
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import org.mockito.Mockito
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.test.context.TestPropertySource
import java.time.LocalDateTime
import java.util.*

@SpringBootTest
@TestPropertySource(properties = ["offline_mode=true"])
class PhoneListTest(
        @Autowired val phoneSharingService: PhoneSharingService,
        @Autowired val jdbcTemplate: JdbcTemplate,
) {

    @AfterEach
    fun afterEach() {
        setBookersToNull(jdbcTemplate)
    }

    @Test
    fun `check default phone list`() {
        val result = phoneSharingService.phoneList()
        Assertions.assertEquals(10, result.size)

        result[0].run {
            Assertions.assertEquals(UUID.fromString("2524f734-e876-11ed-a05b-0242ac120003"), id)
            Assertions.assertEquals("Apple iPhone 11", name)
            Assertions.assertTrue(available)
            Assertions.assertNull(bookerName)
            Assertions.assertNull(bookingTime)
            Assertions.assertEquals("<unknown>", technology)
            Assertions.assertEquals("<unknown>", _2g)
            Assertions.assertEquals("<unknown>", _3g)
            Assertions.assertEquals("<unknown>", _4g)
        }

        result[3].run {
            Assertions.assertEquals(UUID.fromString("2524f8e2-e876-11ed-a05b-0242ac120003"), id)
            Assertions.assertEquals("iPhone X", name)
            Assertions.assertTrue(available)
            Assertions.assertNull(bookerName)
            Assertions.assertNull(bookingTime)
        }

        result[7].run {
            Assertions.assertEquals(UUID.fromString("2524e910-e876-11ed-a05b-0242ac120003"), id)
            Assertions.assertEquals("Samsung Galaxy S8", name)
            Assertions.assertTrue(available)
            Assertions.assertNull(bookerName)
            Assertions.assertNull(bookingTime)
        }

        result[8].run {
            Assertions.assertEquals(UUID.fromString("2524ec4e-e876-11ed-a05b-0242ac120003"), id)
            Assertions.assertEquals("Samsung Galaxy S8", name)
            Assertions.assertTrue(available)
            Assertions.assertNull(bookerName)
            Assertions.assertNull(bookingTime)
        }

    }

    @Test
    fun `check phones that are booked`() {
        jdbcTemplate.update("""update PHONE set BOOKER_NAME = 'John Doe', BOOKING_TIME = '2023-05-01 18:56' 
            |where ID = '2524f734-e876-11ed-a05b-0242ac120003';""".trimMargin())

        val result = phoneSharingService.phoneList()

        result[0].run {
            Assertions.assertEquals(UUID.fromString("2524f734-e876-11ed-a05b-0242ac120003"), id)
            Assertions.assertEquals("Apple iPhone 11", name)
            Assertions.assertFalse(available)
            Assertions.assertEquals("John Doe", bookerName)
            Assertions.assertEquals(LocalDateTime.of(2023, 5, 1, 18, 56), bookingTime)
        }
    }

    @Test
    fun `check that info returned from phonoapi is actually used`() {
        val phoneInfoServiceMock = Mockito.mock(PhoneInfoService::class.java)
        runBlocking {
            Mockito.`when`(phoneInfoServiceMock.getInfoForName(Mockito.anyString())).thenReturn(PhonoapiPhoneDto(
                    _3g_bands = "2100 MHz"
            ))
        }
        val serviceWithMockedPhonoapi = PhoneSharingService(DateTimeService(), phoneInfoServiceMock, phoneSharingService.phoneRepository)

        val result = serviceWithMockedPhonoapi.phoneList()

        result[0].run {
            Assertions.assertEquals("<unknown>", _2g)
            Assertions.assertEquals("2100 MHz", _3g)
            Assertions.assertEquals("<unknown>", _4g)
            Assertions.assertEquals("3G", technology)
        }
    }

}