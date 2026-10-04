package com.dev.csas.web;

import com.dev.csas.api.course.CourseSummaryResponse;
import com.dev.csas.api.course.StudentCourseDetailsResponse;
import com.dev.csas.api.course.StudentCourseSummaryResponse;
import com.dev.csas.course.CourseApplicationService;
import com.dev.csas.course.CourseQueryService;
import com.dev.csas.course.CourseStatus;
import com.dev.csas.course.StudentCourseQueryService;
import com.dev.csas.security.CurrentActorResolver;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;

import java.util.List;

@Controller
public class ApplicationPageController {

    private final CourseQueryService courses;
    private final CourseApplicationService courseDetails;
    private final StudentCourseQueryService studentCourses;
    private final CurrentActorResolver actors;

    public ApplicationPageController(
            CourseQueryService courses,
            CourseApplicationService courseDetails,
            StudentCourseQueryService studentCourses,
            CurrentActorResolver actors
    ) {
        this.courses = courses;
        this.courseDetails = courseDetails;
        this.studentCourses = studentCourses;
        this.actors = actors;
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/courses")
    public String courses(Authentication authentication, Model model) {
        List<CourseSummaryResponse> result = isDean(authentication)
                ? courses.listForDean()
                : courses.listForTeacher(actors.teacher(authentication));
        model.addAttribute("courses", result);
        return "courses";
    }

    @GetMapping("/courses/{courseId}")
    public String course(@PathVariable long courseId, Authentication authentication, Model model) {
        boolean dean = isDean(authentication);
        var course = dean
                ? courseDetails.getForDean(courseId)
                : courseDetails.getForTeacher(courseId, actors.teacher(authentication));
        model.addAttribute("course", course);
        model.addAttribute("canReopen", dean
                && course.status() == CourseStatus.COMPLETED
                && courseDetails.canReopenInNextSemester(courseId));
        return "course";
    }

    @PostMapping("/courses/{courseId}/reopen")
    public String reopenCourse(@PathVariable long courseId, Authentication authentication) {
        courseDetails.reopenInNextSemester(courseId, actors.dean(authentication));
        return "redirect:/courses/" + courseId;
    }

    @GetMapping("/me/courses")
    public String studentCourses(Authentication authentication, Model model) {
        List<StudentCourseSummaryResponse> result = studentCourses.list(actors.student(authentication));
        model.addAttribute("courses", result);
        return "student/courses";
    }

    @GetMapping("/me/courses/{courseId}")
    public String studentCourse(
            @PathVariable long courseId,
            Authentication authentication,
            Model model
    ) {
        StudentCourseDetailsResponse result = studentCourses.get(actors.student(authentication), courseId);
        model.addAttribute("course", result);
        return "student/course";
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
