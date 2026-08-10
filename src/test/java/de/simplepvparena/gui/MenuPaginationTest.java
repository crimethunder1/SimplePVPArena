package de.simplepvparena.gui;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MenuPaginationTest {

    @Test
    void showsAnEmptyListAsASinglePage() {
        MenuPagination pagination = MenuPagination.of(0, 0);

        assertEquals(1, pagination.getPageCount());
        assertEquals(0, pagination.getItemCountOnPage());
        assertFalse(pagination.hasPreviousPage());
        assertFalse(pagination.hasNextPage());
    }

    @Test
    void keepsAListThatFitsOnOneSinglePage() {
        MenuPagination pagination = MenuPagination.of(MenuPagination.DEFAULT_ITEMS_PER_PAGE, 0);

        assertEquals(1, pagination.getPageCount());
        assertEquals(MenuPagination.DEFAULT_ITEMS_PER_PAGE, pagination.getItemCountOnPage());
        assertFalse(pagination.hasNextPage());
    }

    @Test
    void opensASecondPageForTheItemThatDoesNotFitAnyMore() {
        MenuPagination firstPage = MenuPagination.of(MenuPagination.DEFAULT_ITEMS_PER_PAGE + 1, 0);

        assertEquals(2, firstPage.getPageCount());
        assertEquals(MenuPagination.DEFAULT_ITEMS_PER_PAGE, firstPage.getItemCountOnPage());
        assertTrue(firstPage.hasNextPage());

        MenuPagination secondPage = MenuPagination.of(MenuPagination.DEFAULT_ITEMS_PER_PAGE + 1, 1);

        assertEquals(MenuPagination.DEFAULT_ITEMS_PER_PAGE, secondPage.getFirstItemIndex());
        assertEquals(1, secondPage.getItemCountOnPage());
        assertTrue(secondPage.hasPreviousPage());
        assertFalse(secondPage.hasNextPage());
    }

    @Test
    void countsThePagesShownToTheAdminFromOne() {
        assertEquals(1, MenuPagination.of(100, 0).getPageNumber());
        assertEquals(3, MenuPagination.of(100, 2).getPageNumber());
    }

    @Test
    void clampsAPageThatDoesNotExistIntoTheExistingOnes() {
        MenuPagination behindTheLastPage = MenuPagination.of(3, 7);
        MenuPagination beforeTheFirstPage = MenuPagination.of(3, -4);

        assertEquals(0, behindTheLastPage.pageIndex());
        assertEquals(0, beforeTheFirstPage.pageIndex());
    }

    @Test
    void clampsToTheLastPageThatIsLeftAfterItemsWereRemoved() {
        MenuPagination pagination = new MenuPagination(12, 5, 5);

        assertEquals(2, pagination.pageIndex());
        assertEquals(3, pagination.getPageCount());
        assertEquals(2, pagination.getItemCountOnPage());
    }

    @Test
    void staysOnTheEdgePagesWhenTheNavigationIsUsedOnThem() {
        MenuPagination firstPage = new MenuPagination(12, 5, 0);
        MenuPagination lastPage = new MenuPagination(12, 5, 2);

        assertEquals(0, firstPage.getPreviousPageIndex());
        assertEquals(1, firstPage.getNextPageIndex());
        assertEquals(1, lastPage.getPreviousPageIndex());
        assertEquals(2, lastPage.getNextPageIndex());
    }

    @Test
    void mapsTheSlotsOfAPageOntoTheItemsOfTheWholeList() {
        MenuPagination secondPage = new MenuPagination(12, 5, 1);

        assertEquals(5, secondPage.findItemIndex(0).orElseThrow());
        assertEquals(9, secondPage.findItemIndex(4).orElseThrow());
    }

    @Test
    void mapsNoItemOntoASlotThatTheLastPageDoesNotFill() {
        MenuPagination lastPage = new MenuPagination(12, 5, 2);

        assertTrue(lastPage.findItemIndex(1).isPresent());
        assertTrue(lastPage.findItemIndex(2).isEmpty());
        assertTrue(lastPage.findItemIndex(-1).isEmpty());
    }

    @Test
    void rejectsAPageThatCouldNotHoldASingleItem() {
        assertThrows(IllegalArgumentException.class, () -> new MenuPagination(4, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> new MenuPagination(-1, 5, 0));
    }
}
