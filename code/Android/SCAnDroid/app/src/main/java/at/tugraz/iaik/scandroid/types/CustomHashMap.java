package at.tugraz.iaik.scandroid.types;

import java.util.HashMap;

/**
 * Created by Gerald Palfinger on 06.09.17.
 */

// Source: Java 7 Source Code
// Adds support for some methods added in Java 7. If only Android versions which support Java 7 are targeted, this class can be replaced with the default Java HashMap implementation.
public class CustomHashMap<K, V> extends HashMap<K, V> {
    // For compatibility with Android 5.x
    @Override
    public V putIfAbsent(K key, V value) {
        if (!containsKey(key)) {
            put(key, value);
            return null;
        }
        return value;
    }

    public V getOrDefault(Object key, V defaultValue) {
        return containsKey(key) ? get(key) : defaultValue;
    }
}
