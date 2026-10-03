package com.dev.csas.api.course;

import com.dev.csas.course.CourseQueryService;
import com.dev.csas.security.CurrentActorResolver;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.http.HttpStatus;
import jakarta.validation.Valid;
import com.dev.csas.course.CourseAssignmentService;
import com.dev.csas.api.CourseHeaderResponse;

import java.util.List;

@RestController
@RequestMapping("/api/courses")
public class CourseListController {

    private final CourseQueryService service;
    private final CourseAssignmentService assignment;
    private final CurrentActorResolver actors;

    public CourseListController(CourseQueryService service, CourseAssignmentService assignment,
                                CurrentActorResolver actors) {
        this.service = service;
        this.assignment = assignment;
        this.actors = actors;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public CourseHeaderResponse assign(@Valid @RequestBody CourseCreationRequest request,
                                       Authentication authentication) {
        actors.dean(authentication);
        return assignment.assign(request);
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
