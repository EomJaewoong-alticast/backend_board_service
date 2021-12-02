package com.msp.board_service.domain.response

/**
 * Http Response 응답
 */
class Response(
    var code: Int,
    var message: String?,
    var result: Any?
)