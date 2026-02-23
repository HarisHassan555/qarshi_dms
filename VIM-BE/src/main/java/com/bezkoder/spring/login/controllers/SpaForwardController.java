package com.bezkoder.spring.login.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

import javax.servlet.http.HttpServletRequest;

/**
 * SPA fallback: forward non-API, non-static routes to index.html
 * so Angular can handle deep links on refresh.
 */
@Controller
public class SpaForwardController {

    @RequestMapping(value = {
        "/{path:[^\\.]*}",
        "/**/{path:[^\\.]*}"
    })
    public String forward(HttpServletRequest request) {
        String uri = request.getRequestURI();
        if (uri.startsWith("/api/") || uri.equals("/api")) {
            return "forward:" + uri;
        }
        return "forward:/index.html";
    }
}
