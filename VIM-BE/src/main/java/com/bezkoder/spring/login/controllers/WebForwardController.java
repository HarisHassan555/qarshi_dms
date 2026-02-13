package com.bezkoder.spring.login.controllers;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
public class WebForwardController {

    /**
     * Forwards any request that is not an API call and not a static file to index.html.
     * This allows Angular to handle the routing on page refresh.
     * Matches any path that doesn't contain a dot (excluding files) and is not /api/**.
     */
    @RequestMapping(value = "{path:[^\\.]*}")
    public String redirect() {
        return "forward:/index.html";
    }
}
