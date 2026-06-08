package com.openquartz.easyarchive.starter.model.dto;

import lombok.Data;

import java.util.List;

@Data
public class PageResult<T> {

    private List<T> data;
    private long total;
    private int page;
    private int size;

    public PageResult(List<T> data, long total, int page, int size) {
        this.data = data;
        this.total = total;
        this.page = page;
        this.size = size;
    }

    public static <T> PageResult<T> of(List<T> data, long total, int page, int size) {
        return new PageResult<>(data, total, page, size);
    }
}
