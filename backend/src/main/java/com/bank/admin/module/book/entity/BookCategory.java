package com.bank.admin.module.book.entity;

import com.bank.admin.common.entity.BaseEntity;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 记账分类实体
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("book_category")
public class BookCategory extends BaseEntity {

    /** 分类名称 */
    private String name;

    /** 图标标识（Element Plus 图标名） */
    private String icon;

    /** 分类类型：1收入 2支出 */
    private Integer type;

    /** 排序号 */
    private Integer sortOrder;

    /** 父级ID（0=一级分类） */
    private Long parentId;

    /** 状态：0启用 1停用 */
    private Integer status;
}
