package com.mrjoshuasperry.mcutils.menu;

import java.util.ArrayList;

import com.google.common.collect.Lists;

public class MenuHistory {
    private MenuManager manager;

    private int index;
    private ArrayList<Menu> history;

    /**
     * Creates a new menu history
     * 
     * @param menu The menu at the top of the history
     */
    MenuHistory(MenuManager manager, Menu menu) {
        this.manager = manager;

        this.index = 0;
        this.history = Lists.newArrayList(menu);
    }

    /**
     * Tries to increment the index of the history
     * 
     * @return True if the index is not on the last menu in the history, false
     *         otherwise
     */
    public boolean incrementIndex() {
        if (this.index != this.history.size() - 1) {
            this.manager.createNewMenu(this.history.get(++this.index));
            return true;
        }

        return false;
    }

    /**
     * Tries to decrement the index of the history
     * 
     * @return True if the index is not zero, false otherwise
     */
    public boolean decrementIndex() {
        return this.decrementIndex(false);
    }

    /**
     * Tries to decrement the index of the history
     * 
     * @param remove If the previous index should be removed
     * @return True if the index is not zero, false otherwise
     */
    public boolean decrementIndex(boolean remove) {
        if (this.index != 0) {
            if (remove) {
                this.history.remove(this.index);
            }

            this.manager.createNewMenu(this.history.get(--this.index));
            return true;
        }

        return false;
    }

    /**
     * Sets the index of the history
     * The index is clamped to be between zero and the size of the history list
     * minus one
     * 
     * @param index The index to set
     */
    public void setIndex(int index) {
        int size = this.history.size() - 1;

        // Clamp the index
        if (index < 0) {
            index = 0;
        } else if (index > size) {
            index = size;
        }

        this.index = index;
    }

    /**
     * Adds a menu safely by checking if the history already contains the menu
     * 
     * @param menu The menu to add
     * @return If the menu was added to the history, false otherwise
     */
    public boolean addSafeMenu(Menu menu) {
        return this.addSafeMenu(menu, false);
    }

    /**
     * Adds a menu safely by checking if the history already contains the menu
     * 
     * @param menu      The menu to add
     * @param increment If the index should be incremented
     * @return If the menu was added to the history, false otherwise
     */
    public boolean addSafeMenu(Menu menu, boolean increment) {
        if (!this.history.contains(menu)) {
            this.history.add(menu);

            if (increment) {
                this.incrementIndex();
            }

            return true;
        }

        return false;
    }

    /**
     * Adds a menu without checking if the menu is already in the history
     * 
     * @param menu The menu to add
     */
    public void addUnsafeMenu(Menu menu) {
        this.addUnsafeMenu(menu, false);
    }

    /**
     * Adds a menu without checking if the menu is already in the history
     * 
     * @param menu      The menu to add
     * @param increment If the index should be incremented
     */
    public void addUnsafeMenu(Menu menu, boolean increment) {
        this.history.add(menu);

        if (increment) {
            this.incrementIndex();
        }
    }

    public int getIndex() {
        return this.index;
    }

    public ArrayList<Menu> getHistory() {
        return this.history;
    }
}
