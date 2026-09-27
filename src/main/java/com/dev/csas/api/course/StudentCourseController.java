package com.dev.csas.api.course;

import com.dev.csas.course.StudentCourseQueryService;
import com.dev.csas.security.CurrentActorResolver;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/me/courses")
public class StudentCourseController {

    private final StudentCourseQueryService courses;
    private final CurrentActorResolver actors;

    public StudentCourseController(StudentCourseQueryService courses, CurrentActorResolver actors) {
        this.courses = courses;
        this.actors = actors;
    }

    @GetMapping
    public List<StudentCourseSummaryResponse> list(Authentication authentication) {
        return courses.list(actors.student(authentication));
    }

    @GetMapping("/{courseId}")
    public StudentCourseDetailsResponse get(@PathVariable long courseId, Authentication authentication) {
        return courses.get(actors.student(authentication), courseId);
    }
}
