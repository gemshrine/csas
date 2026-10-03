package com.dev.csas.coursework;

import com.dev.csas.security.CurrentActorResolver;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/coursework-sheets/{sheetId}")
public class CourseworkSheetController {

    private final CourseworkSheetApplicationService application;
    private final CourseworkSheetQueryService query;
    private final CurrentActorResolver actors;

    public CourseworkSheetController(
            CourseworkSheetApplicationService application,
            CourseworkSheetQueryService query,
            CurrentActorResolver actors
    ) {
        this.application = application;
        this.query = query;
        this.actors = actors;
    }

    @GetMapping
    public CourseworkSheetDetailsResponse get(@PathVariable long sheetId, Authentication authentication) {
        if (authentication.getAuthorities().stream().anyMatch(
                authority -> authority.getAuthority().equals("ROLE_DEAN"))) {
            actors.dean(authentication);
            return query.getForDean(sheetId);
        }
        return query.getForTeacher(sheetId, actors.teacher(authentication));
    }

    @PutMapping("/results/{studentId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void enterScore(
            @PathVariable long sheetId,
            @PathVariable long studentId,
            @Valid @RequestBody CourseworkScoreRequest request,
            Authentication authentication
    ) {
        application.enterScore(sheetId, actors.teacher(authentication), new com.dev.csas.shared.StudentID(studentId),
                request.score());
    }

    @PostMapping("/close")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void close(@PathVariable long sheetId, Authentication authentication) {
        application.close(sheetId, actors.teacher(authentication));
    }

    @PostMapping("/reopen")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void reopen(@PathVariable long sheetId, @Valid @RequestBody CourseworkReopenRequest request,
                       Authentication authentication) {
        application.reopen(sheetId, actors.dean(authentication), request.semesterId());
    }

    @PostMapping("/archive")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archive(@PathVariable long sheetId, Authentication authentication) {
        application.archive(sheetId, actors.dean(authentication));
    }
}
