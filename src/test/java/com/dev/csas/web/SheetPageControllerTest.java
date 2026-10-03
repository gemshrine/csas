package com.dev.csas.web;

import com.dev.csas.api.CourseHeaderResponse;
import com.dev.csas.api.attendance.AttendanceSheetDetailsResponse;
import com.dev.csas.api.control.ControlSheetDetailsResponse;
import com.dev.csas.attendance.AttendanceSheetQueryService;
import com.dev.csas.attendance.AttendanceSheetStatus;
import com.dev.csas.control.ControlSheetQueryService;
import com.dev.csas.control.ControlSheetStatus;
import com.dev.csas.course.CourseStatus;
import com.dev.csas.discipline.ControlFormEnum;
import com.dev.csas.security.CurrentActorResolver;
import com.dev.csas.shared.DeanID;
import com.dev.csas.shared.TeacherID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.ui.ExtendedModelMap;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SheetPageControllerTest {

    @Mock
    private ControlSheetQueryService controlSheets;

    @Mock
    private AttendanceSheetQueryService attendanceSheets;

    @Mock
    private CurrentActorResolver actors;

    @InjectMocks
    private SheetPageController controller;

    @Test
    void putsSeminarTeacherControlSheetIntoModel() {
        Authentication teacher = authentication("ROLE_TEACHER");
        ControlSheetDetailsResponse sheet = controlSheet();
        ExtendedModelMap model = new ExtendedModelMap();
        when(actors.teacher(teacher)).thenReturn(new TeacherID(12));
        when(controlSheets.getForTeacher(4, new TeacherID(12))).thenReturn(sheet);

        String view = controller.controlSheet(4, teacher, model);

        assertEquals("control-sheet", view);
        assertSame(sheet, model.getAttribute("sheet"));
        verify(controlSheets).getForTeacher(4, new TeacherID(12));
    }

    @Test
    void putsDeanControlSheetIntoModel() {
        Authentication dean = authentication("ROLE_DEAN");
        ControlSheetDetailsResponse sheet = controlSheet();
        ExtendedModelMap model = new ExtendedModelMap();
        when(actors.dean(dean)).thenReturn(new DeanID(8));
        when(controlSheets.getForDean(4)).thenReturn(sheet);

        String view = controller.controlSheet(4, dean, model);

        assertEquals("control-sheet", view);
        assertSame(sheet, model.getAttribute("sheet"));
        verify(controlSheets).getForDean(4);
    }

    @Test
    void putsLectureTeacherAttendanceSheetIntoModel() {
        Authentication teacher = authentication("ROLE_TEACHER");
        AttendanceSheetDetailsResponse sheet = attendanceSheet();
        ExtendedModelMap model = new ExtendedModelMap();
        when(actors.teacher(teacher)).thenReturn(new TeacherID(13));
        when(attendanceSheets.getForTeacher(5, new TeacherID(13))).thenReturn(sheet);

        String view = controller.attendanceSheet(5, teacher, model);

        assertEquals("attendance-sheet", view);
        assertSame(sheet, model.getAttribute("sheet"));
        verify(attendanceSheets).getForTeacher(5, new TeacherID(13));
    }

    @Test
    void putsDeanAttendanceSheetIntoModel() {
        Authentication dean = authentication("ROLE_DEAN");
        AttendanceSheetDetailsResponse sheet = attendanceSheet();
        ExtendedModelMap model = new ExtendedModelMap();
        when(actors.dean(dean)).thenReturn(new DeanID(8));
        when(attendanceSheets.getForDean(5)).thenReturn(sheet);

        String view = controller.attendanceSheet(5, dean, model);

        assertEquals("attendance-sheet", view);
        assertSame(sheet, model.getAttribute("sheet"));
        verify(attendanceSheets).getForDean(5);
    }

    private Authentication authentication(String role) {
        return UsernamePasswordAuthenticationToken.authenticated(
                "user", "unused", List.of(new SimpleGrantedAuthority(role))
        );
    }

    private ControlSheetDetailsResponse controlSheet() {
        return new ControlSheetDetailsResponse(
                4,
                new CourseHeaderResponse(3, "Базы данных", "ИСТ-21", "Осень 2026", "Весна 2027",
                        com.dev.csas.course.CourseType.SEMINAR, CourseStatus.IN_PROGRESS),
                ControlFormEnum.EXAM,
                ControlSheetStatus.OPEN,
                null,
                List.of()
        );
    }

    private AttendanceSheetDetailsResponse attendanceSheet() {
        return new AttendanceSheetDetailsResponse(
                5,
                new CourseHeaderResponse(3, "Базы данных", "ИСТ-21", "Осень 2026", "Весна 2027",
                        com.dev.csas.course.CourseType.LECTURE, CourseStatus.IN_PROGRESS),
                AttendanceSheetStatus.OPEN,
                List.of(),
                List.of()
        );
    }
}
