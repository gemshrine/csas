package com.dev.csas.report;

import com.dev.csas.api.attendance.AttendanceSheetDetailsResponse;
import com.dev.csas.api.attendance.AttendanceStudentResponse;
import com.dev.csas.api.control.ControlSheetDetailsResponse;
import com.dev.csas.api.control.ControlStudentResultResponse;
import com.dev.csas.api.course.CourseDetailsResponse;
import com.dev.csas.api.course.CourseDetailsResponse.CourseStudentResponse;
import com.dev.csas.api.course.StudentCourseSummaryResponse;
import com.dev.csas.attendance.AttendanceSheetQueryService;
import com.dev.csas.attendance.AttendanceSheetRepository;
import com.dev.csas.attendance.AttendanceStatusEnum;
import com.dev.csas.control.ControlSheetQueryService;
import com.dev.csas.control.ControlSheetStatus;
import com.dev.csas.control.entity.ControlSheetRepository;
import com.dev.csas.course.CourseApplicationService;
import com.dev.csas.course.CourseJPAEntity;
import com.dev.csas.course.CourseRepository;
import com.dev.csas.course.CourseType;
import com.dev.csas.course.StudentCourseQueryService;
import com.dev.csas.coursework.CourseworkGrade;
import com.dev.csas.coursework.CourseworkSheetDetailsResponse;
import com.dev.csas.coursework.CourseworkSheetQueryService;
import com.dev.csas.coursework.CourseworkSheetRepository;
import com.dev.csas.coursework.CourseworkSheetStatus;
import com.dev.csas.error.CatalogNotFoundException;
import com.dev.csas.group.GroupJPAEntity;
import com.dev.csas.group.GroupRepository;
import com.dev.csas.security.ActorAccessDeniedException;
import com.dev.csas.shared.StudentID;
import com.dev.csas.shared.TeacherID;
import com.dev.csas.student.StudentJPAEntity;
import com.dev.csas.student.StudentRepository;
import com.dev.csas.student.StudentReadModel;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ReportExportService {

    private final XlsxReportWriter writer;
    private final CourseApplicationService courses;
    private final CourseRepository courseRepository;
    private final GroupRepository groupRepository;
    private final StudentRepository studentRepository;
    private final StudentCourseQueryService studentCourses;
    private final ControlSheetRepository controlSheetRepository;
    private final ControlSheetQueryService controlSheets;
    private final AttendanceSheetRepository attendanceSheetRepository;
    private final AttendanceSheetQueryService attendanceSheets;
    private final CourseworkSheetRepository courseworkSheetRepository;
    private final CourseworkSheetQueryService courseworkSheets;

    public ReportExportService(
            XlsxReportWriter writer,
            CourseApplicationService courses,
            CourseRepository courseRepository,
            GroupRepository groupRepository,
            StudentRepository studentRepository,
            StudentCourseQueryService studentCourses,
            ControlSheetRepository controlSheetRepository,
            ControlSheetQueryService controlSheets,
            AttendanceSheetRepository attendanceSheetRepository,
            AttendanceSheetQueryService attendanceSheets,
            CourseworkSheetRepository courseworkSheetRepository,
            CourseworkSheetQueryService courseworkSheets
    ) {
        this.writer = writer;
        this.courses = courses;
        this.courseRepository = courseRepository;
        this.groupRepository = groupRepository;
        this.studentRepository = studentRepository;
        this.studentCourses = studentCourses;
        this.controlSheetRepository = controlSheetRepository;
        this.controlSheets = controlSheets;
        this.attendanceSheetRepository = attendanceSheetRepository;
        this.attendanceSheets = attendanceSheets;
        this.courseworkSheetRepository = courseworkSheetRepository;
        this.courseworkSheets = courseworkSheets;
    }

    @Transactional(readOnly = true)
    public byte[] course(long courseId, TeacherID teacher, boolean dean) {
        CourseDetailsResponse course = dean
                ? courses.getForDean(courseId)
                : courses.getForTeacher(courseId, teacher);
        return switch (course.courseType()) {
            case SEMINAR -> seminarCourse(course, teacher, dean);
            case LECTURE -> lectureCourse(course, teacher, dean);
            case COURSEWORK -> courseworkCourse(course, teacher, dean);
        };
    }

    @Transactional(readOnly = true)
    public byte[] attendance(long sheetId, TeacherID teacher, boolean dean) {
        AttendanceSheetDetailsResponse sheet = dean
                ? attendanceSheets.getForDean(sheetId)
                : attendanceSheets.getForTeacher(sheetId, teacher);
        return writer.write(List.of(attendanceSheet(sheet)));
    }

    @Transactional(readOnly = true)
    public byte[] coursework(long sheetId, TeacherID teacher, boolean dean) {
        CourseworkSheetDetailsResponse sheet = dean
                ? courseworkSheets.getForDean(sheetId)
                : courseworkSheets.getForTeacher(sheetId, teacher);
        return writer.write(List.of(courseworkSheet(sheet)));
    }

    @Transactional(readOnly = true)
    public byte[] studentAcademicRecord(long studentId) {
        StudentJPAEntity student = studentRepository.findById(studentId)
                .orElseThrow(() -> CatalogNotFoundException.student(studentId));
        List<StudentCourseSummaryResponse> records = studentCourses.list(new StudentID(studentId));
        List<List<Object>> rows = records.stream()
                .map(record -> row(
                        record.courseId(), record.groupName(), record.disciplineName(), courseTypeLabel(record.courseType()),
                        record.startSemesterName(), record.endSemesterName(), courseStatusLabel(record.status()),
                        nullable(record.rating()), nullable(record.courseworkScore()),
                        courseworkGradeLabel(record.courseworkGrade()), controlForm(record), controlScore(record),
                        passFail(record), nullable(controlFinalScore(record)), nullable(controlFinalGrade(record))
                ))
                .toList();
        String name = displayName(student);
        return writer.write(List.of(new XlsxReportSheet(
                "Успеваемость",
                "Сводная успеваемость студента",
                List.of("Студент: " + name, "Номер зачётной книжки: " + student.getRecordBookNumber(),
                        "Дата формирования: " + LocalDate.now()),
                List.of("ID курса", "Группа", "Дисциплина", "Тип курса", "Семестр начала", "Семестр окончания",
                        "Состояние курса", "Рейтинг, %", "Балл за курсовую", "Оценка за курсовую",
                        "Форма контроля", "Результат контроля", "Зачёт", "Итоговый балл", "Итоговая оценка"),
                rows
        )));
    }

    @Transactional(readOnly = true)
    public byte[] groupRatings(long groupId) {
        GroupJPAEntity group = groupRepository.findById(groupId)
                .orElseThrow(() -> CatalogNotFoundException.group(groupId));
        List<List<Object>> rows = new ArrayList<>();
        for (CourseJPAEntity course : courseRepository.findAllByGroup_IdOrderByIdAsc(groupId)) {
            if (course.getCourseType() != CourseType.SEMINAR) {
                continue;
            }
            CourseDetailsResponse details = courses.getForDean(course.getId());
            List<CourseStudentResponse> ranked = details.students().stream()
                    .sorted(Comparator.comparing(CourseStudentResponse::rating).reversed()
                            .thenComparing(CourseStudentResponse::name))
                    .toList();
            for (int index = 0; index < ranked.size(); index++) {
                CourseStudentResponse student = ranked.get(index);
                rows.add(List.of(details.courseId(), index + 1, student.name(), details.disciplineName(),
                        details.startSemesterName(), details.endSemesterName(), nullable(student.rating())));
            }
        }
        return writer.write(List.of(new XlsxReportSheet(
                "Рейтинг группы",
                "Рейтинговый список группы",
                List.of("Группа: " + group.getName(), "Специальность: " + group.getSpecialty(),
                        "Дата формирования: " + LocalDate.now()),
                List.of("ID курса", "Место в курсе", "Студент", "Дисциплина", "Семестр начала",
                        "Семестр окончания", "Рейтинг, %"),
                rows
        )));
    }

    @Transactional(readOnly = true)
    public byte[] debtors() {
        List<CourseJPAEntity> allCourses = courseRepository.findAllByOrderByIdAsc();
        Map<Long, CourseJPAEntity> courseById = allCourses.stream()
                .collect(Collectors.toMap(CourseJPAEntity::getId, Function.identity()));
        Map<Long, CourseworkSheetStatus> courseworkStatuses = allCourses.isEmpty() ? Map.of()
                : courseworkSheetRepository.findCourseSheetReadModels(
                        allCourses.stream().map(CourseJPAEntity::getId).toList()).stream()
                .collect(Collectors.toMap(model -> model.courseId(), model -> model.status()));
        List<List<Object>> rows = new ArrayList<>();
        for (StudentJPAEntity student : studentRepository.findAll()) {
            for (StudentCourseSummaryResponse record : studentCourses.list(new StudentID(student.getId()))) {
                CourseJPAEntity course = courseById.get(record.courseId());
                if (course == null) {
                    continue;
                }
                if (isControlDebtor(record)) {
                    rows.add(debtorRow(student, record, "Контроль", controlResultLabel(record)));
                }
                if (isCourseworkDebtor(record, courseworkStatuses.get(record.courseId()))) {
                    rows.add(debtorRow(student, record, "Курсовая работа", record.courseworkScore() + " баллов"));
                }
            }
        }
        return writer.write(List.of(new XlsxReportSheet(
                "Должники",
                "Список должников",
                List.of("Включены только явно зафиксированные неудовлетворительные результаты закрытых ведомостей",
                        "Дата формирования: " + LocalDate.now()),
                List.of("Студент", "Номер зачётной книжки", "Группа", "Дисциплина", "Тип курса",
                        "Семестр начала", "Семестр окончания", "Основание задолженности", "Результат"),
                rows
        )));
    }

    @Transactional(readOnly = true)
    public byte[] statistics() {
        List<CourseJPAEntity> allCourses = courseRepository.findAllByOrderByIdAsc();
        Map<Long, CourseJPAEntity> courseById = allCourses.stream()
                .collect(Collectors.toMap(CourseJPAEntity::getId, Function.identity()));
        Map<Long, CourseworkSheetStatus> courseworkStatusByCourse = allCourses.isEmpty() ? Map.of()
                : courseworkSheetRepository.findCourseSheetReadModels(
                        allCourses.stream().map(CourseJPAEntity::getId).toList()).stream()
                .collect(Collectors.toMap(model -> model.courseId(), model -> model.status()));
        Map<Long, StatisticsCount> byCourse = new LinkedHashMap<>();
        Map<Long, StatisticsCount> byGroup = new LinkedHashMap<>();
        Map<String, StatisticsCount> bySpecialty = new LinkedHashMap<>();
        allCourses.forEach(course -> {
            byCourse.put(course.getId(), new StatisticsCount());
            byGroup.computeIfAbsent(course.getGroup().getId(), ignored -> new StatisticsCount());
            bySpecialty.computeIfAbsent(course.getGroup().getSpecialty(), ignored -> new StatisticsCount());
        });

        for (StudentJPAEntity student : studentRepository.findAll()) {
            for (StudentCourseSummaryResponse record : studentCourses.list(new StudentID(student.getId()))) {
                CourseJPAEntity course = courseById.get(record.courseId());
                if (course == null) {
                    continue;
                }
                Integer grade = finalGrade(record, courseworkStatusByCourse.get(course.getId()));
                if (grade != null) {
                    byCourse.get(course.getId()).add(grade);
                    byGroup.get(course.getGroup().getId()).add(grade);
                    bySpecialty.get(course.getGroup().getSpecialty()).add(grade);
                }
            }
        }

        List<List<Object>> groupRows = groupRepository.findAll().stream()
                .sorted(Comparator.comparing(GroupJPAEntity::getName))
                .map(group -> {
                    StatisticsCount count = byGroup.getOrDefault(group.getId(), new StatisticsCount());
                    long studentCount = studentRepository.findAllByGroup_IdOrderByIdAsc(group.getId()).size();
                    long courseCount = allCourses.stream()
                            .filter(course -> course.getGroup().getId().equals(group.getId())).count();
                    return row(group.getName(), group.getSpecialty(), studentCount, courseCount,
                            count.grades, count.highGrades, count.quality());
                })
                .toList();
        List<List<Object>> courseRows = allCourses.stream()
                .map(course -> {
                    StatisticsCount count = byCourse.get(course.getId());
                    return row(course.getId(), course.getGroup().getName(),
                            course.getDiscipline().getName(), courseTypeLabel(course.getCourseType()),
                            course.getStartSemester().getName(), course.getEndSemester().getName(),
                            courseStatusLabel(course.getStatus()), course.getStudents().size(), count.grades,
                            count.highGrades, count.quality());
                })
                .toList();
        List<List<Object>> specialtyRows = bySpecialty.entrySet().stream()
                .sorted(Map.Entry.comparingByKey())
                .map(entry -> {
                    long groupCount = groupRepository.findAll().stream()
                            .filter(group -> group.getSpecialty().equals(entry.getKey())).count();
                    long studentCount = studentRepository.findAll().stream()
                            .filter(student -> student.getGroup() != null
                                    && student.getGroup().getSpecialty().equals(entry.getKey())).count();
                    long courseCount = allCourses.stream()
                            .filter(course -> course.getGroup().getSpecialty().equals(entry.getKey())).count();
                    StatisticsCount count = entry.getValue();
                    return row(entry.getKey(), groupCount, studentCount, courseCount,
                            count.grades, count.highGrades, count.quality());
                })
                .toList();
        List<List<Object>> qualityRows = List.of(row("Выпускные оценки по экзаменам, дифзачётам и курсовым работам",
                byCourse.values().stream().mapToLong(count -> count.grades).sum(),
                byCourse.values().stream().mapToLong(count -> count.highGrades).sum(),
                overallQuality(byCourse.values())));

        return writer.write(List.of(
                new XlsxReportSheet("По группам", "Статистика по группам",
                        List.of("Дата формирования: " + LocalDate.now()),
                        List.of("Группа", "Специальность", "Студентов", "Курсов", "Итоговых оценок",
                                "Оценок 4 и 5", "Качество знаний, %"), groupRows),
                new XlsxReportSheet("По курсам", "Статистика по курсам",
                        List.of("Дата формирования: " + LocalDate.now()),
                        List.of("ID курса", "Группа", "Дисциплина", "Тип курса", "Семестр начала",
                                "Семестр окончания", "Состояние", "Студентов", "Итоговых оценок",
                                "Оценок 4 и 5", "Качество знаний, %"), courseRows),
                new XlsxReportSheet("По специальностям", "Статистика по специальностям",
                        List.of("Дата формирования: " + LocalDate.now()),
                        List.of("Специальность", "Групп", "Студентов", "Курсов", "Итоговых оценок",
                                "Оценок 4 и 5", "Качество знаний, %"), specialtyRows),
                new XlsxReportSheet("Качество знаний", "Качество знаний",
                        List.of("Учитываются итоговые оценки по экзаменам, дифзачётам и курсовым работам",
                                "Зачёт и незачёт в расчёт качества знаний не входят",
                                "Дата формирования: " + LocalDate.now()),
                        List.of("Источник оценок", "Итоговых оценок", "Оценок 4 и 5", "Качество знаний, %"),
                        qualityRows)
        ));
    }

    private byte[] seminarCourse(CourseDetailsResponse course, TeacherID teacher, boolean dean) {
        ControlSheetDetailsResponse control = controlSheetRepository.findByCourse_Id(course.courseId())
                .map(sheet -> dean ? controlSheets.getForDean(sheet.getId())
                        : controlSheets.getForTeacher(sheet.getId(), teacher))
                .orElse(null);
        Map<Long, ControlStudentResultResponse> controlByStudent = control == null ? Map.of()
                : control.students().stream().collect(Collectors.toMap(
                        ControlStudentResultResponse::studentId, Function.identity()));
        List<String> columns = new ArrayList<>(List.of("Студент"));
        course.works().forEach(work -> columns.add(work.title() + " (макс. " + work.maxScore() + ")"));
        columns.add("Рейтинг, %");
        columns.add("Форма контроля");
        columns.add("Результат контроля");
        columns.add("Итоговый балл");
        columns.add("Итоговая оценка");
        List<List<Object>> rows = course.students().stream()
                .map(student -> {
                    List<Object> row = new ArrayList<>();
                    row.add(student.name());
                    for (var work : course.works()) {
                        row.add(work.scores().stream()
                                .filter(score -> score.studentId() == student.studentId())
                                .map(com.dev.csas.api.course.CourseDetailsResponse.CourseWorkScoreResponse::score)
                                .findFirst().orElse(null));
                    }
                    row.add(student.rating());
                    ControlStudentResultResponse result = controlByStudent.get(student.studentId());
                    row.add(control == null ? null : controlFormLabel(control.controlForm()));
                    row.add(result == null ? null : controlValue(result));
                    row.add(result == null ? null : result.finalScore());
                    row.add(result == null ? null : result.finalGrade());
                    return row;
                })
                .toList();
        return writer.write(List.of(new XlsxReportSheet(
                "Итоговая ведомость", "Ведомость курса",
                courseContext(course), columns, rows
        )));
    }

    private byte[] lectureCourse(CourseDetailsResponse course, TeacherID teacher, boolean dean) {
        AttendanceSheetDetailsResponse sheet = attendanceSheetRepository.findByCourse_Id(course.courseId())
                .map(entity -> dean ? attendanceSheets.getForDean(entity.getId())
                        : attendanceSheets.getForTeacher(entity.getId(), teacher))
                .orElse(null);
        XlsxReportSheet reportSheet = sheet == null
                ? new XlsxReportSheet("Посещаемость", "Ведомость посещаемости",
                        courseContext(course), List.of("Студент"),
                        course.students().stream().map(student -> List.<Object>of(student.name())).toList())
                : attendanceSheet(sheet);
        return writer.write(List.of(reportSheet));
    }

    private byte[] courseworkCourse(CourseDetailsResponse course, TeacherID teacher, boolean dean) {
        CourseworkSheetDetailsResponse sheet = courseworkSheetRepository.findByCourse_Id(course.courseId())
                .map(entity -> dean ? courseworkSheets.getForDean(entity.getId())
                        : courseworkSheets.getForTeacher(entity.getId(), teacher))
                .orElse(null);
        XlsxReportSheet reportSheet = sheet == null
                ? new XlsxReportSheet("Курсовая работа", "Результаты курсовых работ",
                        courseContext(course), List.of("Студент", "Балл", "Оценка"), List.of())
                : courseworkSheet(sheet);
        return writer.write(List.of(reportSheet));
    }

    private XlsxReportSheet attendanceSheet(AttendanceSheetDetailsResponse sheet) {
        List<String> columns = new ArrayList<>(List.of("Студент"));
        sheet.lectures().forEach(lecture -> columns.add(lecture.date() + " " + lecture.title()));
        List<List<Object>> rows = sheet.students().stream()
                .map(student -> {
                    List<Object> row = new ArrayList<>();
                    row.add(student.studentName());
                    sheet.lectures().forEach(lecture -> {
                        AttendanceStudentResponse mark = lecture.attendance().stream()
                                .filter(attendance -> attendance.studentId() == student.studentId())
                                .findFirst().orElse(null);
                        row.add(mark == null || mark.status() == null
                                ? "Не отмечено"
                                : mark.status() == AttendanceStatusEnum.PRESENT ? "Был" : "Не был");
                    });
                    return row;
                })
                .toList();
        return new XlsxReportSheet("Посещаемость", "Ведомость посещаемости",
                context(sheet.course().groupName(), sheet.course().disciplineName(),
                        sheet.course().startSemesterName(), sheet.course().endSemesterName(), sheetStatusLabel(sheet.status())),
                columns, rows);
    }

    private XlsxReportSheet courseworkSheet(CourseworkSheetDetailsResponse sheet) {
        List<List<Object>> rows = sheet.results().stream()
                .map(result -> row(result.studentName(), nullable(result.score()), courseworkGradeLabel(result.grade())))
                .toList();
        return new XlsxReportSheet("Курсовая работа", "Результаты курсовых работ",
                context(sheet.course().groupName(), sheet.course().disciplineName(),
                        sheet.course().startSemesterName(), sheet.course().endSemesterName(), sheetStatusLabel(sheet.status())),
                List.of("Студент", "Балл из 100", "Оценка"), rows);
    }

    private List<Object> debtorRow(
            StudentJPAEntity student,
            StudentCourseSummaryResponse record,
            String reason,
            String result
    ) {
        return row(displayName(student), student.getRecordBookNumber(), record.groupName(),
                record.disciplineName(), courseTypeLabel(record.courseType()), record.startSemesterName(),
                record.endSemesterName(), reason, result);
    }

    private boolean isControlDebtor(StudentCourseSummaryResponse record) {
        if (record.controlResult() == null
                || record.controlResult().sheetStatus() == ControlSheetStatus.OPEN) {
            return false;
        }
        return record.controlResult().passFail() != null
                ? record.controlResult().passFail() == com.dev.csas.control.PassFail.FAIL
                : record.controlResult().finalGrade() != null && record.controlResult().finalGrade() < 3;
    }

    private boolean isCourseworkDebtor(
            StudentCourseSummaryResponse record,
            CourseworkSheetStatus courseworkStatus
    ) {
        return record.courseType() == CourseType.COURSEWORK
                && record.status() == com.dev.csas.course.CourseStatus.COMPLETED
                && courseworkStatus != null
                && courseworkStatus != CourseworkSheetStatus.OPEN
                && record.courseworkGrade() == CourseworkGrade.NOT_PASSED;
    }

    private String controlResultLabel(StudentCourseSummaryResponse record) {
        if (record.controlResult().passFail() != null) {
            return passFailLabel(record.controlResult().passFail());
        }
        return "Балл: " + record.controlResult().numericScore()
                + ", итог: " + record.controlResult().finalScore()
                + ", оценка: " + record.controlResult().finalGrade();
    }

    private Integer finalGrade(
            StudentCourseSummaryResponse record,
            CourseworkSheetStatus courseworkStatus
    ) {
        if (record.controlResult() != null
                && record.controlResult().sheetStatus() != ControlSheetStatus.OPEN
                && record.controlResult().finalGrade() != null) {
            return record.controlResult().finalGrade();
        }
        if (record.courseType() == CourseType.COURSEWORK
                && courseworkStatus != null
                && courseworkStatus != CourseworkSheetStatus.OPEN
                && record.courseworkGrade() != null) {
            return switch (record.courseworkGrade()) {
                case FIVE -> 5;
                case FOUR -> 4;
                case THREE -> 3;
                case NOT_PASSED -> 2;
            };
        }
        return null;
    }

    private Double overallQuality(Iterable<StatisticsCount> counts) {
        long grades = 0;
        long highGrades = 0;
        for (StatisticsCount count : counts) {
            grades += count.grades;
            highGrades += count.highGrades;
        }
        return grades == 0 ? null : highGrades * 100.0 / grades;
    }

    private List<String> courseContext(CourseDetailsResponse course) {
        return context(course.groupName(), course.disciplineName(), course.startSemesterName(),
                course.endSemesterName(), courseStatusLabel(course.status()));
    }

    private List<String> context(String group, String discipline, String start, String end, String status) {
        return List.of("Группа: " + group, "Дисциплина: " + discipline,
                "Период: " + start + " - " + end, "Состояние ведомости: " + status,
                "Дата формирования: " + LocalDate.now());
    }

    private List<Object> row(Object... values) {
        return Arrays.asList(values);
    }

    private Object controlForm(StudentCourseSummaryResponse record) {
        return record.controlResult() == null ? null : controlFormLabel(record.controlResult().controlForm());
    }

    private Object controlScore(StudentCourseSummaryResponse record) {
        return record.controlResult() == null ? null : nullable(record.controlResult().numericScore());
    }

    private Object passFail(StudentCourseSummaryResponse record) {
        return record.controlResult() == null || record.controlResult().passFail() == null
                ? null : passFailLabel(record.controlResult().passFail());
    }

    private Double controlFinalScore(StudentCourseSummaryResponse record) {
        return record.controlResult() == null ? null : record.controlResult().finalScore();
    }

    private Integer controlFinalGrade(StudentCourseSummaryResponse record) {
        return record.controlResult() == null ? null : record.controlResult().finalGrade();
    }

    private Object nullable(Number value) {
        return value;
    }

    private Object courseworkGradeLabel(CourseworkGrade grade) {
        if (grade == null) {
            return null;
        }
        return switch (grade) {
            case FIVE -> 5;
            case FOUR -> 4;
            case THREE -> 3;
            case NOT_PASSED -> "Не сдано";
        };
    }

    private Object controlValue(ControlStudentResultResponse result) {
        if (result.passFail() != null) {
            return passFailLabel(result.passFail());
        }
        return result.numericScore();
    }

    private String controlFormLabel(com.dev.csas.discipline.ControlFormEnum form) {
        return switch (form) {
            case EXAM -> "Экзамен";
            case DIFF_CREDIT -> "Дифференцированный зачёт";
            case CREDIT -> "Зачёт";
        };
    }

    private String passFailLabel(com.dev.csas.control.PassFail result) {
        return result == com.dev.csas.control.PassFail.PASS ? "Зачёт" : "Незачёт";
    }

    private String courseTypeLabel(CourseType type) {
        return switch (type) {
            case SEMINAR -> "Практика";
            case LECTURE -> "Лекции";
            case COURSEWORK -> "Курсовая работа";
        };
    }

    private String courseStatusLabel(com.dev.csas.course.CourseStatus status) {
        return status == com.dev.csas.course.CourseStatus.COMPLETED ? "Завершён" : "В работе";
    }

    private String sheetStatusLabel(Enum<?> status) {
        return switch (status.name()) {
            case "OPEN" -> "Открыта";
            case "CLOSED" -> "Закрыта";
            case "ARCHIVED" -> "В архиве";
            default -> status.name();
        };
    }

    private String displayName(StudentJPAEntity student) {
        return student.getLastName() + " " + student.getFirstName()
                + (student.getPatronymic() == null || student.getPatronymic().isBlank()
                ? "" : " " + student.getPatronymic());
    }

    private static final class StatisticsCount {
        private long grades;
        private long highGrades;

        private void add(int grade) {
            grades++;
            if (grade >= 4) {
                highGrades++;
            }
        }

        private Double quality() {
            return grades == 0 ? null : highGrades * 100.0 / grades;
        }
    }
}
