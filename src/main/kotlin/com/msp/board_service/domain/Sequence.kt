package com.msp.board_service.domain

import org.springframework.data.annotation.Id
import org.springframework.data.mongodb.core.mapping.Document

/**
 * sequence id Domain
 *
 *  id에 따라 sequence를 매김
 */
@Document(collection = "sequencesEom")
data class Sequence (
    @Id var id: String,
    var seq: Long
)