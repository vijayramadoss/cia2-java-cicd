package com.cia2.taskmanager.controller;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Shows which version of the app is running and which pod answered.
 * The pipeline sets APP_VERSION to the Git commit, so a new deployment
 * is visible straight away in the browser.
 */
@RestController
public class InfoController {

    private final String appName;
    private final String version;
    private final String message;

    public InfoController(
            @Value("${app.name}") String appName,
            @Value("${app.version}") String version,
            @Value("${app.message}") String message) {
        this.appName = appName;
        this.version = version;
        this.message = message;
    }

    @GetMapping("/api/info")
    public Map<String, String> info() {
        return Map.of(
                "name", appName,
                "version", version,
                "message", message,
                "host", hostName());
    }

    static String hostName() {
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            return "unknown";
        }
    }
}
