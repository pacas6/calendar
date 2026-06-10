package com.example.meetings.repository;

import com.example.meetings.model.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void findByUsername_existingUser_returnsUser() {
        userRepository.save(new User("alice", "alice@example.com", "hash"));

        var result = userRepository.findByUsername("alice");

        assertTrue(result.isPresent());
        assertEquals("alice", result.get().getUsername());
    }

    @Test
    void findByUsername_unknownUser_returnsEmpty() {
        var result = userRepository.findByUsername("ghost");
        assertTrue(result.isEmpty());
    }

    @Test
    void existsByUsername_existingUser_returnsTrue() {
        userRepository.save(new User("alice", "alice@example.com", "hash"));
        assertTrue(userRepository.existsByUsername("alice"));
    }

    @Test
    void existsByUsername_unknownUser_returnsFalse() {
        assertFalse(userRepository.existsByUsername("ghost"));
    }

    @Test
    void findByIcalToken_existingToken_returnsUser() {
        User saved = userRepository.save(new User("alice", "alice@example.com", "hash"));

        var result = userRepository.findByIcalToken(saved.getIcalToken());

        assertTrue(result.isPresent());
        assertEquals("alice", result.get().getUsername());
    }

    @Test
    void findByIcalToken_invalidToken_returnsEmpty() {
        var result = userRepository.findByIcalToken("invalid-token");
        assertTrue(result.isEmpty());
    }

    @Test
    void save_duplicateUsername_throwsException() {
        userRepository.save(new User("alice", "alice@example.com", "hash"));

        assertThrows(Exception.class, () -> {
            userRepository.saveAndFlush(new User("alice", "alice2@example.com", "hash"));
        });
    }
}