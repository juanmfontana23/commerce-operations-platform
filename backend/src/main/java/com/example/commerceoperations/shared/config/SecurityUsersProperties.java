package com.example.commerceoperations.shared.config;

import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security")
public class SecurityUsersProperties {

    private Map<String, SecurityUserProperties> users = Map.of();

    public Map<String, SecurityUserProperties> getUsers() {
        return users;
    }

    public void setUsers(Map<String, SecurityUserProperties> users) {
        this.users = users;
    }
}
