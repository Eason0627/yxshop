package com.yxshop.Utils;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

/**
 * 分页结果封装类
 */
@Data
@AllArgsConstructor
public class PageResult<T> {

    private List<T> records;
    private int total;

}