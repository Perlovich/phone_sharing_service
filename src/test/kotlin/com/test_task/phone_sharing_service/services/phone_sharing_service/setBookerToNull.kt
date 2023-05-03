package com.test_task.phone_sharing_service.services.phone_sharing_service

import org.springframework.jdbc.core.JdbcTemplate

// Booker info is the only info that can be changed in the db by the app.
// To avoid incorrect state, tests that change this info should reset booker once they are finished.
fun setBookersToNull(jdbcTemplate: JdbcTemplate) = jdbcTemplate.update("update PHONE set BOOKER_NAME = null, BOOKING_TIME = null;")
