package de.simplepvparena.gui;

import java.util.OptionalInt;

public record MenuPagination(int itemCount, int itemsPerPage, int pageIndex) {

    public static final int DEFAULT_ITEMS_PER_PAGE = 45;

    public MenuPagination {
        if (itemCount < 0) {
            throw new IllegalArgumentException("itemCount must not be negative");
        }
        if (itemsPerPage < 1) {
            throw new IllegalArgumentException("itemsPerPage must be at least one");
        }
        pageIndex = clampPageIndex(pageIndex, pageCountOf(itemCount, itemsPerPage));
    }

    public static MenuPagination of(int itemCount, int requestedPageIndex) {
        return new MenuPagination(itemCount, DEFAULT_ITEMS_PER_PAGE, requestedPageIndex);
    }

    public int getPageCount() {
        return pageCountOf(itemCount, itemsPerPage);
    }

    public int getPageNumber() {
        return pageIndex + 1;
    }

    public int getFirstItemIndex() {
        return pageIndex * itemsPerPage;
    }

    public int getItemCountOnPage() {
        return Math.max(0, Math.min(itemsPerPage, itemCount - getFirstItemIndex()));
    }

    public boolean hasPreviousPage() {
        return pageIndex > 0;
    }

    public boolean hasNextPage() {
        return pageIndex + 1 < getPageCount();
    }

    public int getPreviousPageIndex() {
        return Math.max(0, pageIndex - 1);
    }

    public int getNextPageIndex() {
        return Math.min(getPageCount() - 1, pageIndex + 1);
    }

    public OptionalInt findItemIndex(int slot) {
        if (slot < 0 || slot >= getItemCountOnPage()) {
            return OptionalInt.empty();
        }
        return OptionalInt.of(getFirstItemIndex() + slot);
    }

    private static int pageCountOf(int itemCount, int itemsPerPage) {
        return Math.max(1, (itemCount + itemsPerPage - 1) / itemsPerPage);
    }

    private static int clampPageIndex(int pageIndex, int pageCount) {
        return Math.min(Math.max(0, pageIndex), pageCount - 1);
    }
}
