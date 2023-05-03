package com.test_task.phone_sharing_service.services

import com.test_task.phone_sharing_service.dtos.PhoneDto
import com.test_task.phone_sharing_service.dtos.FonoapiPhoneDto
import com.test_task.phone_sharing_service.entities.Phone
import com.test_task.phone_sharing_service.exceptions.ConcurrentPhoneModificationException
import com.test_task.phone_sharing_service.exceptions.InvalidBookerNameException
import com.test_task.phone_sharing_service.exceptions.PhoneAlreadyBookedException
import com.test_task.phone_sharing_service.exceptions.PhoneNotFoundException
import com.test_task.phone_sharing_service.repositories.PhoneRepository
import jakarta.persistence.OptimisticLockException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Sort
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import java.time.LocalDateTime
import java.util.*

@Service
class PhoneSharingService(
        val dateTimeService: DateTimeService,
        val phoneInfoService: PhoneInfoService,
        val phoneRepository: PhoneRepository,
) {

    val logger = LoggerFactory.getLogger(PhoneSharingService::class.java)

    fun phoneList(): List<PhoneDto> {
        val phoneEntityList = phoneRepository
                .findAll(Sort.by(listOf(Sort.Order.asc("name").ignoreCase(), Sort.Order.asc("id"))))

        return runBlocking {
             phoneEntityList
                    .map { phone ->
                        async { phoneEntityAndPhoneInfoToPhoneDto(phone, phoneInfoService.getInfoForName(phone.name)) }
                    }
                    .awaitAll()
        }
    }

    fun bookPhone(phoneId: UUID, bookerName: String) {
        validateBookerName(bookerName)
        val phone: Phone = findPhone(phoneId)
        when (phone.bookerName) {
            null -> {
                logger.trace("bookPhone: no problem encountered, proceeding to book phone $phoneId by $bookerName")
                setBookerInfoAndSavePhone(phone, bookerName, dateTimeService.now())
            }
            bookerName -> logger.trace("bookPhone: the phone $phoneId is already booked by $bookerName, no need to do anything")
            else -> throw PhoneAlreadyBookedException("The phone is already booked by ${phone.bookerName}")
        }
    }

    fun returnPhone(phoneId: UUID) {
        val phone: Phone = findPhone(phoneId)
        // if bookerName is null, then there is no need to do anything, the phone is not booked
        if (phone.bookerName != null) {
            logger.trace("returnPhone: starting the return of the phone $phoneId")
            setBookerInfoAndSavePhone(phone, bookerName = null, bookingTime = null)
        } else {
            logger.trace("returnPhone: the phone $phoneId is already returned, no action required")
        }
    }

    private fun findPhone(phoneId: UUID): Phone =
            phoneRepository.findByIdOrNull(phoneId) ?: throw PhoneNotFoundException("No phone found for id $phoneId")

    private fun setBookerInfoAndSavePhone(phone: Phone, bookerName: String?, bookingTime: LocalDateTime?) {
        phone.bookerName = bookerName
        phone.bookingTime = bookingTime
        try {
            phoneRepository.save(phone)
        } catch (e: OptimisticLockException) {
            throw ConcurrentPhoneModificationException("The phone was modified by someone else at the same time")
        }
    }

    private fun phoneEntityAndPhoneInfoToPhoneDto(phone: Phone, phoneInfo: FonoapiPhoneDto): PhoneDto = PhoneDto(
            phone.id,
            phone.name,
            phone.bookerName == null,
            phone.bookerName,
            phone.bookingTime,
            phoneInfo.technology,
            phoneInfo._2g_bands,
            phoneInfo._3g_bands,
            phoneInfo._4g_bands,
    )

    private fun validateBookerName(bookerName: String) {
        if (bookerName.isBlank() || bookerName.length > 255) {
            throw InvalidBookerNameException("param bookerName is invalid: $bookerName")
        }
    }

}
