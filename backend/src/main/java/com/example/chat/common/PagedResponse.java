package com.example.chat.common;

import java.util.List;

public class PagedResponse<T> {
    private List<T> items;
    private String nextCursor;
    private boolean hasMore;
    private int limit;

    public PagedResponse() {}

    public PagedResponse(List<T> items, String nextCursor, boolean hasMore, int limit) {
        this.items = items;
        this.nextCursor = nextCursor;
        this.hasMore = hasMore;
        this.limit = limit;
    }

    public List<T> getItems() {
        return items;
    }

    public void setItems(List<T> items) {
        this.items = items;
    }

    public String getNextCursor() {
        return nextCursor;
    }

    public void setNextCursor(String nextCursor) {
        this.nextCursor = nextCursor;
    }

    public boolean isHasMore() {
        return hasMore;
    }

    public void setHasMore(boolean hasMore) {
        this.hasMore = hasMore;
    }

    public int getLimit() {
        return limit;
    }

    public void setLimit(int limit) {
        this.limit = limit;
    }
}
