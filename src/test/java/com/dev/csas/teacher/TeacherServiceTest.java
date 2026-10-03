package com.dev.csas.teacher;

import com.dev.csas.course.CourseRepository;
import com.dev.csas.error.CatalogConflictException;
import com.dev.csas.error.CatalogNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TeacherServiceTest {

    @Mock
    private TeacherRepository teacherRepository;
    @Mock
    private CourseRepository courseRepository;
    @InjectMocks
    private TeacherService teacherService;

    @Test
    void findByIdThrowsWhenMissing() {
        when(teacherRepository.findById(4L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> teacherService.findById(4L))
                .isInstanceOf(CatalogNotFoundException.class)
                .hasMessage("Преподаватель с id 4 не найден");
    }

    @Test
    void createPersistsTeacher() {
        when(teacherRepository.save(any())).thenAnswer(invocation -> {
            TeacherJPAEntity entity = invocation.getArgument(0);
            entity.setId(2L);
            return entity;
        });

        TeacherResponse created = teacherService.create(request());

        assertThat(created.id()).isEqualTo(2L);
        assertThat(created.department()).isEqualTo("ИВТ");
    }

    @Test
    void deleteThrowsWhenAssignedToCourse() {
        when(teacherRepository.findById(2L)).thenReturn(Optional.of(teacher(2L)));
        when(courseRepository.existsByTeacher_Id(2L)).thenReturn(true);

        assertThatThrownBy(() -> teacherService.delete(2L))
                .isInstanceOf(CatalogConflictException.class)
                .hasMessage("Нельзя удалить преподавателя: он назначен на курс");
        verify(teacherRepository, never()).deleteById(any());
    }

    @Test
    void deleteRemovesUnusedTeacher() {
        when(teacherRepository.findById(2L)).thenReturn(Optional.of(teacher(2L)));
        when(courseRepository.existsByTeacher_Id(2L)).thenReturn(false);

        teacherService.delete(2L);

        verify(teacherRepository).deleteById(2L);
    }

    private static TeacherRequest request() {
        return new TeacherRequest(
                "Пётр",
                "Петров",
                null,
                "ИВТ",
                "Доцент",
                "к.т.н.",
                "petrov@example.com",
                null
        );
    }

    private static TeacherJPAEntity teacher(Long id) {
        return TeacherJPAEntity.builder()
                .id(id)
                .firstName("Пётр")
                .lastName("Петров")
                .department("ИВТ")
                .position("Доцент")
                .build();
    }
}
