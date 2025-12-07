package com.reader.analytics.tracking.domain

import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import java.time.Instant
import java.util.UUID

@Entity
@Table(name = "location_changes")
data class LocationChange(
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    val documentId: String,
    val fromLocation: String?,
    val toLocation: String,
    val changedAt: Instant,
    val category: String?
)
