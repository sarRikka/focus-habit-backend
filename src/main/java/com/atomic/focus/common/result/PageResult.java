package com.atomic.focus.common.result;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

import java.util.Collections;
import java.util.List;

/**
 * 分页响应信封。
 */
@Data
public class PageResult<T> {

    private List<T> items;
    private long page;
    private long pageSize;
    private long total;
    private boolean hasMore;

    public static <T> PageResult<T> empty(long page, long pageSize) {
        PageResult<T> r = new PageResult<>();
        r.items = Collections.emptyList();
        r.page = page;
        r.pageSize = pageSize;
        r.total = 0;
        r.hasMore = false;
        return r;
    }

    public static <T> PageResult<T> of(IPage<T> page) {
        PageResult<T> r = new PageResult<>();
        r.items = page.getRecords();
        r.page = page.getCurrent();
        r.pageSize = page.getSize();
        r.total = page.getTotal();
        r.hasMore = page.getCurrent() * page.getSize() < page.getTotal();
        return r;
    }

    public static <T> PageResult<T> of(List<T> items, long page, long pageSize, long total) {
        PageResult<T> r = new PageResult<>();
        r.items = items;
        r.page = page;
        r.pageSize = pageSize;
        r.total = total;
        r.hasMore = page * pageSize < total;
        return r;
    }
}
