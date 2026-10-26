package com.fptaptech.taskmanager.user;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RegistrationService {
    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder passwordEncoder;

    public RegistrationService(UserRepository users, RoleRepository roles, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.roles = roles;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional
    public boolean register(RegistrationForm form) {
        String username = form.getUsername().strip();
        if (users.existsByUsername(username)) {
            return false;
        }
        AppUser user = new AppUser(username, passwordEncoder.encode(form.getPassword()));
        user.getRoles().add(roles.findByName("ROLE_STUDENT")
                .orElseThrow(() -> new IllegalStateException("ROLE_STUDENT is missing from the database")));
        users.save(user);
        return true;
    }
}
