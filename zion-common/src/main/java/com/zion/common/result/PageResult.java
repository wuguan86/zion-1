package com.zion.common.result;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 分页返回结果
 */
@Data
public class PageResult<T> implements Serializable {

    /**
     * 数据列表
     */
    private List<T> list;

    /**
     * 总条数
     */
    private Long total;

    /**
     * 当前页
     */
    private Long page;

    /**
     * 每页条数
     */
    private Long pageSize;

    public static <T> PageResult<T> of(IPage<T> page) {
        PageResult<T> result = new PageResult<>();
        result.setList(page.getRecords());
        result.setTotal(page.getTotal());
        result.setPage(page.getCurrent());
        result.setPageSize(page.getSize());
        return result;
    }

    public static <T> PageResult<T> of(List<T> list, Long total, Long page, Long pageSize) {
        PageResult<T> result = new PageResult<>();
        result.setList(list);
        result.setTotal(total);
        result.setPage(page);
        result.setPageSize(pageSize);
        return result;
    }

    public static <T> PageResult<T> empty() {
        PageResult<T> result = new PageResult<>();
        result.setList(new java.util.ArrayList<>());
        result.setTotal(0L);
        result.setPage(1L);
        result.setPageSize(10L);
        return result;
    }
}
