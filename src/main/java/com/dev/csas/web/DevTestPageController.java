package com.dev.csas.web;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@Profile("dev")
public class DevTestPageController {

    @GetMapping("/me")
    public String me() {
        return "dev-test";
    }
}
