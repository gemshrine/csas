(() => {
    const {node, label, formatPercentage, setNotice, message, withPending, confirmAndRun, formJson} = window.CsasUi;
    const courseId = Number(document.body.dataset.courseId);
    let course;
    let courseSummary;
    let currentUser;
    let courseCatalogs;

    async function loadData() {
        [course, courseSummary] = await Promise.all([
            window.CsasApi.request(`/api/courses/${courseId}`),
            window.CsasApi.request('/api/courses').then(items => items.find(item => item.courseId === courseId))
        ]);
        if (!courseSummary) {
            throw new Error('Курс не найден в доступном списке.');
        }
    }

    function renderHeader() {
        document.getElementById('course-title').textContent = course.disciplineName;
        document.getElementById('course-summary').textContent =
            `${course.groupName}, ${label(course.courseType)}, ${course.startSemesterName} по ${course.endSemesterName}, ${label(course.status)}`;
        const links = document.getElementById('course-links');
        links.replaceChildren();
        links.append(node('a', {
            className: 'button button-secondary',
            href: `/api/reports/courses/${courseId}/xlsx`,
            attributes: {download: ''}
        }, 'Скачать ведомость XLSX'));
        links.append(node('a', {
            className: 'button button-secondary',
            href: `/api/reports/courses/${courseId}/pdf`,
            attributes: {download: ''}
        }, 'Скачать ведомость PDF'));
        if (courseSummary.controlSheetId !== null) {
            links.append(node('p', {}, `Контрольная ведомость: ${label(courseSummary.controlSheetStatus)}`));
            links.append(node('a', {href: `/control-sheets/${courseSummary.controlSheetId}`}, 'Открыть контрольную ведомость'));
        }
        if (courseSummary.attendanceSheetId !== null) {
            links.append(node('p', {}, `Посещаемость лекций: ${label(courseSummary.attendanceSheetStatus)}`));
            links.append(node('a', {href: `/attendance-sheets/${courseSummary.attendanceSheetId}`}, 'Открыть ведомость посещаемости'));
        }
        if (courseSummary.courseworkSheetId !== null) {
            links.append(node('p', {}, `Курсовая работа: ${label(courseSummary.courseworkSheetStatus)}`));
            links.append(node('a', {href: `/coursework-sheets/${courseSummary.courseworkSheetId}`}, 'Открыть ведомость курсовой работы'));
        }
        if (links.childElementCount === 0) {
            links.hidden = true;
        } else {
            links.hidden = false;
        }
    }

    async function loadCourseCatalogs() {
        const [disciplines, groups, teachers, semesters] = await Promise.all([
            window.CsasApi.request('/api/disciplines'),
            window.CsasApi.request('/api/groups'),
            window.CsasApi.request('/api/teachers'),
            window.CsasApi.request('/api/semesters')
        ]);
        courseCatalogs = {disciplines, groups, teachers, semesters};
    }

    function fillSelect(select, items, nameOf) {
        select.replaceChildren(node('option', {value: ''}, 'Выберите значение'));
        items.forEach(item => select.append(node('option', {value: item.id}, nameOf(item))));
    }

    function connectDeanManagement() {
        const panel = document.getElementById('course-management');
        if (!panel || currentUser.role !== 'DEAN') {
            return;
        }

        const form = document.getElementById('course-edit-form');
        const actions = document.getElementById('course-management-actions');
        fillSelect(form.elements.disciplineId, courseCatalogs.disciplines, item => item.name);
        fillSelect(form.elements.groupId, courseCatalogs.groups, item => item.name);
        fillSelect(form.elements.teacherId, courseCatalogs.teachers, teacher =>
            `${teacher.lastName} ${teacher.firstName} ${teacher.patronymic || ''}`.trim());
        fillSelect(form.elements.startSemesterId, courseCatalogs.semesters, item => item.name);
        fillSelect(form.elements.endSemesterId, courseCatalogs.semesters, item => item.name);
        panel.hidden = false;

        document.getElementById('course-edit-open').addEventListener('click', () => {
            form.elements.disciplineId.value = String(course.disciplineId);
            form.elements.groupId.value = String(course.groupId);
            form.elements.teacherId.value = String(course.teacherId);
            form.elements.startSemesterId.value = String(course.startSemesterId);
            form.elements.endSemesterId.value = String(course.endSemesterId);
            form.hidden = false;
            actions.hidden = true;
        });

        document.getElementById('course-edit-cancel').addEventListener('click', () => {
            form.hidden = true;
            actions.hidden = false;
        });

        form.addEventListener('submit', async event => {
            event.preventDefault();
            const submit = form.querySelector('button[type="submit"]');
            const values = formJson(form);
            try {
                await withPending(submit, 'Сохранение', () => window.CsasApi.request(
                    `/api/courses/${courseId}`, {
                        method: 'PUT',
                        json: {
                            disciplineId: Number(values.disciplineId),
                            groupId: Number(values.groupId),
                            startSemesterId: Number(values.startSemesterId),
                            endSemesterId: Number(values.endSemesterId),
                            courseType: course.courseType,
                            teacherId: Number(values.teacherId)
                        }
                    }
                ));
                form.hidden = true;
                actions.hidden = false;
                await refreshPage('Курс изменён.');
            } catch (error) {
                setNotice(document.querySelector('[data-global-notice]'), message(error), 'error');
            }
        });

        const remove = document.getElementById('course-delete');
        remove.addEventListener('click', async () => {
            try {
                const deleted = await confirmAndRun(remove, 'удалить курс', () =>
                    window.CsasApi.request(`/api/courses/${courseId}`, {method: 'DELETE'}));
                if (deleted) {
                    window.location.assign('/courses');
                }
            } catch (error) {
                setNotice(document.querySelector('[data-global-notice]'), message(error), 'error');
            }
        });
    }

    function scoreFor(work, studentId) {
        return work.scores.find(score => score.studentId === studentId)?.score ?? '';
    }

    function renderScores() {
        const panel = document.getElementById('works-panel');
        const table = document.getElementById('works-table');
        const editors = document.getElementById('work-editors');
        const readOnlyNotice = document.getElementById('scores-readonly');
        table.replaceChildren();
        editors?.replaceChildren();
        panel.hidden = course.courseType !== 'SEMINAR';
        if (panel.hidden) {
            return;
        }
        const canEditScores = currentUser.role === 'TEACHER' && course.status === 'IN_PROGRESS';
        readOnlyNotice.hidden = canEditScores;
        if (!canEditScores) {
            readOnlyNotice.textContent = currentUser.role === 'TEACHER'
                    ? 'Курс завершён. Баллы доступны только для просмотра.'
                    : 'Изменять задания и баллы может преподаватель семинарской части.';
        }

        const head = node('thead');
        const headerRow = node('tr');
        headerRow.append(node('th', {}, 'Студент'));
        course.works.forEach(work => {
            const heading = node('th');
            heading.append(node('span', {}, `${work.number}. ${work.title}`));
            heading.append(node('span', {className: 'muted'}, `Максимум ${work.maxScore}, дата ${work.date}`));
            headerRow.append(heading);
        });
        headerRow.append(node('th', {}, 'Рейтинг'));
        head.append(headerRow);
        table.append(head);

        const body = node('tbody');
        if (course.students.length === 0) {
            const row = node('tr');
            row.append(node('td', {attributes: {colspan: String(course.works.length + 2)}, className: 'empty-state'},
                'В курсе пока нет студентов.'));
            body.append(row);
        }
        course.students.forEach(student => {
            const row = node('tr');
            row.append(node('td', {}, student.name));
            course.works.forEach(work => {
                const cell = node('td');
                const score = scoreFor(work, student.studentId);
                if (canEditScores) {
                    const form = node('form', {className: 'score-form'});
                    const input = node('input', {
                        type: 'number',
                        value: score,
                        attributes: {min: '0', max: String(work.maxScore), step: '1', 'aria-label': `Балл: ${student.name}`}
                    });
                    const button = node('button', {className: 'button button-secondary', type: 'submit'}, 'Сохранить');
                    form.append(input, button);
                    form.addEventListener('submit', async event => {
                        event.preventDefault();
                        if (input.value === '' || Number(input.value) < 0 || Number(input.value) > work.maxScore) {
                            setNotice(document.querySelector('[data-global-notice]'),
                                `Введите целый балл от 0 до ${work.maxScore}.`, 'error');
                            return;
                        }
                        try {
                            await withPending(button, 'Сохранение', () => window.CsasApi.request(
                                `/api/courses/${courseId}/works/${work.number}/scores/${student.studentId}`,
                                {method: 'PUT', json: {score: Number(input.value)}}
                            ));
                            setNotice(document.querySelector('[data-global-notice]'), 'Балл сохранён.', 'success');
                            await loadData();
                            renderHeader();
                            renderScores();
                        } catch (error) {
                            setNotice(document.querySelector('[data-global-notice]'), message(error), 'error');
                        }
                    });
                    cell.append(form);
                } else {
                    cell.textContent = score === '' ? 'Нет оценки' : String(score);
                }
                row.append(cell);
            });
            row.append(node('td', {}, student.rating === null ? 'Нет данных' : formatPercentage(student.rating)));
            body.append(row);
        });
        table.append(body);

        if (currentUser.role === 'TEACHER' && course.status === 'IN_PROGRESS' && editors) {
            course.works.forEach(work => {
                const section = node('section', {className: 'work-editor'});
                section.append(node('h3', {}, `Работа ${work.number}`));
                const form = node('form', {className: 'field-grid'});
                const title = field('Название', 'title', 'text', work.title);
                title.maxLength = 100;
                const date = field('Дата', 'date', 'date', work.date);
                const maxScore = field('Максимальный балл', 'maxScore', 'number', work.maxScore);
                maxScore.min = '1';
                const save = node('button', {className: 'button button-secondary', type: 'submit'}, 'Сохранить параметры');
                form.append(wrapField('Название работы', title), wrapField('Дата', date),
                    wrapField('Максимальный балл', maxScore), save);
                form.addEventListener('submit', async event => {
                    event.preventDefault();
                    try {
                        await withPending(save, 'Сохранение', () => window.CsasApi.request(
                            `/api/courses/${courseId}/works/${work.number}`,
                            {method: 'PUT', json: {
                                title: title.value,
                                date: date.value,
                                maxScore: Number(maxScore.value)
                            }}
                        ));
                        setNotice(document.querySelector('[data-global-notice]'), 'Работа изменена.', 'success');
                        await loadData();
                        renderHeader();
                        renderScores();
                    } catch (error) {
                        setNotice(document.querySelector('[data-global-notice]'), message(error), 'error');
                    }
                });
                section.append(form);
                editors.append(section);
            });
        }
    }

    function field(ariaLabel, name, type, value) {
        return node('input', {type, value, attributes: {name, 'aria-label': ariaLabel, required: 'required'}});
    }

    function wrapField(title, input) {
        const wrapper = node('label', {className: 'field'});
        wrapper.append(node('span', {}, title), input);
        return wrapper;
    }

    function renderActions() {
        const panel = document.getElementById('course-actions');
        const actions = document.getElementById('course-action-buttons');
        const workForm = document.getElementById('work-form');
        const controlForm = document.getElementById('control-sheet-form');
        const teacherSeminar = currentUser.role === 'TEACHER' && course.courseType === 'SEMINAR';
        const teacherLecture = currentUser.role === 'TEACHER' && course.courseType === 'LECTURE';
        const canCompleteCourse = (teacherSeminar || teacherLecture) && course.status === 'IN_PROGRESS';
        panel.hidden = !teacherSeminar && !teacherLecture
                && !document.querySelector('#course-actions form[method="post"]');
        workForm.hidden = !(teacherSeminar && course.status === 'IN_PROGRESS');
        controlForm.hidden = !(teacherSeminar && course.status === 'COMPLETED' && courseSummary.controlSheetId === null);
        actions.replaceChildren();
        if (canCompleteCourse) {
            const complete = node('button', {className: 'button button-secondary', type: 'button'}, 'Завершить курс');
            complete.addEventListener('click', async () => {
                try {
                    const done = await confirmAndRun(complete, 'завершить курс', () =>
                        window.CsasApi.request(`/api/courses/${courseId}/complete`, {method: 'POST'}));
                    if (done) {
                        await refreshPage('Курс завершён.');
                    }
                } catch (error) {
                    setNotice(document.querySelector('[data-global-notice]'), message(error), 'error');
                }
            });
            actions.append(complete);
        }
    }

    async function refreshPage(successMessage) {
        await loadData();
        renderHeader();
        renderActions();
        renderScores();
        setNotice(document.querySelector('[data-global-notice]'), successMessage, 'success');
    }

    function connectForms() {
        const workForm = document.getElementById('work-form');
        workForm.addEventListener('submit', async event => {
            event.preventDefault();
            const button = workForm.querySelector('button[type="submit"]');
            const fields = formJson(workForm);
            try {
                await withPending(button, 'Добавление', () => window.CsasApi.request(
                    `/api/courses/${courseId}/works`,
                    {method: 'POST', json: {...fields, maxScore: Number(fields.maxScore)}}
                ));
                workForm.reset();
                await refreshPage('Работа добавлена.');
            } catch (error) {
                setNotice(document.querySelector('[data-global-notice]'), message(error), 'error');
            }
        });

        const controlForm = document.getElementById('control-sheet-form');
        controlForm.addEventListener('submit', async event => {
            event.preventDefault();
            const button = controlForm.querySelector('button[type="submit"]');
            const fields = formJson(controlForm);
            try {
                const result = await withPending(button, 'Создание', () => window.CsasApi.request(
                    `/api/courses/${courseId}/control-sheet`,
                    {method: 'POST', json: {
                        grade5From: Number(fields.grade5From),
                        grade4From: Number(fields.grade4From),
                        grade3From: Number(fields.grade3From)
                    }}
                ));
                window.location.assign(`/control-sheets/${result.sheetId}`);
            } catch (error) {
                setNotice(document.querySelector('[data-global-notice]'), message(error), 'error');
            }
        });
    }

    window.CsasPages = window.CsasPages || {};
    window.CsasPages.course = async user => {
        currentUser = user;
        try {
            await loadData();
            renderHeader();
            renderActions();
            renderScores();
            if (currentUser.role === 'DEAN') {
                await loadCourseCatalogs();
                connectDeanManagement();
            }
            connectForms();
            setNotice(document.querySelector('[data-global-notice]'), 'Данные курса загружены.');
        } catch (error) {
            setNotice(document.querySelector('[data-global-notice]'), message(error), 'error');
        }
    };
})();
