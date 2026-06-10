package com.example.meetings.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class CalendarControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getCalendar_unauthenticated_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/calendar"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void getCalendar_authenticated_returnsOk() throws Exception {
        mockMvc.perform(post("/register")
                .param("username", "alice")
                .param("email", "alice@example.com")
                .param("password", "password123")
                .with(csrf()));

        mockMvc.perform(get("/calendar")
                        .with(user("alice")))
                .andExpect(status().isOk())
                .andExpect(view().name("calendar"))
                .andExpect(model().attributeExists("meetings"))
                .andExpect(model().attributeExists("pendingInvites"))
                .andExpect(model().attributeExists("icalHttpUrl"));
    }

    @Test
    void getCalendar_authenticated_icalUrlIsPresent() throws Exception {
        mockMvc.perform(post("/register")
                .param("username", "bob")
                .param("email", "bob@example.com")
                .param("password", "password123")
                .with(csrf()));

        mockMvc.perform(get("/calendar")
                        .with(user("bob")))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("icalWebcalUrl"));
    }
}