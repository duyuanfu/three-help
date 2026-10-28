package com.bangbangbang.entity;

/**
 * 列表排序契约：凡是有 sort_no 列的实体都实现本接口。
 * 数值越小越靠前；新增行时由 TableService 统一分配。
 */
public interface Sortable {

    Long getSortNo();

    void setSortNo(Long sortNo);
}
