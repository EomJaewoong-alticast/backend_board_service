package com.msp.board_service.domain.request

import org.hibernate.validator.constraints.Length
import javax.validation.constraints.*

/**
 * 글 생성 요청 Domain
 */
data class PostCreateRequest(
    // 카테고리
    @get:Pattern(
        regexp = """^C[0-9][0-9][0-9]$""",
        message = "category is not matched"
    )
    @get:NotBlank(message = "category does not exist")
    var category: String,

    // 제목
    @get:NotNull(message = "title does not exist")
    var title: ArrayList<@NotNull(message = "title does not exist")
    @Size(min=1, message = "title does not exist") HashMap<String,
            @Length(min=2, max=100, message="title's length must be between 2 to 100") String>>,

    // 내용
    @get:NotBlank(message = "content is empty")
    @get:Length(max = 2000, message = "The maxinum content length is 2000")
    var content: String,

    // 작성자
    @get:NotBlank(message = "author is empty")
    @get:Length(min=2, max=20, message = "author's length must be between 2 to 20")
    var author: String,

    // 노출 시간
    @field:Pattern(regexp = """^[1-9]+[0-9]?$""", message = "showedAt does not matched")
    var showedAt: String?
)

/**
 * 글 수정 요청 Domain
 */
data class PostUpdateRequest(
    var title: ArrayList<HashMap<String, String>>?, // 제목
    var content: String?,                           // 내용

    // 작성자
    @get:NotBlank(message = "author is empty")
    var author: String?,

    // 삭제 여부
    @field:AssertTrue(message = "delYn does not matched")
    @field:AssertFalse(message = "delYn does not matched")
    var delYn: Boolean?
)