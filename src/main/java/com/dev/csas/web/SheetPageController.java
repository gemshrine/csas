package com.dev.csas.web;

import com.dev.csas.api.attendance.AttendanceSheetDetailsResponse;
import com.dev.csas.api.control.ControlSheetDetailsResponse;
import com.dev.csas.attendance.AttendanceSheetQueryService;
import com.dev.csas.control.ControlSheetQueryService;
import com.dev.csas.coursework.CourseworkSheetDetailsResponse;
import com.dev.csas.coursework.CourseworkSheetQueryService;
import com.dev.csas.security.CurrentActorResolver;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@Controller
public class SheetPageController {

    private final ControlSheetQueryService controlSheets;
    private final AttendanceSheetQueryService attendanceSheets;
    private final CourseworkSheetQueryService courseworkSheets;
    private final CurrentActorResolver actors;

    public SheetPageController(
            ControlSheetQueryService controlSheets,
            AttendanceSheetQueryService attendanceSheets,
            CourseworkSheetQueryService courseworkSheets,
            CurrentActorResolver actors
    ) {
        this.controlSheets = controlSheets;
        this.attendanceSheets = attendanceSheets;
        this.courseworkSheets = courseworkSheets;
        this.actors = actors;
    }

    @GetMapping("/control-sheets/{sheetId}")
    public String controlSheet(
            @PathVariable long sheetId,
            Authentication authentication,
            Model model
    ) {
        ControlSheetDetailsResponse sheet = isDean(authentication)
                ? controlSheets.getForDean(sheetId)
                : controlSheets.getForTeacher(sheetId, actors.teacher(authentication));
        model.addAttribute("sheet", sheet);
        return "control-sheet";
    }

    @GetMapping("/attendance-sheets/{sheetId}")
    public String attendanceSheet(
            @PathVariable long sheetId,
            Authentication authentication,
            Model model
    ) {
        AttendanceSheetDetailsResponse sheet = isDean(authentication)
                ? attendanceSheets.getForDean(sheetId)
                : attendanceSheets.getForTeacher(sheetId, actors.teacher(authentication));
        model.addAttribute("sheet", sheet);
        return "attendance-sheet";
    }

    @GetMapping("/coursework-sheets/{sheetId}")
    public String courseworkSheet(
            @PathVariable long sheetId,
            Authentication authentication,
            Model model
    ) {
        CourseworkSheetDetailsResponse sheet = isDean(authentication)
                ? courseworkSheets.getForDean(sheetId)
                : courseworkSheets.getForTeacher(sheetId, actors.teacher(authentication));
        model.addAttribute("sheet", sheet);
        return "coursework-sheet";
    }

    private boolean isDean(Authentication authentication) {
        if (authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_DEAN"))) {
            actors.dean(authentication);
            return true;
        }
        return false;
    }
}
