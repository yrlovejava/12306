package com.squirrel.index12306.framework.starter.convention.page;

import lombok.Data;

/**
 * 分页请求对象
 *
 * <p>{@link PageRequest}、{@link PageResponse}
 * 可以理解为防腐层的一种实现，不论底层 ORM 框架，对外分页参数属性不变
 */
@Data
public class PageRequest {

    /**
     * 当前页
     */
    private Long current;

    /**
     * 每页显示条数
     */
    private Long size;
}
