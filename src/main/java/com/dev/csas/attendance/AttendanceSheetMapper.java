package com.dev.csas.attendance;

import com.dev.csas.course.CourseMapper;
import com.dev.csas.shared.LectureID;
import com.dev.csas.shared.StudentID;
import com.dev.csas.student.StudentJPAEntity;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class AttendanceSheetMapper {

    private final CourseMapper courseMapper;

    public AttendanceSheetMapper(CourseMapper courseMapper) {
        this.courseMapper = courseMapper;
    }

    public AttendanceSheet toDomain(AttendanceSheetJPAEntity entity) {
        Objects.requireNonNull(entity, "Ведомость посещаемости не может быть пустой");

        List<Lecture> lectures = entity.getLectures().stream()
                .map(lecture -> new Lecture(
                        new LectureID(lecture.getId()),
                        lecture.getLectureDate(),
                        lecture.getTitle()
                ))
                .toList();

        Map<LectureID, Map<StudentID, AttendanceRecord>> attendances = new HashMap<>();
        entity.getLectures().forEach(lecture -> {
            LectureID lectureID = new LectureID(lecture.getId());
            Map<StudentID, AttendanceRecord> records = lecture.getAttendances().stream()
                    .collect(Collectors.toMap(
                            record -> new StudentID(record.getStudent().getId()),
                            record -> new AttendanceRecord(record.isWasPresent()
                                    ? AttendanceStatusEnum.PRESENT
                                    : AttendanceStatusEnum.ABSENT)
                    ));
            if (!records.isEmpty()) {
                attendances.put(lectureID, records);
            }
        });

        return AttendanceSheet.reconstitute(
                courseMapper.toDomain(entity.getCourse()),
                entity.getStatus(),
                lectures,
                attendances
        );
    }

    public void updateEntity(AttendanceSheet source, AttendanceSheetJPAEntity target) {
        Objects.requireNonNull(source, "Доменная ведомость посещаемости не может быть пустой");
        Objects.requireNonNull(target, "JPA-ведомость посещаемости не может быть пустой");

        target.setStatus(source.status());

        Map<LectureID, LectureJPAEntity> lectures = target.getLectures().stream()
                .collect(Collectors.toMap(lecture -> new LectureID(lecture.getId()), Function.identity()));
        Map<StudentID, StudentJPAEntity> students = target.getCourse().getStudents().stream()
                .collect(Collectors.toMap(student -> new StudentID(student.getId()), Function.identity()));
        Map<LectureID, Map<StudentID, AttendanceRecord>> recordsByLecture = source.attendances();
        Set<LectureID> retainedLectures = new HashSet<>();

        for (Lecture lecture : source.lectures()) {
            LectureID lectureID = lecture.getId();
            LectureJPAEntity lectureEntity = Optional.ofNullable(lectures.get(lectureID))
                    .orElseThrow(() -> new IllegalStateException(
                            "Лекция не найдена в JPA-ведомости: " + lectureID.id()
                    ));
            retainedLectures.add(lectureID);
            lectureEntity.setLectureDate(lecture.getDate());
            lectureEntity.setTitle(lecture.getTitle());

            Map<StudentID, AttendanceRecord> records = recordsByLecture.getOrDefault(lectureID, Map.of());
            Map<StudentID, LectureAttendanceJPAEntity> currentRecords = lectureEntity.getAttendances().stream()
                    .collect(Collectors.toMap(
                            record -> new StudentID(record.getStudent().getId()),
                            Function.identity()
                    ));
            Set<StudentID> retainedStudents = new HashSet<>();

            records.forEach((studentID, record) -> {
                StudentJPAEntity student = Optional.ofNullable(students.get(studentID))
                        .orElseThrow(() -> new IllegalStateException(
                                "Студент отметки не найден в составе курса: " + studentID.id()
                        ));
                retainedStudents.add(studentID);

                LectureAttendanceJPAEntity recordEntity = currentRecords.get(studentID);
                if (recordEntity == null) {
                    recordEntity = LectureAttendanceJPAEntity.builder()
                            .lecture(lectureEntity)
                            .student(student)
                            .build();
                    lectureEntity.getAttendances().add(recordEntity);
                }
                recordEntity.setWasPresent(record.status() == AttendanceStatusEnum.PRESENT);
            });

            lectureEntity.getAttendances().removeIf(record ->
                    !retainedStudents.contains(new StudentID(record.getStudent().getId())));
        }

        target.getLectures().removeIf(lecture ->
                !retainedLectures.contains(new LectureID(lecture.getId())));
    }
}
