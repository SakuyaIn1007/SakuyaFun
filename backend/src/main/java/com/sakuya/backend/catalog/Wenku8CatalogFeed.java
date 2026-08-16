package com.sakuya.backend.catalog;

/** Wenku8 首页栏目常量；sort 是适配服务允许的上游排序参数。 */
public enum Wenku8CatalogFeed {
    RECOMMEND("lastupdate"),
    NOVELS("weekvote"),
    RANKING("allvote");

    private final String sort;
    Wenku8CatalogFeed(String sort) { this.sort = sort; }
    public String sort() { return sort; }
}
