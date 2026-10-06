package com.dev.csas.group;

import com.dev.csas.audit.UserActionService;
import com.dev.csas.course.CourseRepository;
import com.dev.csas.enums.EducationFormEnum;
import com.dev.csas.enums.StudyFormEnum;
import com.dev.csas.error.CatalogConflictException;
import com.dev.csas.error.CatalogNotFoundException;
import com.dev.csas.student.StudentRepository;
import com.dev.csas.user.RoleEnum;
import com.dev.csas.user.UserJPAEntity;
import com.dev.csas.user.UserRepository;
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
class GroupServiceTest {

    @Mock
    private GroupRepository groupRepository;
    @Mock
    private StudentRepository studentRepository;
    @Mock
    private CourseRepository courseRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserActionService userActions;
    @InjectMocks
    private GroupService groupService;

    @Test
    void findAllReturnsMappedGroups() {
        when(groupRepository.findAll()).thenReturn(List.of(group(1L, "ИВТ-41-1")));

        List<GroupResponse> result = groupService.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.getFirst().name()).isEqualTo("ИВТ-41-1");
    }

    @Test
    void findByIdThrowsWhenMissing() {
        when(groupRepository.findById(7L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> groupService.findById(7L))
                .isInstanceOf(CatalogNotFoundException.class)
                .hasMessage("Группа с id 7 не найдена");
    }

    @Test
    void createPersistsGroup() {
        when(groupRepository.existsByName("ИВТ-41-1")).thenReturn(false);
        when(groupRepository.save(any())).thenAnswer(invocation -> {
            GroupJPAEntity entity = invocation.getArgument(0);
            entity.setId(1L);
            return entity;
        });

        GroupResponse created = groupService.create(request("ИВТ-41-1"));

        assertThat(created.id()).isEqualTo(1L);
        assertThat(created.specialty()).isEqualTo("Информатика");
    }

    @Test
    void createAssignsActiveDeanAsCurator() {
        UserJPAEntity dean = UserJPAEntity.builder()
                .id(17L)
                .username("dean")
                .role(RoleEnum.DEAN)
                .enabled(true)
                .build();
        when(userRepository.findById(17L)).thenReturn(Optional.of(dean));
        when(groupRepository.existsByName("ИВТ-41-1")).thenReturn(false);
        when(groupRepository.save(any())).thenAnswer(invocation -> {
            GroupJPAEntity entity = invocation.getArgument(0);
            entity.setId(1L);
            return entity;
        });

        GroupResponse created = groupService.create(request("ИВТ-41-1", 17L));

        assertThat(created.curatorId()).isEqualTo(17L);
        assertThat(created.curatorUsername()).isEqualTo("dean");
    }

    @Test
    void createRejectsUserWithoutDeanRoleAsCurator() {
        UserJPAEntity teacher = UserJPAEntity.builder()
                .id(17L)
                .username("teacher")
                .role(RoleEnum.TEACHER)
                .enabled(true)
                .build();
        when(groupRepository.existsByName("ИВТ-41-1")).thenReturn(false);
        when(userRepository.findById(17L)).thenReturn(Optional.of(teacher));

        assertThatThrownBy(() -> groupService.create(request("ИВТ-41-1", 17L)))
                .isInstanceOf(CatalogConflictException.class)
                .hasMessageContaining("только активный пользователь с ролью DEAN");
        verify(groupRepository, never()).save(any());
    }

    @Test
    void listsEnabledDeanAccountsAsCuratorOptions() {
        UserJPAEntity dean = UserJPAEntity.builder()
                .id(17L)
                .username("dean")
                .role(RoleEnum.DEAN)
                .enabled(true)
                .build();
        when(userRepository.findAllByRoleAndEnabledTrueOrderByIdAsc(RoleEnum.DEAN))
                .thenReturn(List.of(dean));

        assertThat(groupService.findCurators())
                .containsExactly(new GroupCuratorResponse(17L, "dean"));
    }

    @Test
    void createThrowsWhenNameTaken() {
        when(groupRepository.existsByName("ИВТ-41-1")).thenReturn(true);

        assertThatThrownBy(() -> groupService.create(request("ИВТ-41-1")))
                .isInstanceOf(CatalogConflictException.class)
                .hasMessageContaining("уже существует");
        verify(groupRepository, never()).save(any());
    }

    @Test
    void updateChangesFields() {
        GroupJPAEntity existing = group(1L, "ИВТ-41-1");
        when(groupRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(groupRepository.existsByNameAndIdNot("ИВТ-42-1", 1L)).thenReturn(false);
        when(groupRepository.save(existing)).thenReturn(existing);

        GroupResponse updated = groupService.update(1L, request("ИВТ-42-1"));

        assertThat(updated.name()).isEqualTo("ИВТ-42-1");
    }

    @Test
    void deleteRemovesUnusedGroup() {
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group(1L, "ИВТ-41-1")));
        when(studentRepository.existsByGroup_Id(1L)).thenReturn(false);
        when(courseRepository.existsByGroup_Id(1L)).thenReturn(false);

        groupService.delete(1L);

        verify(groupRepository).deleteById(1L);
    }

    @Test
    void deleteThrowsWhenStudentsExist() {
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group(1L, "ИВТ-41-1")));
        when(studentRepository.existsByGroup_Id(1L)).thenReturn(true);

        assertThatThrownBy(() -> groupService.delete(1L))
                .isInstanceOf(CatalogConflictException.class)
                .hasMessageContaining("связанные студенты или курсы");
        verify(groupRepository, never()).deleteById(any());
    }

    @Test
    void deleteThrowsWhenCoursesExist() {
        when(groupRepository.findById(1L)).thenReturn(Optional.of(group(1L, "ИВТ-41-1")));
        when(studentRepository.existsByGroup_Id(1L)).thenReturn(false);
        when(courseRepository.existsByGroup_Id(1L)).thenReturn(true);

        assertThatThrownBy(() -> groupService.delete(1L))
                .isInstanceOf(CatalogConflictException.class)
                .hasMessageContaining("связанные студенты или курсы");
    }

    private static GroupRequest request(String name) {
        return request(name, null);
    }

    private static GroupRequest request(String name, Long curatorId) {
        return new GroupRequest(
                name,
                2022,
                "Информатика",
                4,
                EducationFormEnum.BUDGET,
                StudyFormEnum.FULL_TIME,
                curatorId
        );
    }

    private static GroupJPAEntity group(Long id, String name) {
        return GroupJPAEntity.builder()
                .id(id)
                .name(name)
                .yearOfAdmission(2022)
                .specialty("Информатика")
                .course(4)
                .educationForm(EducationFormEnum.BUDGET)
                .studyForm(StudyFormEnum.FULL_TIME)
                .build();
    }
}
