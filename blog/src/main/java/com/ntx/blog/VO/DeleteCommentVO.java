package com.ntx.blog.VO;

import lombok.Data;

@Data
public class DeleteCommentVO {

    private Integer userId;
    private Integer commentId;
    private Integer blogId;
}
