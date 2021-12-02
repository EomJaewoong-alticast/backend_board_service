package com.msp.board_service.domain

import org.bson.types.ObjectId
import org.springframework.data.mongodb.core.mapping.Document

/**
 * 수정 이력 Domain
 */
@Document(collection = "traces")
data class Trace(
    var version: String,                            // 수정 버전
    var title: ArrayList<HashMap<String, String>>,  // 제목
    var content: String,                            // 내용
    var editor: String,                             // 수정자
    var editedAt: String,                             // 수정일시
    var postId: String                              // 글 ID
)