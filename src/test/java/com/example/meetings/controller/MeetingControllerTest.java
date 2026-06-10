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
class MeetingControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getProposePage_unauthenticated_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/meetings/new"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void getProposePage_authenticated_returnsOk() throws Exception {
        // Primeiro regista o user
        mockMvc.perform(post("/register")
                .param("username", "alice")
                .param("email", "alice@example.com")
                .param("password", "password123")
                .with(csrf()));

        mockMvc.perform(get("/meetings/new")
                        .with(user("alice")))
                .andExpect(status().isOk())
                .andExpect(view().name("propose"));
    }

    @Test
    void postPropose_validData_redirectsToCalendar() throws Exception {
        // Regista o user
        mockMvc.perform(post("/register")
                .param("username", "alice")
                .param("email", "alice@example.com")
                .param("password", "password123")
                .with(csrf()));

        mockMvc.perform(post("/meetings/new")
                        .param("title", "Team Meeting")
                        .param("description", "Weekly sync")
                        .param("start", "2025-09-01T10:00")
                        .param("end", "2025-09-01T11:00")
                        .param("invitees", "")
                        .with(user("alice"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/calendar"));
    }

    @Test
    void postPropose_endBeforeStart_returnsProposePage() throws Exception {
        mockMvc.perform(post("/register")
                .param("username", "alice")
                .param("email", "alice@example.com")
                .param("password", "password123")
                .with(csrf()));

        mockMvc.perform(post("/meetings/new")
                        .param("title", "Bad Meeting")
                        .param("start", "2025-09-01T11:00")
                        .param("end", "2025-09-01T10:00")
                        .param("invitees", "")
                        .with(user("alice"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("propose"))
                .andExpect(model().attributeExists("error"));
    }
}