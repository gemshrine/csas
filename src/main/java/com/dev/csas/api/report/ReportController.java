package com.dev.csas.api.report;

import com.dev.csas.report.ReportExportService;
import com.dev.csas.security.ActorAccessDeniedException;
import com.dev.csas.security.CurrentActorResolver;
import com.dev.csas.shared.TeacherID;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/reports")
public class ReportController {

    private static final MediaType XLSX_MEDIA_TYPE = MediaType.parseMediaType(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
    );

    private final ReportExportService reports;
    private final CurrentActorResolver actors;

    public ReportController(ReportExportService reports, CurrentActorResolver actors) {
        this.reports = reports;
        this.actors = actors;
    }

    @GetMapping("/courses/{courseId}/xlsx")
    public ResponseEntity<byte[]> course(@PathVariable long courseId, Authentication authentication) {
        boolean dean = isDean(authentication);
        TeacherID teacher = dean ? null : actors.teacher(authentication);
        return xlsx("course-" + courseId + ".xlsx", reports.course(courseId, teacher, dean));
    }

    @GetMapping("/attendance-sheets/{sheetId}/xlsx")
    public ResponseEntity<byte[]> attendance(@PathVariable long sheetId, Authentication authentication) {
        boolean dean = isDean(authentication);
        TeacherID teacher = dean ? null : actors.teacher(authentication);
        return xlsx("attendance-" + sheetId + ".xlsx", reports.attendance(sheetId, teacher, dean));
    }

    @GetMapping("/coursework-sheets/{sheetId}/xlsx")
    public ResponseEntity<byte[]> coursework(@PathVariable long sheetId, Authentication authentication) {
        boolean dean = isDean(authentication);
        TeacherID teacher = dean ? null : actors.teacher(authentication);
        return xlsx("coursework-" + sheetId + ".xlsx", reports.coursework(sheetId, teacher, dean));
    }

    @GetMapping("/students/me/academic-record.xlsx")
    public ResponseEntity<byte[]> myAcademicRecord(Authentication authentication) {
        long studentId = actors.student(authentication).id();
        return xlsx("student-academic-record.xlsx", reports.studentAcademicRecord(studentId));
    }

    @GetMapping("/students/{studentId}/academic-record.xlsx")
    public ResponseEntity<byte[]> studentAcademicRecord(
            @PathVariable long studentId,
            Authentication authentication
    ) {
        if (isDean(authentication)) {
            actors.dean(authentication);
        } else if (actors.student(authentication).id() != studentId) {
            throw new ActorAccessDeniedException("Студент может выгрузить только собственную успеваемость");
        }
        return xlsx("student-" + studentId + "-academic-record.xlsx",
                reports.studentAcademicRecord(studentId));
    }

    @GetMapping("/groups/{groupId}/ratings.xlsx")
    public ResponseEntity<byte[]> groupRatings(@PathVariable long groupId, Authentication authentication) {
        actors.dean(authentication);
        return xlsx("group-" + groupId + "-ratings.xlsx", reports.groupRatings(groupId));
    }

    @GetMapping("/debtors.xlsx")
    public ResponseEntity<byte[]> debtors(Authentication authentication) {
        actors.dean(authentication);
        return xlsx("debtors.xlsx", reports.debtors());
    }

    @GetMapping("/statistics.xlsx")
    public ResponseEntity<byte[]> statistics(Authentication authentication) {
        actors.dean(authentication);
        return xlsx("statistics.xlsx", reports.statistics());
    }

    private boolean isDean(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_DEAN"));
    }

    private ResponseEntity<byte[]> xlsx(String filename, byte[] bytes) {
        return ResponseEntity.ok()
                .contentType(XLSX_MEDIA_TYPE)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(filename, StandardCharsets.UTF_8).build().toString())
                .body(bytes);
    }
}
