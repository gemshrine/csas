package com.dev.csas.group;

import com.dev.csas.audit.UserActionService;
import com.dev.csas.audit.UserActionType;
import com.dev.csas.course.CourseRepository;
import com.dev.csas.error.CatalogConflictException;
import com.dev.csas.error.CatalogNotFoundException;
import com.dev.csas.student.StudentRepository;
import com.dev.csas.user.RoleEnum;
import com.dev.csas.user.UserJPAEntity;
import com.dev.csas.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class GroupService {

    private final GroupRepository groupRepository;
    private final StudentRepository studentRepository;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final UserActionService userActions;

    @Transactional(readOnly = true)
    public List<GroupResponse> findAll() {
        return groupRepository.findAll().stream()
                .map(GroupResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public GroupResponse findById(Long id) {
        return GroupResponse.from(getGroup(id));
    }

    @Transactional
    public GroupResponse create(GroupRequest request) {
        ensureNameUnique(request.name(), null);
        GroupJPAEntity entity = GroupJPAEntity.builder()
                .name(request.name())
                .yearOfAdmission(request.yearOfAdmission())
                .specialty(request.specialty())
                .course(request.course())
                .educationForm(request.educationForm())
                .studyForm(request.studyForm())
                .curator(resolveCurator(request.curatorId()))
                .build();
        GroupJPAEntity saved = groupRepository.save(entity);
        userActions.record(UserActionType.GROUP_CREATED, "GROUP", saved.getId(), null);
        return GroupResponse.from(saved);
    }

    @Transactional
    public GroupResponse update(Long id, GroupRequest request) {
        GroupJPAEntity entity = getGroup(id);
        ensureNameUnique(request.name(), id);
        entity.setName(request.name());
        entity.setYearOfAdmission(request.yearOfAdmission());
        entity.setSpecialty(request.specialty());
        entity.setCourse(request.course());
        entity.setEducationForm(request.educationForm());
        entity.setStudyForm(request.studyForm());
        entity.setCurator(resolveCurator(request.curatorId()));
        GroupJPAEntity saved = groupRepository.save(entity);
        userActions.record(UserActionType.GROUP_UPDATED, "GROUP", saved.getId(), null);
        return GroupResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<GroupCuratorResponse> findCurators() {
        return userRepository.findAllByRoleAndEnabledTrueOrderByIdAsc(RoleEnum.DEAN).stream()
                .map(user -> new GroupCuratorResponse(user.getId(), user.getUsername()))
                .toList();
    }

    @Transactional
    public void delete(Long id) {
        getGroup(id);
        if (studentRepository.existsByGroup_Id(id) || courseRepository.existsByGroup_Id(id)) {
            throw new CatalogConflictException(
                    "Нельзя удалить группу: есть связанные студенты или курсы"
            );
        }
        groupRepository.deleteById(id);
        userActions.record(UserActionType.GROUP_DELETED, "GROUP", id, null);
    }

    private GroupJPAEntity getGroup(Long id) {
        return groupRepository.findById(id)
                .orElseThrow(() -> CatalogNotFoundException.group(id));
    }

    private UserJPAEntity resolveCurator(Long curatorId) {
        if (curatorId == null) {
            return null;
        }
        UserJPAEntity user = userRepository.findById(curatorId)
                .orElseThrow(() -> new CatalogNotFoundException(
                        "Пользователь с id " + curatorId + " не найден"
                ));
        if (user.getRole() != RoleEnum.DEAN || !user.isEnabled()) {
            throw new CatalogConflictException("Куратором группы может быть только активный пользователь с ролью DEAN");
        }
        return user;
    }

    private void ensureNameUnique(String name, Long currentId) {
        boolean taken = currentId == null
                ? groupRepository.existsByName(name)
                : groupRepository.existsByNameAndIdNot(name, currentId);
        if (taken) {
            throw new CatalogConflictException("Группа с названием «" + name + "» уже существует");
        }
    }
}
