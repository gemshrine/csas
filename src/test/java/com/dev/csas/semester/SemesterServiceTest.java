package com.dev.csas.semester;

import com.dev.csas.course.CourseRepository;
import com.dev.csas.error.CatalogBadRequestException;
import com.dev.csas.error.CatalogConflictException;
import com.dev.csas.error.CatalogNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SemesterServiceTest {

    @Mock
    private SemesterRepository semesterRepository;
    @Mock
    private CourseRepository courseRepository;
    @InjectMocks
    private SemesterService semesterService;

    @Test
    void findByIdThrowsWhenMissing() {
        when(semesterRepository.findById(9L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> semesterService.findById(9L))
                .isInstanceOf(CatalogNotFoundException.class)
                .hasMessage("Семестр с id 9 не найден");
    }

    @Test
    void createPersistsSemester() {
        when(semesterRepository.existsByName("Осенний семестр 2025/2026")).thenReturn(false);
        when(semesterRepository.save(any())).thenAnswer(invocation -> {
            SemesterJPAEntity entity = invocation.getArgument(0);
            entity.setId(3L);
            return entity;
        });

        SemesterResponse created = semesterService.create(request(
                "Осенний семестр 2025/2026",
                LocalDate.of(2025, 9, 1),
                LocalDate.of(2026, 1, 31)
        ));

        assertThat(created.id()).isEqualTo(3L);
        assertThat(created.startDate()).isEqualTo(LocalDate.of(2025, 9, 1));
    }

    @Test
    void createThrowsWhenDatesAreInvalid() {
        SemesterRequest invalid = request(
                "Осенний семестр 2025/2026",
                LocalDate.of(2026, 1, 31),
                LocalDate.of(2025, 9, 1)
        );

        assertThatThrownBy(() -> semesterService.create(invalid))
                .isInstanceOf(CatalogBadRequestException.class)
                .hasMessage("Дата начала семестра должна быть раньше даты окончания");
        verify(semesterRepository, never()).save(any());
    }

    @Test
    void createThrowsWhenStartEqualsEnd() {
        SemesterRequest invalid = request(
                "Осенний семестр 2025/2026",
                LocalDate.of(2025, 9, 1),
                LocalDate.of(2025, 9, 1)
        );

        assertThatThrownBy(() -> semesterService.create(invalid))
                .isInstanceOf(CatalogBadRequestException.class);
    }

    @Test
    void createThrowsWhenNameTaken() {
        when(semesterRepository.existsByName("Осенний семестр 2025/2026")).thenReturn(true);

        assertThatThrownBy(() -> semesterService.create(request(
                "Осенний семестр 2025/2026",
                LocalDate.of(2025, 9, 1),
                LocalDate.of(2026, 1, 31)
        ))).isInstanceOf(CatalogConflictException.class);
    }

    @Test
    void deleteThrowsWhenCoursesExist() {
        when(semesterRepository.findById(3L)).thenReturn(Optional.of(semester(3L)));
        when(courseRepository.existsBySemester_Id(3L)).thenReturn(true);

        assertThatThrownBy(() -> semesterService.delete(3L))
                .isInstanceOf(CatalogConflictException.class)
                .hasMessage("Нельзя удалить семестр: есть связанные курсы");
        verify(semesterRepository, never()).deleteById(any());
    }

    @Test
    void deleteRemovesUnusedSemester() {
        when(semesterRepository.findById(3L)).thenReturn(Optional.of(semester(3L)));
        when(courseRepository.existsBySemester_Id(3L)).thenReturn(false);

        semesterService.delete(3L);

        verify(semesterRepository).deleteById(3L);
    }

    private static SemesterRequest request(String name, LocalDate start, LocalDate end) {
        return new SemesterRequest(name, start, end);
    }

    private static SemesterJPAEntity semester(Long id) {
        return SemesterJPAEntity.builder()
                .id(id)
                .name("Осенний семестр 2025/2026")
                .startDate(LocalDate.of(2025, 9, 1))
                .endDate(LocalDate.of(2026, 1, 31))
                .build();
    }
}
