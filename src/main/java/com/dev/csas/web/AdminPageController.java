package com.dev.csas.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class AdminPageController {

    @GetMapping("/admin/users")
    public String users() {
        return "admin/users";
    }
}
