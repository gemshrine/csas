package com.dev.csas.discipline;

import com.dev.csas.audit.UserActionService;
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
class DisciplineServiceTest {

    @Mock
    private DisciplineRepository disciplineRepository;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private UserActionService userActions;
    @InjectMocks
    private DisciplineService disciplineService;

    @Test
    void findByIdThrowsWhenMissing() {
        when(disciplineRepository.findById(8L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> disciplineService.findById(8L))
                .isInstanceOf(CatalogNotFoundException.class)
                .hasMessage("Дисциплина с id 8 не найдена");
    }

    @Test
    void createPersistsDiscipline() {
        when(disciplineRepository.existsByName("Базы данных")).thenReturn(false);
        when(disciplineRepository.save(any())).thenAnswer(invocation -> {
            DisciplineJPAEntity entity = invocation.getArgument(0);
            entity.setId(5L);
            return entity;
        });

        DisciplineResponse created = disciplineService.create(request("Базы данных"));

        assertThat(created.id()).isEqualTo(5L);
        assertThat(created.controlForm()).isEqualTo(ControlFormEnum.EXAM);
    }

    @Test
    void createThrowsWhenNameTaken() {
        when(disciplineRepository.existsByName("Базы данных")).thenReturn(true);

        assertThatThrownBy(() -> disciplineService.create(request("Базы данных")))
                .isInstanceOf(CatalogConflictException.class)
                .hasMessageContaining("уже существует");
        verify(disciplineRepository, never()).save(any());
    }

    @Test
    void deleteThrowsWhenCoursesExist() {
        when(disciplineRepository.findById(5L)).thenReturn(Optional.of(discipline(5L)));
        when(courseRepository.existsByDiscipline_Id(5L)).thenReturn(true);

        assertThatThrownBy(() -> disciplineService.delete(5L))
                .isInstanceOf(CatalogConflictException.class)
                .hasMessage("Нельзя удалить дисциплину: есть связанные курсы");
        verify(disciplineRepository, never()).deleteById(any());
    }

    @Test
    void deleteRemovesUnusedDiscipline() {
        when(disciplineRepository.findById(5L)).thenReturn(Optional.of(discipline(5L)));
        when(courseRepository.existsByDiscipline_Id(5L)).thenReturn(false);

        disciplineService.delete(5L);

        verify(disciplineRepository).deleteById(5L);
    }

    private static DisciplineRequest request(String name) {
        return new DisciplineRequest(name, "Описание", 32, 16, 16, ControlFormEnum.EXAM);
    }

    private static DisciplineJPAEntity discipline(Long id) {
        return DisciplineJPAEntity.builder()
                .id(id)
                .name("Базы данных")
                .lectureHours(32)
                .practiceHours(16)
                .labHours(16)
                .controlForm(ControlFormEnum.EXAM)
                .build();
    }
}
