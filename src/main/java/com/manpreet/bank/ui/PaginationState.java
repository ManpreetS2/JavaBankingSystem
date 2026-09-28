package com.manpreet.bank.ui;

/**
 * Pure pagination helpers for the Transactions workspace.
 */
public final class PaginationState {

    public static final int PAGE_SIZE = 20;

    private int pageIndex;
    private long totalCount;

    public PaginationState() {
        this(0, 0L);
    }

    public PaginationState(int pageIndex, long totalCount) {
        this.pageIndex = Math.max(0, pageIndex);
        this.totalCount = Math.max(0L, totalCount);
        clamp();
    }

    public void reset() {
        pageIndex = 0;
    }

    public void setTotalCount(long totalCount) {
        this.totalCount = Math.max(0L, totalCount);
        clamp();
    }

    public int getPageIndex() {
        return pageIndex;
    }

    public long getTotalCount() {
        return totalCount;
    }

    public int getPageSize() {
        return PAGE_SIZE;
    }

    public int getOffset() {
        return pageIndex * PAGE_SIZE;
    }

    public int getTotalPages() {
        if (totalCount <= 0) {
            return 1;
        }
        return (int) ((totalCount + PAGE_SIZE - 1) / PAGE_SIZE);
    }

    public boolean hasPrevious() {
        return pageIndex > 0;
    }

    public boolean hasNext() {
        return pageIndex + 1 < getTotalPages();
    }

    public void previous() {
        if (hasPrevious()) {
            pageIndex--;
        }
    }

    public void next() {
        if (hasNext()) {
            pageIndex++;
        }
    }

    public String pageLabel() {
        return "Page " + (pageIndex + 1) + " of " + getTotalPages();
    }

    private void clamp() {
        int maxIndex = Math.max(0, getTotalPages() - 1);
        if (pageIndex > maxIndex) {
            pageIndex = maxIndex;
        }
    }
}
