package com.dev.csas.student;

import com.dev.csas.attendance.LectureAttendanceRepository;
import com.dev.csas.control.ControlResultRepository;
import com.dev.csas.error.CatalogBadRequestException;
import com.dev.csas.error.CatalogConflictException;
import com.dev.csas.error.CatalogNotFoundException;
import com.dev.csas.group.GroupJPAEntity;
import com.dev.csas.group.GroupRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StudentService {

    private final StudentRepository studentRepository;
    private final GroupRepository groupRepository;
    private final ControlResultRepository controlResultRepository;
    private final LectureAttendanceRepository lectureAttendanceRepository;

    @Transactional(readOnly = true)
    public List<StudentResponse> findAll() {
        return studentRepository.findAll().stream()
                .map(StudentResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public StudentResponse findById(Long id) {
        return StudentResponse.from(getStudent(id));
    }

    @Transactional
    public StudentResponse create(StudentRequest request) {
        ensureRecordBookUnique(request.recordBookNumber(), null);
        StudentJPAEntity entity = StudentJPAEntity.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .patronymic(request.patronymic())
                .recordBookNumber(request.recordBookNumber())
                .birthDate(request.birthDate())
                .email(request.email())
                .phone(request.phone())
                .gender(request.gender())
                .educationForm(request.educationForm())
                .studyForm(request.studyForm())
                .admissionDate(request.admissionDate())
                .status(request.status())
                .group(resolveGroup(request.groupId()))
                .build();
        return StudentResponse.from(studentRepository.save(entity));
    }

    @Transactional
    public StudentResponse update(Long id, StudentRequest request) {
        StudentJPAEntity entity = getStudent(id);
        ensureRecordBookUnique(request.recordBookNumber(), id);
        entity.setFirstName(request.firstName());
        entity.setLastName(request.lastName());
        entity.setPatronymic(request.patronymic());
        entity.setRecordBookNumber(request.recordBookNumber());
        entity.setBirthDate(request.birthDate());
        entity.setEmail(request.email());
        entity.setPhone(request.phone());
        entity.setGender(request.gender());
        entity.setEducationForm(request.educationForm());
        entity.setStudyForm(request.studyForm());
        entity.setAdmissionDate(request.admissionDate());
        entity.setStatus(request.status());
        entity.setGroup(resolveGroup(request.groupId()));
        return StudentResponse.from(studentRepository.save(entity));
    }

    @Transactional
    public void delete(Long id) {
        getStudent(id);
        if (controlResultRepository.existsByStudent_Id(id)
                || lectureAttendanceRepository.existsByStudent_Id(id)) {
            throw new CatalogConflictException(
                    "Нельзя удалить студента: есть связанные результаты контроля или отметки посещаемости"
            );
        }
        studentRepository.deleteById(id);
    }

    private StudentJPAEntity getStudent(Long id) {
        return studentRepository.findById(id)
                .orElseThrow(() -> CatalogNotFoundException.student(id));
    }

    private GroupJPAEntity resolveGroup(Long groupId) {
        if (groupId == null) {
            return null;
        }
        return groupRepository.findById(groupId)
                .orElseThrow(() -> new CatalogBadRequestException(
                        "Группа с id " + groupId + " не найдена"
                ));
    }

    private void ensureRecordBookUnique(String recordBookNumber, Long currentId) {
        boolean taken = currentId == null
                ? studentRepository.existsByRecordBookNumber(recordBookNumber)
                : studentRepository.existsByRecordBookNumberAndIdNot(recordBookNumber, currentId);
        if (taken) {
            throw new CatalogConflictException(
                    "Студент с номером зачётки «" + recordBookNumber + "» уже существует"
            );
        }
    }
}
