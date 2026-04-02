package com.lifeos.admin.domain.vo;

import lombok.Data;

import java.util.List;

@Data
public class AdminPageResult<T> {
    private List<T> records;
    private long total;
    private int page;
    private int size;
}
