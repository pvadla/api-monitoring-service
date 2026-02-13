package com.api.monitor.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        return "index";  // loads index.html
    }

    @GetMapping("/dashboard")
    public String dashboard() {
        return "dashboard";  // loads dashboard.html
    }
}