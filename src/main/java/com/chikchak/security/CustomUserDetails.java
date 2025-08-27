package com.chikchak.security;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.GrantedAuthority;
import java.util.Collection;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.List;

// Custom User Details
@RequiredArgsConstructor
public class CustomUserDetails implements UserDetails {
    private final com.chikchak.model.User user;

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getEmail();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return user.getStatus() != com.chikchak.model.UserStatus.SUSPENDED;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.getStatus() == com.chikchak.model.UserStatus.ACTIVE;
    }

    public Long getUserId() {
        return user.getId();
    }

    public com.chikchak.model.User getUser() {
        return user;
    }
}
