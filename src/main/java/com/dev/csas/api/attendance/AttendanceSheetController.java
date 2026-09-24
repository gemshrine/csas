package com.dev.csas.api.attendance;

import com.dev.csas.attendance.AttendanceSheetApplicationService;
import com.dev.csas.security.CurrentActorResolver;
import com.dev.csas.shared.LectureID;
import com.dev.csas.shared.StudentID;
import org.springframework.http.HttpStatus;
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
@RequestMapping("/api/attendance-sheets/{sheetId}")
@Validated
public class AttendanceSheetController {

    private final AttendanceSheetApplicationService service;
    private final CurrentActorResolver actors;

    public AttendanceSheetController(AttendanceSheetApplicationService service, CurrentActorResolver actors) {
        this.service = service;
        this.actors = actors;
    }

    @PostMapping("/lectures")
    public ResponseEntity<LectureResponse> addLecture(
            @PathVariable long sheetId,
            @Valid @RequestBody AddLectureRequest request,
            Authentication authentication
    ) {
        LectureID lecture = service.addLecture(
                sheetId,
                actors.teacher(authentication),
                request.date(),
                request.title()
        );
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new LectureResponse(lecture.id(), request.date(), request.title()));
    }

    @PutMapping("/lectures/{lectureId}/attendance/{studentId}")
    public ResponseEntity<Void> registerAttendance(
            @PathVariable long sheetId,
            @PathVariable long lectureId,
            @PathVariable long studentId,
            @Valid @RequestBody RegisterAttendanceRequest request,
            Authentication authentication
    ) {
        service.registerAttendance(
                sheetId,
                actors.teacher(authentication),
                new StudentID(studentId),
                request.status(),
                new LectureID(lectureId)
        );
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/close")
    public ResponseEntity<Void> close(@PathVariable long sheetId, Authentication authentication) {
        service.close(sheetId, actors.teacher(authentication));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/archive")
    public ResponseEntity<Void> archive(@PathVariable long sheetId, Authentication authentication) {
        service.archive(sheetId, actors.dean(authentication));
        return ResponseEntity.noContent().build();
    }
}
