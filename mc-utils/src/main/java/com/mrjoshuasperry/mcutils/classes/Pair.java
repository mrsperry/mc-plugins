package com.mrjoshuasperry.mcutils.classes;

public class Pair<K, V> {
    private final K key;
    private final V value;

    /**
     * Creates a simple key-value pair.
     * <br>
     * <br>
     * The contents of this pair are not required to have any relation to each other
     * and should be considered independent.
     * 
     * @param key   The first value
     * @param value The second value
     */
    public Pair(K key, V value) {
        this.key = key;
        this.value = value;
    }

    /**
     * @return The first value in this pair
     */
    public K getKey() {
        return this.key;
    }

    /**
     * @return The second value in this pair
     */
    public V getValue() {
        return this.value;
    }
}
