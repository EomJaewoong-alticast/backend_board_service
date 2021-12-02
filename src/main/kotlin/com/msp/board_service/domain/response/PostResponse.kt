package com.msp.board_service.domain.response

import java.sql.Timestamp
import java.time.LocalDateTime

/**
 * 게시글 목록 응답 Domain
 */
data class PostListResponse(
    var total: Long,                // 글 전체 갯수
    var posts: Any?    // 게시글
)

/**
 * 게시글 목록에 들어가는 게시글 Domain
 */
data class ListInPostResponse(
    var postId: String,                             // 글 Id
    var category: String,                           // 카테고리
    var title: ArrayList<HashMap<String, String>>,  // 제목
    var author: String,                             // 작성자
    var createdAt: String,                          // 생성 시간
    var updatedAt: String?,                         // 수정 시간
    var showedAt: String                            // 노출 시간
)

/**
 * 게시글 ID 응답 Domain
 */
data class PostIdResponse(
    var postId: String      // 글 ID
)