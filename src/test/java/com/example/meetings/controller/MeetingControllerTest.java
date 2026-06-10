package com.example.meetings.controller;

import com.example.meetings.repository.MeetingRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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

    @Autowired
    private MeetingRepository meetingRepository;

    @Test
    void getProposePage_unauthenticated_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/meetings/new"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void getProposePage_authenticated_returnsOk() throws Exception {
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

    private org.springframework.security.core.userdetails.User principalFor(String username) {
        return new org.springframework.security.core.userdetails.User(
                username, "password123",
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    @Test
    void postRespond_accept_redirectsToCalendar() throws Exception {
        mockMvc.perform(post("/register")
                .param("username", "alice")
                .param("email", "alice@example.com")
                .param("password", "password123")
                .with(csrf()));

        mockMvc.perform(post("/register")
                .param("username", "bob")
                .param("email", "bob@example.com")
                .param("password", "password123")
                .with(csrf()));

        mockMvc.perform(post("/meetings/new")
                .param("title", "Team Meeting")
                .param("start", "2025-09-01T10:00")
                .param("end", "2025-09-01T11:00")
                .param("invitees", "bob")
                .with(SecurityMockMvcRequestPostProcessors.user(principalFor("alice")))
                .with(csrf()));

        Long meetingId = meetingRepository.findAll().get(0).getId();

        mockMvc.perform(post("/meetings/" + meetingId + "/respond")
                        .param("action", "accept")
                        .with(SecurityMockMvcRequestPostProcessors.user(principalFor("bob")))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/calendar"));
    }

    @Test
    void postRespond_decline_redirectsToCalendar() throws Exception {
        mockMvc.perform(post("/register")
                .param("username", "alice")
                .param("email", "alice@example.com")
                .param("password", "password123")
                .with(csrf()));

        mockMvc.perform(post("/register")
                .param("username", "bob")
                .param("email", "bob@example.com")
                .param("password", "password123")
                .with(csrf()));

        mockMvc.perform(post("/meetings/new")
                .param("title", "Team Meeting")
                .param("start", "2025-09-01T10:00")
                .param("end", "2025-09-01T11:00")
                .param("invitees", "bob")
                .with(SecurityMockMvcRequestPostProcessors.user(principalFor("alice")))
                .with(csrf()));

        Long meetingId = meetingRepository.findAll().get(0).getId();

        mockMvc.perform(post("/meetings/" + meetingId + "/respond")
                        .param("action", "decline")
                        .with(SecurityMockMvcRequestPostProcessors.user(principalFor("bob")))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/calendar"));
    }
}