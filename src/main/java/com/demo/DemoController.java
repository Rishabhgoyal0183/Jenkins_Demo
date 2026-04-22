package com.demo;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@RestController
@RequestMapping("/api")
@Slf4j
public class DemoController {

    Logger logger = Logger.getLogger(DemoController.class.getName());

    @GetMapping("/ping")
    public Map<String, String> ping() {
        logger.log(Level.INFO, "Received /ping request");
        return Map.of(
                "status",    "UP",
                "message",   "Jenkins Demo App is running!",
                "timestamp", LocalDateTime.now().toString()
        );
    }

    @GetMapping("/greet/{name}")
    public Map<String, String> greet(@PathVariable String name) {
        return Map.of(
                "message", "Hello, " + name + "! Deployed via Jenkins CI/CD."
        );
    }


    @PostMapping("/echo")
    public Map<String, Object> echo(@RequestBody Map<String, Object> body) {
        body.put("echoed_at", LocalDateTime.now().toString());
        return body;
    }
}
