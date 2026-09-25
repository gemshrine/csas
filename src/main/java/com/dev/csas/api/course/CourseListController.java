package com.dev.csas.api.course;

import com.dev.csas.course.CourseQueryService;
import com.dev.csas.security.CurrentActorResolver;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
public class CourseListController {

    private final CourseQueryService service;
    private final CurrentActorResolver actors;

    public CourseListController(CourseQueryService service, CurrentActorResolver actors) {
        this.service = service;
        this.actors = actors;
    }

    @GetMapping
    public List<CourseSummaryResponse> list(Authentication authentication) {
        if (authentication.getAuthorities().stream().anyMatch(authority ->
                authority.getAuthority().equals("ROLE_DEAN"))) {
            actors.dean(authentication);
            return service.listForDean();
        }
        return service.listForTeacher(actors.teacher(authentication));
    }
}
