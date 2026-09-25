package com.dev.csas.student;

import com.dev.csas.attendance.LectureAttendanceRepository;
import com.dev.csas.control.ControlResultRepository;
import com.dev.csas.enums.StudentStatusEnum;
import com.dev.csas.error.CatalogBadRequestException;
import com.dev.csas.error.CatalogConflictException;
import com.dev.csas.error.CatalogNotFoundException;
import com.dev.csas.group.GroupJPAEntity;
import com.dev.csas.group.GroupRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StudentServiceTest {

    @Mock
    private StudentRepository studentRepository;
    @Mock
    private GroupRepository groupRepository;
    @Mock
    private ControlResultRepository controlResultRepository;
    @Mock
    private LectureAttendanceRepository lectureAttendanceRepository;
    @InjectMocks
    private StudentService studentService;

    @Test
    void findByIdThrowsWhenMissing() {
        when(studentRepository.findById(3L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentService.findById(3L))
                .isInstanceOf(CatalogNotFoundException.class)
                .hasMessage("Студент с id 3 не найден");
    }

    @Test
    void createAssignsExistingGroup() {
        GroupJPAEntity group = GroupJPAEntity.builder().id(10L).name("ИВТ-41-1").build();
        when(studentRepository.existsByRecordBookNumber("A123456")).thenReturn(false);
        when(groupRepository.findById(10L)).thenReturn(Optional.of(group));
        when(studentRepository.save(any())).thenAnswer(invocation -> {
            StudentJPAEntity entity = invocation.getArgument(0);
            entity.setId(1L);
            return entity;
        });

        StudentResponse created = studentService.create(request("A123456", 10L));

        assertThat(created.id()).isEqualTo(1L);
        assertThat(created.group().id()).isEqualTo(10L);
        assertThat(created.group().name()).isEqualTo("ИВТ-41-1");
    }

    @Test
    void createThrowsWhenGroupMissing() {
        when(studentRepository.existsByRecordBookNumber("A123456")).thenReturn(false);
        when(groupRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> studentService.create(request("A123456", 99L)))
                .isInstanceOf(CatalogBadRequestException.class)
                .hasMessage("Группа с id 99 не найдена");
    }

    @Test
    void createThrowsWhenRecordBookTaken() {
        when(studentRepository.existsByRecordBookNumber("A123456")).thenReturn(true);

        assertThatThrownBy(() -> studentService.create(request("A123456", null)))
                .isInstanceOf(CatalogConflictException.class)
                .hasMessageContaining("номером зачётки");
    }

    @Test
    void findAllMapsStudentsWithoutGroup() {
        StudentJPAEntity student = StudentJPAEntity.builder()
                .id(1L)
                .firstName("Иван")
                .lastName("Иванов")
                .recordBookNumber("A123456")
                .build();
        when(studentRepository.findAll()).thenReturn(List.of(student));

        List<StudentResponse> result = studentService.findAll();

        assertThat(result.getFirst().group()).isNull();
        assertThat(result.getFirst().recordBookNumber()).isEqualTo("A123456");
    }

    @Test
    void deleteThrowsWhenControlResultsExist() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student(1L)));
        when(controlResultRepository.existsByStudent_Id(1L)).thenReturn(true);

        assertThatThrownBy(() -> studentService.delete(1L))
                .isInstanceOf(CatalogConflictException.class)
                .hasMessageContaining("результаты контроля или отметки посещаемости");
        verify(studentRepository, never()).deleteById(any());
    }

    @Test
    void deleteThrowsWhenAttendanceExists() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student(1L)));
        when(controlResultRepository.existsByStudent_Id(1L)).thenReturn(false);
        when(lectureAttendanceRepository.existsByStudent_Id(1L)).thenReturn(true);

        assertThatThrownBy(() -> studentService.delete(1L))
                .isInstanceOf(CatalogConflictException.class);
    }

    @Test
    void deleteRemovesStudentWithoutLinks() {
        when(studentRepository.findById(1L)).thenReturn(Optional.of(student(1L)));
        when(controlResultRepository.existsByStudent_Id(1L)).thenReturn(false);
        when(lectureAttendanceRepository.existsByStudent_Id(1L)).thenReturn(false);

        studentService.delete(1L);

        verify(studentRepository).deleteById(1L);
    }

    private static StudentRequest request(String recordBook, Long groupId) {
        return new StudentRequest(
                "Иван",
                "Иванов",
                null,
                recordBook,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                StudentStatusEnum.STUDYING,
                groupId
        );
    }

    private static StudentJPAEntity student(Long id) {
        return StudentJPAEntity.builder()
                .id(id)
                .firstName("Иван")
                .lastName("Иванов")
                .recordBookNumber("A123456")
                .build();
    }
}
