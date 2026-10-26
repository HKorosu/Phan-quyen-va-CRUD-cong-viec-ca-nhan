package com.fptaptech.taskmanager.security;

import com.fptaptech.taskmanager.user.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {
    private final UserRepository users;

    public DatabaseUserDetailsService(UserRepository users) {
        this.users = users;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        var appUser = users.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Account not found"));
        var authorities = appUser.getRoles().stream()
                .map(role -> new SimpleGrantedAuthority(role.getName()))
                .toList();
        return User.withUsername(appUser.getUsername())
                .password(appUser.getPassword())
                .authorities(authorities)
                .build();
    }
}
