package com.formationcloud.platform.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Make React the official UI entry point.
 *
 * Why:
 * This project still contains legacy static HTML pages under /static.
 * If we don't redirect, opening http://localhost:8080/ will show the old UI,
 * which is confusing for users and for the jury.
 */
@Controller
public class RootRedirectController {

    @GetMapping({"/", "/index.html"})
    public String root() {
        return "redirect:/app/";
    }

    // Optional compatibility redirects (in case someone bookmarked old pages)
    @GetMapping({"/login", "/login.html", "/register", "/register.html"})
    public String authPages() {
        return "redirect:/app/";
    }
}
