package com.example.meetings.service;

import com.example.meetings.model.User;
import com.example.meetings.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppUserDetailsServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AppUserDetailsService appUserDetailsService;

    @Test
    void loadUserByUsername_existingUser_returnsUserDetails() {
        User user = new User("alice", "alice@example.com", "hashedPassword");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        UserDetails details = appUserDetailsService.loadUserByUsername("alice");

        assertEquals("alice", details.getUsername());
        assertEquals("hashedPassword", details.getPassword());
    }

    @Test
    void loadUserByUsername_existingUser_hasRoleUser() {
        User user = new User("alice", "alice@example.com", "hashedPassword");
        when(userRepository.findByUsername("alice")).thenReturn(Optional.of(user));

        UserDetails details = appUserDetailsService.loadUserByUsername("alice");

        assertTrue(details.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_USER")));
    }

    @Test
    void loadUserByUsername_unknownUser_throwsException() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThrows(UsernameNotFoundException.class, () ->
                appUserDetailsService.loadUserByUsername("ghost")
        );
    }
}