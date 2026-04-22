package com.demo.tests;

import com.demo.DemoController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(DemoController.class)
public class AppControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void pingReturnsStatusUp() throws Exception {
        mockMvc.perform(get("/api/ping"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("UP"));
    }

    @Test
    void greetReturnsPersonalisedMessage() throws Exception {
        mockMvc.perform(get("/api/greet/Rishabh"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Hello, Rishabh! Deployed via Jenkins CI/CD."));
    }

    @Test
    void echoReturnsBodyWithTimestamp() throws Exception {
        String body = "{\"key\":\"value\"}";

        mockMvc.perform(post("/api/echo")
                        .contentType("application/json")
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.key").value("value"))
                .andExpect(jsonPath("$.echoed_at").exists());
    }

    @Test
    void thisTestWillFail() throws Exception {
        // This is intentionally wrong
        // /api/ping returns "UP" but we are asserting "DOWN"
        // This will FAIL and trigger the email
        mockMvc.perform(get("/api/ping"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("DOWN")); // ← wrong value on purpose
    }

}
