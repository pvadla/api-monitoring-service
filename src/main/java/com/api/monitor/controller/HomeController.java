package com.api.monitor.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    // @Value("${google.client-id}")
    // private String clientId;

    @GetMapping("/")
    public String home() {
        return "index";  // loads index.html
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard";  // loads dashboard.html
    }

    // @GetMapping("/debug-env")
    // @ResponseBody
    // public String debugEnv() {
    //     //String clientId = this.clientId;
    //     String profile = System.getenv("SPRING_PROFILES_ACTIVE");
    //     return "GOOGLE_CLIENT_ID: " + clientId + "<br>PROFILE: " + profile;
    // }
}