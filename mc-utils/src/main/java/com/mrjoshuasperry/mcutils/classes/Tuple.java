package com.mrjoshuasperry.mcutils.classes;

public final class Tuple<T, K, V> {
    private final T value1;
    private final K value2;
    private final V value3;

    /**
     * Creates a container of three independent values
     * 
     * @param value1 The first value
     * @param value2 The second value
     * @param value3 The third value
     */
    public Tuple(T value1, K value2, V value3) {
        this.value1 = value1;
        this.value2 = value2;
        this.value3 = value3;
    }

    public final T getValue1() {
        return value1;
    }

    public final K getValue2() {
        return value2;
    }

    public final V getValue3() {
        return value3;
    }
}
