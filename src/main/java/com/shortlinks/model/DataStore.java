package com.shortlinks.model;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class DataStore {
    private Map<UUID, UserAccount> users = new HashMap<>();
    private Map<String, ShortLink> shortLinks = new HashMap<>();

    public Map<UUID, UserAccount> getUsers() {
        return users;
    }

    public void setUsers(Map<UUID, UserAccount> users) {
        this.users = users;
    }

    public Map<String, ShortLink> getShortLinks() {
        return shortLinks;
    }

    public void setShortLinks(Map<String, ShortLink> shortLinks) {
        this.shortLinks = shortLinks;
    }
}
