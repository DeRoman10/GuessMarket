package com.engine;

import com.data.users.entities.User;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class UserManager {
    private final Map<String, User> users = new ConcurrentHashMap<>();

    private static String key(String name) { return name.trim().toLowerCase(); }

    public boolean addUserIfAbsent(User user) {
        return users.putIfAbsent(key(user.getName()), user) == null;
    }

    public void loadUsers(Collection<User> loadedUsers) {
        users.clear();
        if (loadedUsers != null) {
            for (User user : loadedUsers) users.put(key(user.getName()), user);
        }
    }

    public User getUser(String name) throws Exception {
        User user = (name == null) ? null : users.get(key(name));
        if (user == null) {
            throw new Exception("User '" + name + "' not found in the system.");
        }
        return user;
    }

    public Collection<User> getAllUsers() {
        return users.values();
    }

    public Set<String> getManagedEvents(User user) {
        if (user == null || user.getManagedEvents() == null) {
            return new HashSet<>();
        }
        return user.getManagedEvents();
    }
}
