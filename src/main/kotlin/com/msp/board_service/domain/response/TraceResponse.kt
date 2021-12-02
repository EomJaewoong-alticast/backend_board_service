package com.msp.board_service.domain.response

/**
 * 수정이력 목록 응답 Domain
 */
data class TraceListResponse(
    var postId: String,         // 글 ID
    var total: Long,            // 수정 이력 갯수
    var traces: Any?            // 수정 이력
)

/**
 * 수정이력 목록에 들어가는 이력
 */
data class ListInTraceResponse(
    var version: String,                            // 수정 이력
    var title: ArrayList<HashMap<String, String>>,  // 제목
    var editor: String,                             // 이 버전의 작성자
    var editedAt: String,                           // 수정된 시간
)

/**
 * 수정 이력 응답 Domain
 */
data class TraceVersionResponse(
    var postId: String,         // 글 ID
    var version: String,        // 수정 이력 버전
)