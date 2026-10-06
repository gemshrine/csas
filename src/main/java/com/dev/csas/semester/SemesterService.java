package com.dev.csas.semester;

import com.dev.csas.audit.UserActionService;
import com.dev.csas.audit.UserActionType;
import com.dev.csas.course.CourseRepository;
import com.dev.csas.error.CatalogBadRequestException;
import com.dev.csas.error.CatalogConflictException;
import com.dev.csas.error.CatalogNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SemesterService {

    private final SemesterRepository semesterRepository;
    private final CourseRepository courseRepository;
    private final UserActionService userActions;

    @Transactional(readOnly = true)
    public List<SemesterResponse> findAll() {
        return semesterRepository.findAll().stream()
                .map(SemesterResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public SemesterResponse findById(Long id) {
        return SemesterResponse.from(getSemester(id));
    }

    @Transactional
    public SemesterResponse create(SemesterRequest request) {
        ensurePeriodValid(request);
        ensureNameUnique(request.name(), null);
        SemesterJPAEntity entity = SemesterJPAEntity.builder()
                .name(request.name())
                .startDate(request.startDate())
                .endDate(request.endDate())
                .build();
        SemesterJPAEntity saved = semesterRepository.save(entity);
        userActions.record(UserActionType.SEMESTER_CREATED, "SEMESTER", saved.getId(), null);
        return SemesterResponse.from(saved);
    }

    @Transactional
    public SemesterResponse update(Long id, SemesterRequest request) {
        SemesterJPAEntity entity = getSemester(id);
        ensurePeriodValid(request);
        ensureNameUnique(request.name(), id);
        entity.setName(request.name());
        entity.setStartDate(request.startDate());
        entity.setEndDate(request.endDate());
        SemesterJPAEntity saved = semesterRepository.save(entity);
        userActions.record(UserActionType.SEMESTER_UPDATED, "SEMESTER", saved.getId(), null);
        return SemesterResponse.from(saved);
    }

    @Transactional
    public void delete(Long id) {
        getSemester(id);
        if (courseRepository.existsByStartSemester_Id(id) || courseRepository.existsByEndSemester_Id(id)) {
            throw new CatalogConflictException(
                    "Нельзя удалить семестр: есть связанные курсы"
            );
        }
        semesterRepository.deleteById(id);
        userActions.record(UserActionType.SEMESTER_DELETED, "SEMESTER", id, null);
    }

    private SemesterJPAEntity getSemester(Long id) {
        return semesterRepository.findById(id)
                .orElseThrow(() -> CatalogNotFoundException.semester(id));
    }

    private void ensurePeriodValid(SemesterRequest request) {
        if (!request.startDate().isBefore(request.endDate())) {
            throw new CatalogBadRequestException("Дата начала семестра должна быть раньше даты окончания");
        }
    }

    private void ensureNameUnique(String name, Long currentId) {
        boolean taken = currentId == null
                ? semesterRepository.existsByName(name)
                : semesterRepository.existsByNameAndIdNot(name, currentId);
        if (taken) {
            throw new CatalogConflictException("Семестр с названием «" + name + "» уже существует");
        }
    }
}
