package com.example.meetings.controller;

import com.example.meetings.model.User;
import com.example.meetings.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class ICalControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Test
    void getIcalFeed_invalidToken_returns404() throws Exception {
        mockMvc.perform(get("/ical/invalid-token.ics"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getIcalFeed_validToken_returns200() throws Exception {
        // Regista user para obter token real
        mockMvc.perform(post("/register")
                .param("username", "alice")
                .param("email", "alice@example.com")
                .param("password", "password123")
                .with(csrf()));

        // Obtém o token real da BD
        User alice = userRepository.findByUsername("alice").orElseThrow();
        String token = alice.getIcalToken();

        mockMvc.perform(get("/ical/" + token + ".ics"))
                .andExpect(status().isOk());
    }

    @Test
    void getIcalFeed_validToken_contentTypeIsCalendar() throws Exception {
        mockMvc.perform(post("/register")
                .param("username", "bob")
                .param("email", "bob@example.com")
                .param("password", "password123")
                .with(csrf()));

        User bob = userRepository.findByUsername("bob").orElseThrow();
        String token = bob.getIcalToken();

        mockMvc.perform(get("/ical/" + token + ".ics"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith("text/calendar"));
    }

    @Test
    void getIcalFeed_validToken_containsCalendarContent() throws Exception {
        mockMvc.perform(post("/register")
                .param("username", "charlie")
                .param("email", "charlie@example.com")
                .param("password", "password123")
                .with(csrf()));

        User charlie = userRepository.findByUsername("charlie").orElseThrow();
        String token = charlie.getIcalToken();

        mockMvc.perform(get("/ical/" + token + ".ics"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("BEGIN:VCALENDAR")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("END:VCALENDAR")));
    }
}