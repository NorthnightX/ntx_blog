package com.ntx.common.domain;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;

/**
 * 
 * @TableName t_blog_type
 */
@TableName(value ="t_blog_type")
@Data
public class TBlogType implements Serializable {
    /**
     * 博客分类编号
     */
    @TableId(type = IdType.AUTO)
    private Integer id;

    /**
     * 博客分类名称
     */
    private String name;

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
    private LocalDateTime gmtModified;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;

}