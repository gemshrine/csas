package com.dev.csas.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ReportPageController {

    @GetMapping("/reports")
    public String reports() {
        return "reports";
    }
}
