package com.test_task.phone_sharing_service.repositories

import com.test_task.phone_sharing_service.entities.Phone
import org.springframework.data.domain.Sort
import org.springframework.data.repository.CrudRepository
import java.util.*

interface PhoneRepository : CrudRepository<Phone, UUID> {
    fun findAll(sort: Sort): Iterable<Phone>
}
