package com.demo;

import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class DemoController {


    @GetMapping("/ping")
    public Map<String, String> ping() {
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
