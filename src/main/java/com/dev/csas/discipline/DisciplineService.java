package com.dev.csas.discipline;

import com.dev.csas.audit.UserActionService;
import com.dev.csas.audit.UserActionType;
import com.dev.csas.course.CourseRepository;
import com.dev.csas.error.CatalogConflictException;
import com.dev.csas.error.CatalogNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DisciplineService {

    private final DisciplineRepository disciplineRepository;
    private final CourseRepository courseRepository;
    private final UserActionService userActions;

    @Transactional(readOnly = true)
    public List<DisciplineResponse> findAll() {
        return disciplineRepository.findAll().stream()
                .map(DisciplineResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public DisciplineResponse findById(Long id) {
        return DisciplineResponse.from(getDiscipline(id));
    }

    @Transactional
    public DisciplineResponse create(DisciplineRequest request) {
        ensureNameUnique(request.name(), null);
        DisciplineJPAEntity entity = DisciplineJPAEntity.builder()
                .name(request.name())
                .description(request.description())
                .lectureHours(request.lectureHours())
                .practiceHours(request.practiceHours())
                .labHours(request.labHours())
                .controlForm(request.controlForm())
                .build();
        DisciplineJPAEntity saved = disciplineRepository.save(entity);
        userActions.record(UserActionType.DISCIPLINE_CREATED, "DISCIPLINE", saved.getId(), null);
        return DisciplineResponse.from(saved);
    }

    @Transactional
    public DisciplineResponse update(Long id, DisciplineRequest request) {
        DisciplineJPAEntity entity = getDiscipline(id);
        ensureNameUnique(request.name(), id);
        entity.setName(request.name());
        entity.setDescription(request.description());
        entity.setLectureHours(request.lectureHours());
        entity.setPracticeHours(request.practiceHours());
        entity.setLabHours(request.labHours());
        entity.setControlForm(request.controlForm());
        DisciplineJPAEntity saved = disciplineRepository.save(entity);
        userActions.record(UserActionType.DISCIPLINE_UPDATED, "DISCIPLINE", saved.getId(), null);
        return DisciplineResponse.from(saved);
    }

    @Transactional
    public void delete(Long id) {
        getDiscipline(id);
        if (courseRepository.existsByDiscipline_Id(id)) {
            throw new CatalogConflictException(
                    "Нельзя удалить дисциплину: есть связанные курсы"
            );
        }
        disciplineRepository.deleteById(id);
        userActions.record(UserActionType.DISCIPLINE_DELETED, "DISCIPLINE", id, null);
    }

    private DisciplineJPAEntity getDiscipline(Long id) {
        return disciplineRepository.findById(id)
                .orElseThrow(() -> CatalogNotFoundException.discipline(id));
    }

    private void ensureNameUnique(String name, Long currentId) {
        boolean taken = currentId == null
                ? disciplineRepository.existsByName(name)
                : disciplineRepository.existsByNameAndIdNot(name, currentId);
        if (taken) {
            throw new CatalogConflictException("Дисциплина с названием «" + name + "» уже существует");
        }
    }
}
