package com.dev.csas.api.course;

import com.dev.csas.course.CourseApplicationService;
import com.dev.csas.security.CurrentActorResolver;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/courses/{courseId}")
public class CourseController {

    private final CourseApplicationService courses;
    private final CurrentActorResolver actors;

    public CourseController(CourseApplicationService courses, CurrentActorResolver actors) {
        this.courses = courses;
        this.actors = actors;
    }

    @GetMapping
    public CourseDetailsResponse get(@PathVariable long courseId, Authentication authentication) {
        if (authentication.getAuthorities().stream().anyMatch(authority ->
                authority.getAuthority().equals("ROLE_DEAN"))) {
            actors.dean(authentication);
            return courses.getForDean(courseId);
        }
        return courses.getForTeacher(courseId, actors.teacher(authentication));
    }

    @PostMapping("/works")
    @ResponseStatus(HttpStatus.CREATED)
    public CourseWorkCreatedResponse addWork(@PathVariable long courseId,
                                             @Valid @RequestBody CourseWorkRequest request,
                                             Authentication authentication) {
        return new CourseWorkCreatedResponse(
                courses.addWork(courseId, actors.teacher(authentication), request)
        );
    }

    @PutMapping("/works/{workNumber}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void updateWork(@PathVariable long courseId, @PathVariable long workNumber,
                           @Valid @RequestBody CourseWorkRequest request, Authentication authentication) {
        courses.updateWork(courseId, workNumber, actors.teacher(authentication), request);
    }

    @PutMapping("/works/{workNumber}/scores/{studentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void enterScore(@PathVariable long courseId, @PathVariable long workNumber,
                           @PathVariable long studentId, @Valid @RequestBody CourseScoreRequest request,
                           Authentication authentication) {
        courses.enterScore(courseId, workNumber, studentId, actors.teacher(authentication), request.score());
    }

    @PostMapping("/complete")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void complete(@PathVariable long courseId, Authentication authentication) {
        courses.complete(courseId, actors.teacher(authentication));
    }

    public record CourseWorkCreatedResponse(long workNumber) {
    }
}
