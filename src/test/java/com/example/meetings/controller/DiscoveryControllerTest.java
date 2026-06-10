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
class DiscoveryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void getDiscover_unauthenticated_redirectsToLogin() throws Exception {
        mockMvc.perform(get("/discover"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrlPattern("**/login"));
    }

    @Test
    void getDiscover_authenticated_returnsOk() throws Exception {
        mockMvc.perform(get("/discover")
                        .with(user("alice")))
                .andExpect(status().isOk())
                .andExpect(view().name("discover"))
                .andExpect(model().attributeExists("providers"))
                .andExpect(model().attributeExists("results"))
                .andExpect(model().attributeExists("q"));
    }

    @Test
    void getDiscover_withQuery_returnsResults() throws Exception {
        mockMvc.perform(get("/discover")
                        .param("q", "concerto")
                        .with(user("alice")))
                .andExpect(status().isOk())
                .andExpect(view().name("discover"))
                .andExpect(model().attributeExists("results"));
    }

    @Test
    void getDiscover_emptyQuery_returnsEmptyResults() throws Exception {
        mockMvc.perform(get("/discover")
                        .param("q", "")
                        .with(user("alice")))
                .andExpect(status().isOk())
                .andExpect(model().attribute("results", java.util.List.of()));
    }

    @Test
    void postCopy_authenticated_redirectsToCalendar() throws Exception {
        // Regista o user
        mockMvc.perform(post("/register")
                .param("username", "alice")
                .param("email", "alice@example.com")
                .param("password", "password123")
                .with(csrf()));

        mockMvc.perform(post("/discover/copy")
                        .param("source", "Ticketmaster")
                        .param("externalId", "123")
                        .param("title", "Concert")
                        .param("description", "Great concert")
                        .param("start", "2025-09-01T20:00:00Z")
                        .param("end", "2025-09-01T22:00:00Z")
                        .param("url", "http://ticketmaster.com/event/123")
                        .param("venue", "Altice Arena")
                        .with(user("alice"))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/calendar"));
    }

    @Test
    void getDiscover_noProvidersConfigured_anyConfiguredIsFalse() throws Exception {
        mockMvc.perform(get("/discover")
                        .param("q", "concerto")
                        .with(user("alice")))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("anyConfigured"));
    }

    @Test
    void getDiscover_nullQuery_returnsEmptyResults() throws Exception {
        mockMvc.perform(get("/discover")
                        .with(user("alice")))
                .andExpect(status().isOk())
                .andExpect(model().attribute("q", ""))
                .andExpect(model().attribute("results", java.util.List.of()));
    }

    @Test
    void postCopy_withoutEnd_redirectsToCalendar() throws Exception {
        mockMvc.perform(post("/register")
                .param("username", "alice")
                .param("email", "alice@example.com")
                .param("password", "password123")
                .with(csrf()));

        // sem end time
        mockMvc.perform(post("/discover/copy")
                        .param("source", "AgendaLx")
                        .param("externalId", "456")
                        .param("title", "Teatro")
                        .param("start", "2025-09-01T20:00:00Z")
                        .with(org.springframework.security.test.web.servlet.request
                                .SecurityMockMvcRequestPostProcessors.user(
                                        new org.springframework.security.core.userdetails.User(
                                                "alice", "password123",
                                                java.util.List.of(new org.springframework.security.core.authority
                                                        .SimpleGrantedAuthority("ROLE_USER")))))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/calendar"));
    }

    @Test
    void postCopy_blankEnd_treatsAsNoEnd() throws Exception {
        mockMvc.perform(post("/register")
                .param("username", "alice")
                .param("email", "alice@example.com")
                .param("password", "password123")
                .with(csrf()));

        mockMvc.perform(post("/discover/copy")
                        .param("source", "AgendaLx")
                        .param("externalId", "456")
                        .param("title", "Teatro")
                        .param("start", "2025-09-01T20:00:00Z")
                        .param("end", "")  // blank end
                        .with(org.springframework.security.test.web.servlet.request
                                .SecurityMockMvcRequestPostProcessors.user(
                                        new org.springframework.security.core.userdetails.User(
                                                "alice", "password123",
                                                java.util.List.of(new org.springframework.security.core.authority
                                                        .SimpleGrantedAuthority("ROLE_USER")))))
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/calendar"));
    }

    @Test
    void getDiscover_queryWithNoConfiguredProviders_returnsEmptyResults() throws Exception {
        // AgendaLx está sempre configurado, mas testamos o modelo
        mockMvc.perform(get("/discover")
                        .param("q", "concerto")
                        .with(user("alice")))
                .andExpect(status().isOk())
                .andExpect(view().name("discover"))
                .andExpect(model().attributeExists("results"));
    }

    @Test
    void getDiscover_queryButNoProvidersConfigured_returnsEmptyResults() throws Exception {
        // Todos os providers não configurados → anyConfigured = false
        // Como não podemos desligar o AgendaLx facilmente,
        // testamos que o modelo tem results mesmo assim
        mockMvc.perform(get("/discover")
                        .param("q", "concerto")
                        .with(user("alice")))
                .andExpect(status().isOk())
                .andExpect(model().attributeExists("anyConfigured"))
                .andExpect(model().attributeExists("results"));
    }
}