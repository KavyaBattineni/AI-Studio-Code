package com.aivideo.platform.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Exercises the real HTTP layer (controllers, validation, security, exception handling) end
 * to end for the most important user journey in the app. The OTP itself is never exposed by
 * any API response (by design - see OtpService), so this test captures it the same way
 * MockEmailService "delivers" it in real local development: via EmailService.sendOtpEmail.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthFlowIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EmailService emailService;

    @Test
    void fullRegisterVerifyLoginJourney() throws Exception {
        String email = "e2e-" + System.nanoTime() + "@example.com";
        String registerBody = objectMapper.writeValueAsString(Map.of(
                "name", "E2E User", "email", email, "mobile", "9123456782",
                "password", "Password@123", "confirmPassword", "Password@123"));

        mockMvc.perform(post("/api/auth/register").contentType("application/json").content(registerBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true));

        ArgumentCaptor<String> otpCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendOtpEmail(eq(email), anyString(), otpCaptor.capture(), anyString());
        String otp = otpCaptor.getValue();
        assertNotNull(otp);

        String verifyBody = objectMapper.writeValueAsString(Map.of("email", email, "otp", otp));
        mockMvc.perform(post("/api/auth/verify-otp").contentType("application/json").content(verifyBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.data.user.email").value(email));

        String loginBody = objectMapper.writeValueAsString(Map.of("email", email, "password", "Password@123"));
        mockMvc.perform(post("/api/auth/login").contentType("application/json").content(loginBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.user.email").value(email))
                .andExpect(jsonPath("$.data.user.role").value("USER"));
    }

    @Test
    void login_withWrongPassword_returnsUnauthorizedWithoutRevealingWhetherEmailExists() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("email", "nobody-" + System.nanoTime() + "@example.com", "password", "whatever123"));
        mockMvc.perform(post("/api/auth/login").contentType("application/json").content(body))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errorCode").value("UNAUTHORIZED"))
                .andExpect(jsonPath("$.message").value("Invalid email or password"));
    }

    @Test
    void protectedEndpoint_withoutToken_returnsUnauthorized() throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/wallet"))
                .andExpect(status().isUnauthorized());
    }
}
