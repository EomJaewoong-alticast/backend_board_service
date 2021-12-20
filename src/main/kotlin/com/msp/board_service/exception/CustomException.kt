package com.msp.board_service.exception

import javax.validation.Path

class CustomException: Exception {
    var errorCode = 0

    companion object {
        /**
         * 4001: offset값이 잘못된 경우
         * 4002: count값이 잘못된 경우
         * 4003: q값이 잘못된 경우
         * 4004: 값이 비어있거나 잘못 된 경우
         * 4005: 노출 시간이 등록 시점보다 이전일 때
         * 4006: 이미 삭제된 글을 다시 삭제하려 할 때
         * 4007: 아무것도 수정되지 않았을 때
         * 4008: 존재하지 않는 Post
         * 4009: 존재하지 않는 Trace
         * 4010: 시작, 끝 기간이 잘못된 경우
         */
        fun OffsetInvalid() = CustomException("Offset value is missing or invalid", 4001)
        fun CountInvalid() = CustomException("Count value is missing or invalid", 4002)
        fun QueryNotMatched(q: String) = CustomException("q doesn't match condition - q: $q", 4003)
        fun InValidValue(input: Any?, field: Path, msg: String) = CustomException("Invalid Value - input value: ${input}, field: ${field}, message: ${msg}", 4004)
        fun IncorrectExposureTime() = CustomException("Incorrect exposure time", 4005)
        fun AlreadyDeleted(postId: String) = CustomException("Post[${postId}] is already deleted", 4006)
        fun NotModified(postId: String) = CustomException("post[${postId}] is not modified", 4007)
        fun NoPost(postId: String) = CustomException("Post[${postId}] dose not exist", 4008)
        fun NoTrace(postId: String, version: String) = CustomException("Post[${postId}], Trace[${version}] dose not exist", 4009)
        fun InvalidDate() = CustomException("Invalid date", 4010)
    }

    constructor(message: String?) : super(message) {}

    constructor(errorCode: Int): super() {
        this.errorCode = errorCode
    }

    constructor(message: String?, errorCode: Int): super(message) {
        this.errorCode = errorCode
    }
}