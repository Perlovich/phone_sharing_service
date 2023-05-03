package com.test_task.phone_sharing_service.services

import com.test_task.phone_sharing_service.dtos.PhonoapiPhoneDto
import com.test_task.phone_sharing_service.dtos.PhonoapiRequest
import kotlinx.coroutines.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.web.client.RestTemplate

@Service
class PhoneInfoService {

    val logger = LoggerFactory.getLogger(PhoneInfoService::class.java)

    private val PHONOAPI_ENDPOINT = "https://fonoapi.freshpixl.com/v1/getdevice"

    @Value("\${offline_mode:#{false}}")
    private var offlineMode: Boolean = false

    // Phono api is unavailable since 2020. It's impossible to get a new token.
    // https://github.com/shakee93/fonoapi/commit/7b23d6ba31d168a5e532ff052ebc9a98dce8dfc7
    // So, this property in practice will always be null.
    @Value("\${phonoapi_token:#{null}}")
    private var phonoapiToken: String? = null

    // The cache is never evicted or updated.
    private val cache: MutableMap<String, PhonoapiPhoneDto> = mutableMapOf()

    private val restTemplate = RestTemplate()

    private val DEFAULT_PHONE_INFO = PhonoapiPhoneDto()

    private val mutex = Mutex()

    private val requestsInProgress: MutableMap<String, Deferred<PhonoapiPhoneDto>> = mutableMapOf()

    suspend fun getInfoForName(phoneName: String): PhonoapiPhoneDto {
        val phoneDeferred = coroutineScope {
            // Before making a request to 3d party API we should check if there is no other request for the same phone.
            // If there is such a request, we should just wait until it completes.
            mutex.withLock {
                if (requestsInProgress.contains(phoneName)) {
                    requestsInProgress[phoneName]!!
                } else if (cache.containsKey(phoneName)) {
                    async { cache[phoneName]!! }
                } else {
                    async {
                        requestFonoapi(phoneName).also {
                            cache[phoneName] = it
                            requestsInProgress.remove(phoneName)
                        }
                    }.also { requestsInProgress[phoneName] = it }
                }
            }
        }
        return phoneDeferred.await()
    }

    private suspend fun requestFonoapi(phoneName: String): PhonoapiPhoneDto {
        if (phonoapiToken?.isBlank() != false) {
            logger.warn("The phonoapi token is not set. The request to their API won't be sent.")
            return DEFAULT_PHONE_INFO
        } else if (offlineMode) {
            logger.warn("The offline mode is on. The request to their API won't be sent.")
            return DEFAULT_PHONE_INFO
        }

        val phoneList: List<PhonoapiPhoneDto> = try {
            withContext(Dispatchers.IO) {
                @Suppress("UNCHECKED_CAST")
                (restTemplate.postForObject(PHONOAPI_ENDPOINT, PhonoapiRequest(phoneName, phonoapiToken!!), List::class.java) as? List<PhonoapiPhoneDto>)
                        ?: listOf()
            }
        } catch (e: Throwable) {
            logger.error("Error when trying to use PhonoApi", e)
            listOf()
        }
        return if (phoneList.isNotEmpty()) phoneList.first() else DEFAULT_PHONE_INFO
    }

}
