package com.msp.board_service.domain

import org.hibernate.validator.constraints.Length
import org.springframework.data.mongodb.core.mapping.Document
import javax.validation.constraints.Digits
import javax.validation.constraints.NotBlank
import javax.validation.constraints.NotNull
import javax.validation.constraints.Size

/**
 * 수정 이력 Domain
 */
@Document(collection = "traces")
data class Trace(
    // 수정 버전
    @field:NotBlank(message = "version does not exist")
    @field:Digits(
        integer = Integer.MAX_VALUE,
        fraction = 0,
        message = "version does not matched")
    var version: String,

    // 제목
    @field:NotNull(message = "title does not exist")
    var title: ArrayList<@NotNull(message = "title does not exist")
    @Size(min=1, message = "title does not exist") HashMap<String,
            @Length(min=2, max=100, message="title's length must be between 2 to 100") String>>,

    // 내용
    @field:NotBlank(message = "content does not exist")
    @field:Length(max = 2000, message = "The maxinum content length is 2000")
    var content: String,

    // 수정자
    @field:NotBlank(message = "author does not exist")
    @field:Length(min=2, max=20, message="editor's length must be between 2 to 20")
    var editor: String,

    // 수정일시
    @field:NotBlank(message = "editedAt does not exist")
    var editedAt: String,

    // 글 ID
    @field:NotBlank(message = "postId does not exist")
    @field:Digits(
        integer = Integer.MAX_VALUE,
        fraction = 0,
        message = "postId does not matched")
    var postId: String
)