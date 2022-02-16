package com.msp.board_service.domain.request

import org.hibernate.validator.constraints.ConstraintComposition
import org.hibernate.validator.constraints.Length
import java.sql.Timestamp
import java.time.LocalDateTime
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
    @get:NotNull(message = "category does not exist")
    var category: String,

    // 제목
    @get:NotNull(message = "title does not exist")
    var title: ArrayList<@NotNull(message = "title does not exist")
    @Size(min=1, message = "title does not exist") HashMap<String,
            @Length(min=2, max=100, message="title's length must be between 2 to 100") String>>,

    // 내용
    @get:NotBlank(message = "content is empty")
    @get:NotNull(message = "content does not exist")
    var content: String,

    // 작성자
    @get:NotBlank(message = "author is empty")
    @get:NotNull(message = "author does not exist")
    var author: String,

    // 노출 시간
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