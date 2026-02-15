package com.api.monitor.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;



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

    @GetMapping("/debug-env")
    @ResponseBody
    public String debugEnv() {
        StringBuilder sb = new StringBuilder("<pre>");
        var env = System.getenv();
        env.entrySet().stream()
            .sorted(java.util.Map.Entry.comparingByKey())
            .forEach(e -> {
                String key = e.getKey();
                String value = e.getValue();
                boolean sensitive = key.matches("(?i).*(PASSWORD|SECRET|KEY|TOKEN|CREDENTIAL|DATABASE_URL).*");
                sb.append(key).append("=").append(sensitive ? "[REDACTED]" : value).append("\n");
            });
        return sb.append("</pre>").toString();
    }
}