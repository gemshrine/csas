package com.dev.csas.api.control;

import com.dev.csas.control.ControlSheetApplicationService;
import com.dev.csas.control.PassFail;
import com.dev.csas.security.CurrentActorResolver;
import com.dev.csas.shared.StudentID;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/control-sheets/{sheetId}")
@Validated
public class ControlSheetController {

    private final ControlSheetApplicationService service;
    private final CurrentActorResolver actors;

    public ControlSheetController(ControlSheetApplicationService service, CurrentActorResolver actors) {
        this.service = service;
        this.actors = actors;
    }

    @PutMapping("/results/{studentId}/numeric")
    public ResponseEntity<Void> enterNumericResult(
            @PathVariable long sheetId,
            @PathVariable long studentId,
            @Valid @RequestBody NumericResultRequest request,
            Authentication authentication
    ) {
        service.enterNumericResult(
                sheetId,
                actors.teacher(authentication),
                new StudentID(studentId),
                request.score(),
                request.reason()
        );
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/results/{studentId}/pass-fail")
    public ResponseEntity<Void> enterPassFailResult(
            @PathVariable long sheetId,
            @PathVariable long studentId,
            @Valid @RequestBody PassFailResultRequest request,
            Authentication authentication
    ) {
        service.enterPassFailResult(
                sheetId,
                actors.teacher(authentication),
                new StudentID(studentId),
                request.result(),
                request.reason()
        );
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/close")
    public ResponseEntity<Void> close(@PathVariable long sheetId, Authentication authentication) {
        service.close(sheetId, actors.teacher(authentication));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/reopen")
    public ResponseEntity<Void> reopen(
            @PathVariable long sheetId,
            @Valid @RequestBody(required = false) ReasonRequest request,
            Authentication authentication
    ) {
        service.reopen(sheetId, actors.dean(authentication), request == null ? null : request.reason());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/archive")
    public ResponseEntity<Void> archive(
            @PathVariable long sheetId,
            @Valid @RequestBody(required = false) ReasonRequest request,
            Authentication authentication
    ) {
        service.archive(sheetId, actors.dean(authentication), request == null ? null : request.reason());
        return ResponseEntity.noContent().build();
    }
}
