package com.mrjoshuasperry.mcutils.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Material;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;

import com.mrjoshuasperry.mcutils.menu.items.MenuItem;
import com.mrjoshuasperry.mcutils.menu.items.StaticMenuItem;

import net.kyori.adventure.text.Component;

/**
 * Page arithmetic at the boundaries, where off-by-ones hide: no items, exactly
 * one page, and one item over a page.
 */
class PaginatedMenuTest {
    /** A 6-row menu's inset region: rows 1-4, columns 1-7. */
    private static final int PAGE_SIZE = 28;
    private static final int ROWS = 6;

    @BeforeEach
    void setUp() {
        MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    private static List<MenuItem> items(int count) {
        List<MenuItem> items = new ArrayList<>();
        for (int index = 0; index < count; index++) {
            items.add(new StaticMenuItem(Material.STONE, null));
        }
        return items;
    }

    private static PaginatedMenu menu(int itemCount) {
        return new PaginatedMenu(Component.text("Test"), ROWS, items(itemCount));
    }

    @Test
    void theInsetRegionIsThePageSize() {
        assertEquals(PAGE_SIZE, menu(0).getPageSize());
    }

    @Test
    void anEmptyListStillHasOnePage() {
        PaginatedMenu empty = menu(0);

        assertEquals(1, empty.getPageCount(), "an empty menu must still render");
        assertEquals(0, empty.getPage());
    }

    @Test
    void exactlyOnePageWorthIsOnePage() {
        assertEquals(1, menu(PAGE_SIZE).getPageCount());
    }

    @Test
    void oneItemOverSpillsToASecondPage() {
        assertEquals(2, menu(PAGE_SIZE + 1).getPageCount());
    }

    @Test
    void pagesAreClampedToTheValidRange() {
        PaginatedMenu paged = menu(PAGE_SIZE + 1);

        paged.setPage(99);
        assertEquals(1, paged.getPage(), "past the end should clamp to the last page");

        paged.setPage(-5);
        assertEquals(0, paged.getPage(), "before the start should clamp to the first page");
    }

    @Test
    void navigatingMovesOnePageAtATime() {
        PaginatedMenu paged = menu(PAGE_SIZE * 3);

        paged.nextPage();
        assertEquals(1, paged.getPage());

        paged.nextPage();
        assertEquals(2, paged.getPage());

        paged.nextPage();
        assertEquals(2, paged.getPage(), "the last page should not advance further");

        paged.previousPage();
        assertEquals(1, paged.getPage());
    }

    @Test
    void theSecondPageHoldsTheRemainder() {
        PaginatedMenu paged = menu(PAGE_SIZE + 3);
        paged.setPage(1);

        // The inset region starts at slot 10 for a 6-row menu.
        assertNotNull(paged.getItem(10));
        assertNotNull(paged.getItem(12));
        assertNull(paged.getItem(13), "slots past the remainder should be empty, not stale");
    }

    @Test
    void refreshingContentsKeepsTheCurrentPage() {
        PaginatedMenu paged = menu(PAGE_SIZE * 3);
        paged.setPage(2);

        paged.setContents(items(PAGE_SIZE * 3));

        assertEquals(2, paged.getPage(), "a live refresh must not yank the viewer back to page one");
    }

    @Test
    void shrinkingContentsClampsTheCurrentPage() {
        PaginatedMenu paged = menu(PAGE_SIZE * 3);
        paged.setPage(2);

        paged.setContents(items(2));

        assertEquals(0, paged.getPage(), "the page the viewer was on no longer exists");
    }

    @Test
    void rowsAreClampedToAValidInventorySize() {
        // Below three rows there is no inset region at all, and above six is not a
        // legal chest size.
        assertEquals(27, new PaginatedMenu(Component.text("Small"), 1, items(0)).getSize());
        assertEquals(54, new PaginatedMenu(Component.text("Big"), 99, items(0)).getSize());
    }
}
