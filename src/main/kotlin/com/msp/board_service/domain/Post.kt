package com.msp.board_service.domain

import org.springframework.data.mongodb.core.mapping.Document
import java.sql.Timestamp
import java.time.LocalDateTime

/**
 * 게시글 Domain
 */
@Document(collection = "posts")
data class Post(
    var postId: String,                                            // 글 ID
    var category: String,                                          // 카테고리
    var title: ArrayList<HashMap<String, String>>,                 // 제목
    var content: String,                                           // 내용
    var author: String,                                            // 작성자
    var createdAt: String
        = Timestamp.valueOf(LocalDateTime.now()).time.toString(),  // 생성 시간
    var updatedAt: String? = null,                                 // 수정 시간
    var showedAt: String = createdAt,                              // 노출 시간
    var delYn: Boolean? = false                                    // 삭제 여부
)