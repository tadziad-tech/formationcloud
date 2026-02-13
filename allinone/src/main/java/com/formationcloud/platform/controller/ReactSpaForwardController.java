package com.formationcloud.platform.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * React SPA entry point.
 *
 * IMPORTANT:
 * This project uses HashRouter (/#/...) so the server only receives "/app".
 * We only need to forward /app -> /app/index.html.
 */
@Controller
public class ReactSpaForwardController {

    @GetMapping({"/app", "/app/"})
    public String forwardRoot() {
        return "forward:/app/index.html";
    }
}
