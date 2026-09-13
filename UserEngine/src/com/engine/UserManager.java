package com.engine;

import com.data.users.entities.User;

import java.util.*;

public class UserManager {
    private Map<String, User> users;

    public UserManager() {
        this.users = new HashMap<>();
    }

    public void loadUsers(Collection<User> loadedUsers) {
        users.clear();
        if (loadedUsers != null) {
            for (User user : loadedUsers) {
                users.put(user.getName(), user);
            }
        }
    }

    public User getUser(String name) throws Exception {
        User user = users.get(name);
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
