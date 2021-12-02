package com.msp.board_service.domain.request

/**
 * 글 생성 요청 Domain
 */
data class PostCreateRequest(
    var category: String?,                           // 카테고리
    var title: ArrayList<HashMap<String, String>>?,  // 제목
    var content: String?,                            // 내용
    var author: String?,                             // 작성자
    var showedAt: String?                            // 노출 시간
)

/**
 * 글 수정 요청 Domain
 */
data class PostUpdateRequest(
    var title: ArrayList<HashMap<String, String>>?, // 제목
    var content: String?,                           // 내용
    var author: String?,                             // 작성자
    var delYn: Boolean?                             // 삭제 여부
)