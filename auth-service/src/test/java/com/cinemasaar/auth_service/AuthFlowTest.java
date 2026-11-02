package com.cinemasaar.auth_service;

import com.cinemasaar.auth_service.Model.User;
import com.cinemasaar.auth_service.Repository.UserRepository;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Full HTTP flow with the MongoDB repository replaced by an in-memory map,
 * so it runs without Atlas: register -> login -> /me -> admin-only check.
 */
@SpringBootTest(properties = {
        "spring.data.mongodb.uri=mongodb://localhost:27017/test",
        "jwt.secret=test-secret-test-secret-test-secret-123456",
        "jwt.expiration-ms=3600000",
        "spring.data.mongodb.auto-index-creation=false"
})
@AutoConfigureMockMvc
class AuthFlowTest {

    @Autowired
    MockMvc mvc;

    @MockitoBean
    UserRepository userRepository;

    private final Map<String, User> db = new HashMap<>();

    @BeforeEach
    void fakeDb() {
        db.clear();
        when(userRepository.existsByEmail(anyString()))
                .thenAnswer(i -> db.values().stream().anyMatch(u -> u.getEmail().equals(i.getArgument(0))));
        when(userRepository.findByEmail(anyString()))
                .thenAnswer(i -> db.values().stream().filter(u -> u.getEmail().equals(i.getArgument(0))).findFirst());
        when(userRepository.findById(anyString()))
                .thenAnswer(i -> Optional.ofNullable(db.get((String) i.getArgument(0))));
        when(userRepository.save(any(User.class))).thenAnswer(i -> {
            User u = i.getArgument(0);
            if (u.getId() == null) u.setId(UUID.randomUUID().toString());
            db.put(u.getId(), u);
            return u;
        });
    }

    @Test
    void registerLoginAndAccessProtectedEndpoints() throws Exception {
        String body = """
                {"name":"Sara","email":"Sara@Example.com","password":"password123"}""";

        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("sara@example.com"))
                .andExpect(jsonPath("$.role").value("USER"))
                .andExpect(jsonPath("$.token").isNotEmpty());

        // duplicate email -> 409
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isConflict());

        // wrong password -> 401
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"sara@example.com","password":"wrong-pass"}"""))
                .andExpect(status().isUnauthorized());

        // correct login -> token
        String loginJson = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"sara@example.com","password":"password123"}"""))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(loginJson, "$.token");

        // /me without token -> 401
        mvc.perform(get("/api/users/me")).andExpect(status().isUnauthorized());

        // /me with token -> profile, no password field
        mvc.perform(get("/api/users/me").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Sara"))
                .andExpect(jsonPath("$.password").doesNotExist());

        // USER calling admin endpoint -> 403
        mvc.perform(get("/api/users").header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden());
    }

    @Test
    void validationErrorsReturn400() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"","email":"not-an-email","password":"short"}"""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.email").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists())
                .andExpect(jsonPath("$.fieldErrors.name").exists());
    }
}
