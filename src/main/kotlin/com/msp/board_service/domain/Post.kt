package com.msp.board_service.domain

import com.fasterxml.jackson.annotation.JsonCreator
import org.hibernate.validator.constraints.Length
import org.springframework.data.mongodb.core.mapping.Document
import javax.validation.constraints.*

/**
 * 게시글 Domain
 */
@Document(collection = "posts")
data class Post(
    // 글 ID
    @field:NotBlank(message = "postId does not exist")
    @field:Pattern(regexp = """^[1-9]+[0-9]?$""", message = "postId does not matched")
    var postId: String,

    // 카테고리
    @field:Pattern(regexp = "^C[0-9][0-9][0-9]$", message = "category dose not matched")
    var category: String,

    // 제목
    @field:NotNull(message = "title does not exist")
    var title: ArrayList<@NotNull(message = "title does not exist")
                         @Size(min=1, message = "title does not exist") HashMap<String,
            @Length(min=2, max=100, message="title's length must be between 2 to 100") String>>,

    // 내용
    @field:NotBlank(message = "content does not exist")
    @field:Length(max = 2000, message = "The maxinum content length is 2000")
    var content: String,

    // 작성자
    @field:NotBlank(message = "author does not exist")
    @field:Length(min=2, max=20, message = "author's length must be between 2 to 20")
    var author: String,

    // 생성 시간
    @field:NotBlank(message = "createdAt does not exist")
    @field:Pattern(regexp = """^[1-9]+[0-9]?$""", message = "createdAt does not matched")
    var createdAt: String,

    // 수정 시간
    @field:NotBlank(message = "updatedAt does not exist")
    @field:Pattern(regexp = """^[1-9]+[0-9]?$""", message = "updatedAt does not matched")
    var updatedAt: String,

    // 노출 시간
    @field:NotBlank(message = "showedAt does not exist")
    @field:Pattern(regexp = """^[1-9]+[0-9]?$""", message = "showedAt does not matched")
    var showedAt: String,

    // 삭제 여부
    @field:NotBlank(message = "delYn does not exist")
    @field:AssertTrue(message = "delYn does not matched")
    @field:AssertFalse(message = "delYn does not matched")
    var delYn: Boolean
)