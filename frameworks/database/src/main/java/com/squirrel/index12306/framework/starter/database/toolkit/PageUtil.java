package com.squirrel.index12306.framework.starter.database.toolkit;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.squirrel.index12306.framework.starter.common.toolkit.BeanUtil;
import com.squirrel.index12306.framework.starter.convention.page.PageRequest;
import com.squirrel.index12306.framework.starter.convention.page.PageResponse;

/**
 * 分页工具类
 */
public class PageUtil {

    /**
     * {@link PageRequest} to {@link Page}
     * @param pageRequest 分页请求参数
     * @return Page
     */
    public static Page convert(PageRequest pageRequest) {
        return convert(pageRequest.getCurrent(),pageRequest.getSize());
    }

    /**
     * {@link PageRequest} to {@link Page}
     *
     * @param current 当前页
     * @param size 每页数据量大小
     * @return Page
     */
    public static Page convert(long current, long size) {
        return new Page(current, size);
    }

    /**
     * {@link IPage} to {@link PageRequest}
     *
     * @param iPage 分页参数
     * @return PageResponse
     */
    public static PageResponse convert(IPage iPage) {
        return buildConventionPage(iPage);
    }

    /**
     * {@link IPage} to {@link PageRequest}
     *
     * @param iPage 分页参数
     * @param targetClass 目标类
     * @param <TARGET> 目标类泛型
     * @param <ORIGINAL> 原始类泛型
     * @return PageResponse
     */
    public static <TARGET, ORIGINAL> PageResponse<TARGET> convert(IPage<ORIGINAL> iPage, Class<TARGET> targetClass) {
        iPage.convert(each -> BeanUtil.convert(each, targetClass));
        return buildConventionPage(iPage);
    }

    /**
     * {@link IPage} build to {@link PageRequest}
     *
     * @param iPage 分页参数
     * @return PageResponse
     */
    private static PageResponse buildConventionPage(IPage iPage) {
        return PageResponse.builder().current(iPage.getCurrent()).size(iPage.getSize()).records(iPage.getRecords()).total(iPage.getTotal()).build();
    }
}
