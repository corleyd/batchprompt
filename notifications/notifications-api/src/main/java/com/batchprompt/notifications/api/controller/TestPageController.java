package com.batchprompt.notifications.api.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Controller for rendering test pages.
 */
@Controller
@RequestMapping("/test")
public class TestPageController {

    /**
     * Renders the WebSocket test page.
     * 
     * @return the name of the template to render
     */
    @GetMapping("/ws")
    public String getWebSocketTestPage() {
        return "wstest";
    }
}
