package com.ntx.blog.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoId;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Vector;

/**
 * 
 * @TableName t_blog
 */
@TableName(value ="t_blog")
@Data
//@Document(collation = "blog")
//@CompoundIndex(def = "{'title':1, 'clickCount':-1}")
public  class TBlog implements Serializable {
    /**
     * 博客编号
     */
    @TableId(type = IdType.AUTO)
//    @Indexed
//    @MongoId
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
    private LocalDateTime gmtCreate;

    /**
     * 更新时间
     */
    private  LocalDateTime gmtModified;

    private Integer blogger;
    private Integer isPublic;
    private Integer comment;
    private Integer likeCount;
    private Integer stampCount;
    private Integer collectCount;



    @TableField(exist = false)
    private  static final long serialVersionUID = 1L;



}