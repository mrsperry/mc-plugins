package com.mrjoshuasperry.mcutils.menu;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockbukkit.mockbukkit.MockBukkit;
import org.mockbukkit.mockbukkit.ServerMock;

import com.mrjoshuasperry.mcutils.menu.items.MenuItem;
import com.mrjoshuasperry.mcutils.menu.items.StaticMenuItem;

import net.kyori.adventure.text.Component;

/** How a click routes from the menu down to the item, including its click type. */
class MenuTest {
    private ServerMock server;

    @BeforeEach
    void setUp() {
        this.server = MockBukkit.mock();
    }

    @AfterEach
    void tearDown() {
        MockBukkit.unmock();
    }

    /** Records the click type handed to the three-arg handler. */
    private static final class RecordingItem extends MenuItem {
        private ClickType lastClick;

        private RecordingItem() {
            super(Material.STONE);
        }

        @Override
        public void onClick(Player player, Menu menu, ClickType click) {
            this.lastClick = click;
        }
    }

    private Menu menuWith(MenuItem item) {
        Menu menu = new Menu(Component.text("Test"), 9);
        menu.setItem(0, item);
        return menu;
    }

    @Test
    void clickTypeReachesTheItem() {
        RecordingItem item = new RecordingItem();
        Menu menu = this.menuWith(item);

        menu.clickedSlot(this.server.addPlayer(), 0, ClickType.SHIFT_LEFT);

        assertEquals(ClickType.SHIFT_LEFT, item.lastClick);
    }

    @Test
    void theTwoArgClickDefaultsToUnknown() {
        RecordingItem item = new RecordingItem();
        Menu menu = this.menuWith(item);

        menu.clickedSlot(this.server.addPlayer(), 0);

        assertEquals(ClickType.UNKNOWN, item.lastClick);
    }

    @Test
    void aPlainHandlerStillFiresWhenAClickTypeIsRouted() {
        boolean[] fired = { false };
        Menu menu = this.menuWith(new StaticMenuItem(Material.STONE, (player, clicked) -> fired[0] = true));

        // An item that only knows the two-arg handler must still run when the menu
        // routes a click type through the new three-arg path.
        menu.clickedSlot(this.server.addPlayer(), 0, ClickType.RIGHT);

        assertTrue(fired[0]);
    }
}
