package com.test_task.phone_sharing_service.services

import com.test_task.phone_sharing_service.dtos.FonoapiPhoneDto
import com.test_task.phone_sharing_service.dtos.FonoapiRequest
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

    private val FONOAPI_ENDPOINT = "https://fonoapi.freshpixl.com/v1/getdevice"

    @Value("\${offline_mode:#{false}}")
    private var offlineMode: Boolean = false

    // Fonoapi is unavailable since 2020. It's impossible to get a new token.
    // https://github.com/shakee93/fonoapi/commit/7b23d6ba31d168a5e532ff052ebc9a98dce8dfc7
    // So, this property in practice will always be null.
    @Value("\${fonoapi_token:#{null}}")
    private var fonoapiToken: String? = null

    // The cache is never evicted or updated.
    private val cache: MutableMap<String, Deferred<FonoapiPhoneDto>> = mutableMapOf()

    private val restTemplate = RestTemplate()

    private val DEFAULT_PHONE_INFO = FonoapiPhoneDto()

    private val mutex = Mutex()

    suspend fun getInfoForName(phoneName: String): FonoapiPhoneDto {
        val phoneDeferred = coroutineScope {
            mutex.withLock {
                if (cache.containsKey(phoneName)) {
                    cache[phoneName]!!
                } else {
                    async {
                        requestFonoapi(phoneName)
                    }.also { cache[phoneName] = it }
                }
            }
        }
        return phoneDeferred.await()
    }

    private suspend fun requestFonoapi(phoneName: String): FonoapiPhoneDto {
        if (fonoapiToken?.isBlank() != false) {
            logger.warn("The fonoapi token is not set. The request to their API won't be sent.")
            return DEFAULT_PHONE_INFO
        } else if (offlineMode) {
            logger.warn("The offline mode is on. The request to their API won't be sent.")
            return DEFAULT_PHONE_INFO
        }

        val phoneList: List<FonoapiPhoneDto> = try {
            withContext(Dispatchers.IO) {
                @Suppress("UNCHECKED_CAST")
                (restTemplate.postForObject(FONOAPI_ENDPOINT, FonoapiRequest(phoneName, fonoapiToken!!), List::class.java) as? List<FonoapiPhoneDto>)
                        ?: listOf()
            }
        } catch (e: Throwable) {
            logger.error("Error when trying to use Fonoapi", e)
            listOf()
        }
        return if (phoneList.isNotEmpty()) phoneList.first() else DEFAULT_PHONE_INFO
    }

}
