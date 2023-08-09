package com.ntx.blog.dto;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
@Data
@Document(collation = "blogdto")
public class BlogDTO {
    /**
     * 博客编号
     */
    @TableId(type = IdType.AUTO)
    @MongoId
    private Integer id;

    /**
     * 博客标题
     */
    private String title;

    /**
     * 博客封面
     */
    private String image;

    /**
     * 点击阅读量
     */
    private Integer clickCount;

    /**
     * 博客内容
     */
    private String content;

    /**
     * 博客所属分类
     */
    private Integer typeId;
    private String typeName;

    /**
     * 状态（1：正常 2：停用）
     */
    private Integer status;

    /**
     * 逻辑删除 1（true）未删除， 0（false）已删除
     */
    private Integer deleted;

    /**
     * 创建时间
     */
    @DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
        private LocalDateTime gmtCreate;

    /**
     * 更新时间
     */
    @DateTimeFormat(pattern="yyyy-MM-dd HH:mm:ss")
    @JsonFormat(pattern="yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime gmtModified;
    private Integer bloggerId;
    private String bloggerName;
    private String bloggerImage;
    private Integer isPublic;
    private Integer comment;
    private Integer likeCount;
    private Integer stampCount;
    private Integer collectCount;
}
