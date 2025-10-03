package com.example.evcharger.model

/**
 * Represents a locally stored EV Owner user.
 * NIC is the primary key.
 */
data class User(
    val nic: String,
    var fullName: String,
    var email: String,
    var phone: String,
    var isActive: Boolean = true
)