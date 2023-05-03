package com.test_task.phone_sharing_service.controllers

import com.test_task.phone_sharing_service.dtos.BookPhoneRequest
import com.test_task.phone_sharing_service.dtos.PhoneDto
import com.test_task.phone_sharing_service.exceptions.ConcurrentPhoneModificationException
import com.test_task.phone_sharing_service.exceptions.InvalidBookerNameException
import com.test_task.phone_sharing_service.exceptions.PhoneAlreadyBookedException
import com.test_task.phone_sharing_service.exceptions.PhoneNotFoundException
import com.test_task.phone_sharing_service.services.PhoneSharingService
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.*
import java.util.*

@RestController
@RequestMapping("phones")
class PhoneSharingController(val phoneSharingService: PhoneSharingService) {

    val logger = LoggerFactory.getLogger(PhoneSharingController::class.java)

    @GetMapping
    fun phoneList(): List<PhoneDto> = phoneSharingService.phoneList()

    /**
     * No security is configured. Any booker name can be used.
     *
     * If the provided id is not found, then 404 is returned.
     * If the length of the provided name is not in [1, 255], then 422 returned.
     * If the phone is already booked by this very booker, no exception is thrown. It's considered an OK situation.
     * If the phone is already booked by another booker, then 409 is returned.
     */
    @PostMapping("book/{phoneId}")
    fun bookPhone(@PathVariable phoneId: UUID, @RequestBody request: BookPhoneRequest) =
            phoneSharingService.bookPhone(phoneId, request.bookerName.trim())

    /**
     * No security is configured. Anyone may call this method, no matter who is the current holder of the phone.
     *
     * If the provided id is not found, then 404 is returned,
     * If the phone is already returned, no exception is thrown. It's considered an OK situation.
     */
    @PostMapping("return/{phoneId}")
    fun returnPhone(@PathVariable phoneId: UUID) = phoneSharingService.returnPhone(phoneId)

    @ResponseStatus(HttpStatus.NOT_FOUND)
    @ExceptionHandler(PhoneNotFoundException::class)
    fun phoneNotFoundException(e: PhoneNotFoundException) {
        logger.error("PhoneSharingController. Returning 404", e)
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(PhoneAlreadyBookedException::class)
    fun phoneAlreadyBookedException(e: PhoneAlreadyBookedException) {
        logger.error("PhoneSharingController. Returning 409", e)
    }

    @ResponseStatus(HttpStatus.CONFLICT)
    @ExceptionHandler(ConcurrentPhoneModificationException::class)
    fun concurrentPhoneModificationException(e: ConcurrentPhoneModificationException) {
        logger.error("PhoneSharingController. Returning 409", e)
    }

    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    @ExceptionHandler(InvalidBookerNameException::class)
    fun invalidBookerNameException(e: InvalidBookerNameException) {
        logger.error("PhoneSharingController. Returning 422", e)
    }

}
