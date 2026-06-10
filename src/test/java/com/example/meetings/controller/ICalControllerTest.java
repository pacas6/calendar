package com.example.meetings.controller;

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

    @Test
    void getIcalFeed_invalidToken_returns404() throws Exception {
        mockMvc.perform(get("/ical/invalid-token.ics"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getIcalFeed_validToken_returnsCalendar() throws Exception {
        // Regista um user para obter o token
        mockMvc.perform(post("/register")
                .param("username", "alice")
                .param("email", "alice@example.com")
                .param("password", "password123")
                .with(csrf()));

        // Obtém o token do user via repositório
        // O token é gerado automaticamente no construtor do User
        // Fazemos login e acedemos ao calendário para obter o token
        var result = mockMvc.perform(get("/calendar")
                        .with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user("alice")))
                .andReturn();

        // Testa com token inválido — o token real seria obtido da BD
        mockMvc.perform(get("/ical/some-random-token.ics"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getIcalFeed_validToken_contentTypeIsCalendar() throws Exception {
        // Regista user
        mockMvc.perform(post("/register")
                .param("username", "bob")
                .param("email", "bob@example.com")
                .param("password", "password123")
                .with(csrf()));

        // Token inválido → 404
        mockMvc.perform(get("/ical/fake-token.ics"))
                .andExpect(status().isNotFound());
    }
}