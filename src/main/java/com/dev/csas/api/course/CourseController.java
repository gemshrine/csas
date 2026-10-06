package com.dev.csas.api.course;

import com.dev.csas.course.CourseApplicationService;
import com.dev.csas.course.CourseAssignmentService;
import com.dev.csas.control.ControlSheetApplicationService;
import com.dev.csas.control.GradingScale;
import com.dev.csas.api.control.CreateControlSheetRequest;
import com.dev.csas.api.CourseHeaderResponse;
import com.dev.csas.api.course.CourseCreationRequest;
import com.dev.csas.security.CurrentActorResolver;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/courses/{courseId}")
public class CourseController {

    private final CourseApplicationService courses;
    private final CourseAssignmentService assignments;
    private final ControlSheetApplicationService controlSheets;
    private final CurrentActorResolver actors;

    public CourseController(CourseApplicationService courses, CourseAssignmentService assignments,
                            ControlSheetApplicationService controlSheets, CurrentActorResolver actors) {
        this.courses = courses;
        this.assignments = assignments;
        this.controlSheets = controlSheets;
        this.actors = actors;
    }

    @PostMapping("/control-sheet")
    @ResponseStatus(HttpStatus.CREATED)
    public ControlSheetCreatedResponse createControlSheet(
            @PathVariable long courseId,
            @Valid @RequestBody CreateControlSheetRequest request,
            Authentication authentication
    ) {
        long sheetId = controlSheets.createForCompletedCourse(courseId, actors.teacher(authentication),
                new GradingScale(request.grade5From(), request.grade4From(), request.grade3From()));
        return new ControlSheetCreatedResponse(sheetId);
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

    @PutMapping
    public CourseHeaderResponse update(
            @PathVariable long courseId,
            @Valid @RequestBody CourseCreationRequest request,
            Authentication authentication
    ) {
        actors.dean(authentication);
        return assignments.update(courseId, request);
    }

    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long courseId, Authentication authentication) {
        actors.dean(authentication);
        assignments.delete(courseId);
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

    @PostMapping("/reopen")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reopen(
            @PathVariable long courseId,
            @Valid @RequestBody CourseReopenRequest request,
            Authentication authentication
    ) {
        courses.reopen(courseId, actors.dean(authentication), request.semesterId());
    }

    public record CourseWorkCreatedResponse(long workNumber) {
    }

    public record ControlSheetCreatedResponse(long sheetId) {
    }
}
