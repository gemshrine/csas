package com.dev.csas.teacher;

import com.dev.csas.course.CourseRepository;
import com.dev.csas.error.CatalogConflictException;
import com.dev.csas.error.CatalogNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class TeacherService {

    private final TeacherRepository teacherRepository;
    private final CourseRepository courseRepository;

    @Transactional(readOnly = true)
    public List<TeacherResponse> findAll() {
        return teacherRepository.findAll().stream()
                .map(TeacherResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public TeacherResponse findById(Long id) {
        return TeacherResponse.from(getTeacher(id));
    }

    @Transactional
    public TeacherResponse create(TeacherRequest request) {
        TeacherJPAEntity entity = TeacherJPAEntity.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .patronymic(request.patronymic())
                .department(request.department())
                .position(request.position())
                .academicDegree(request.academicDegree())
                .email(request.email())
                .phone(request.phone())
                .build();
        return TeacherResponse.from(teacherRepository.save(entity));
    }

    @Transactional
    public TeacherResponse update(Long id, TeacherRequest request) {
        TeacherJPAEntity entity = getTeacher(id);
        entity.setFirstName(request.firstName());
        entity.setLastName(request.lastName());
        entity.setPatronymic(request.patronymic());
        entity.setDepartment(request.department());
        entity.setPosition(request.position());
        entity.setAcademicDegree(request.academicDegree());
        entity.setEmail(request.email());
        entity.setPhone(request.phone());
        return TeacherResponse.from(teacherRepository.save(entity));
    }

    @Transactional
    public void delete(Long id) {
        getTeacher(id);
        if (courseRepository.existsByTeacher_Id(id)) {
            throw new CatalogConflictException(
                    "Нельзя удалить преподавателя: он назначен на курс"
            );
        }
        teacherRepository.deleteById(id);
    }

    private TeacherJPAEntity getTeacher(Long id) {
        return teacherRepository.findById(id)
                .orElseThrow(() -> CatalogNotFoundException.teacher(id));
    }
}
