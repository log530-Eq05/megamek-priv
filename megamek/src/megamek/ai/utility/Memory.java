/*
 * Copyright (c) 2024 - The MegaMek Team. All Rights Reserved.
 *
 *  This program is free software; you can redistribute it and/or modify it
 *  under the terms of the GNU General Public License as published by the Free
 *  Software Foundation; either version 2 of the License, or (at your option)
 *  any later version.
 *
 *  This program is distributed in the hope that it will be useful, but
 *  WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 *  or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License
 *  for more details.
 */
package megamek.ai.utility;

import java.util.*;

/**
 * Memory class to store data for state processing and decision-making.
 * The memory class is not much dissimilar from the "Options" that many objects have, the difference is that the memory is supposed to be
 * volatile, it is not saved anywhere, it is just a temporary storage for the AI to use during the execution of the AI.
 * The memory will usually keep tabs on "things" that happened, so you can iteratively build on top of what happened in the past to
 * determine future events. One example would be to keep track of the last target that was attacked, so you can avoid attacking the same
 * twice in a roll, or maybe you have a penalty if you were shot at last turn by a specific weapon or in a specific place, this avoids
 * having to keep track of all this information in the object itself, adding an indefinite amount of flags and toggles, and instead
 * allowing for arbitrary information to be stored "on the fly" and used as needed. And of course, to be forgotten when done with.
 * To push anything into memory all you have to do is get a reference to the memory and put the information in it, either with "put" or
 * with computeIfAbsent, which will only put the information if it is not already there, and also good if you want to put a default value
 * such as a list or a map.
 * Every memory has to have a key, which is a string that identifies the memory, and a value, which can be any object, but it is recommended
 * to use the same type of object for the same key, as the memory does not enforce any type of type safety, and you will have to remember
 * the type of the object you put in the memory when you get it back, otherwise you may get a ClassCastException if you try to do it by
 * hand, or an optional empty if you use the "get" method with the wrong type.
 * The use of the memory is reasonably safe, you should not find issues with nullpointers, as the memory will return an empty optional if
 * the key is not found, and you can check if the key is present with "containsKey". Requesting for a specific return type will also return
 * an empty optional if the key is found but the type is not the one you requested, which hardens even more the feature against clerical
 * mistakes.
 * @author Luana Coppio
 */
public class Memory {

    private final Map<String, Object> memory = new HashMap<>();

    public void remove(String key) {
        memory.remove(key);
    }

    public void clear(String prefix) {
        memory.entrySet().removeIf(entry -> entry.getKey().startsWith(prefix));
    }

    public void clear() {
        memory.clear();
    }

    public void put(String key, Object value) {
        memory.put(key, value);
    }

    public Optional<Object> get(String key) {
        return Optional.ofNullable(memory.getOrDefault(key, null));
    }

    public boolean containsKey(String key) {
        return memory.containsKey(key);
    }

    public Optional<String> getString(String key) {
        if (memory.containsKey(key) && memory.get(key) instanceof String) {
            return Optional.of((String) memory.get(key));
        }
        return Optional.empty();
    }

    public Optional<Integer> getInt(String key) {
        if (memory.containsKey(key) && memory.get(key) instanceof Integer) {
            return Optional.of((Integer) memory.get(key));
        }
        return Optional.empty();
    }

    public Optional<Double> getDouble(String key) {
        if (memory.containsKey(key) && memory.get(key) instanceof Double) {
            return Optional.of((Double) memory.get(key));
        }
        return Optional.empty();
    }

    public Optional<Boolean> getBoolean(String key) {
        if (memory.containsKey(key) && memory.get(key) instanceof Boolean) {
            return Optional.of((Boolean) memory.get(key));
        }
        return Optional.empty();
    }
}
