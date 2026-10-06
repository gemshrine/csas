(() => {
    const {node, label, setNotice, message} = window.CsasUi;
    const actions = {
        ACCOUNT_ROLE_CHANGED: 'Изменена роль учётной записи',
        GROUP_CREATED: 'Создана группа',
        GROUP_UPDATED: 'Изменена группа',
        GROUP_DELETED: 'Удалена группа',
        STUDENT_CREATED: 'Создан студент',
        STUDENT_UPDATED: 'Изменён студент',
        STUDENT_DELETED: 'Удалён студент',
        TEACHER_CREATED: 'Создан преподаватель',
        TEACHER_UPDATED: 'Изменён преподаватель',
        TEACHER_DELETED: 'Удалён преподаватель',
        DISCIPLINE_CREATED: 'Создана дисциплина',
        DISCIPLINE_UPDATED: 'Изменена дисциплина',
        DISCIPLINE_DELETED: 'Удалена дисциплина',
        SEMESTER_CREATED: 'Создан семестр',
        SEMESTER_UPDATED: 'Изменён семестр',
        SEMESTER_DELETED: 'Удалён семестр',
        COURSE_ASSIGNED: 'Назначен курс',
        COURSE_WORK_ADDED: 'Добавлено задание курса',
        COURSE_WORK_UPDATED: 'Изменено задание курса',
        COURSE_SCORE_ENTERED: 'Выставлен балл за задание',
        COURSE_COMPLETED: 'Курс завершён',
        COURSE_REOPENED: 'Курс переоткрыт',
        CONTROL_SHEET_CREATED: 'Создана контрольная ведомость',
        CONTROL_RESULT_ENTERED: 'Внесён результат контроля',
        CONTROL_RESULT_CHANGED: 'Изменён результат контроля',
        CONTROL_SHEET_CLOSED: 'Контрольная ведомость закрыта',
        CONTROL_SHEET_REOPENED: 'Контрольная ведомость переоткрыта',
        CONTROL_SHEET_ARCHIVED: 'Контрольная ведомость архивирована',
        ATTENDANCE_LECTURE_ADDED: 'Добавлена лекция',
        ATTENDANCE_REGISTERED: 'Отмечено посещение',
        ATTENDANCE_SHEET_CLOSED: 'Ведомость посещаемости закрыта',
        ATTENDANCE_SHEET_ARCHIVED: 'Ведомость посещаемости архивирована',
        COURSEWORK_RESULT_ENTERED: 'Обновлён результат курсовой работы',
        COURSEWORK_SHEET_CLOSED: 'Ведомость курсовой работы закрыта',
        COURSEWORK_SHEET_REOPENED: 'Ведомость курсовой работы переоткрыта',
        COURSEWORK_SHEET_ARCHIVED: 'Ведомость курсовой работы архивирована'
    };
    const entities = {
        USER: 'Учётная запись',
        GROUP: 'Группа',
        STUDENT: 'Студент',
        TEACHER: 'Преподаватель',
        DISCIPLINE: 'Дисциплина',
        SEMESTER: 'Семестр',
        COURSE: 'Курс',
        CONTROL_SHEET: 'Контрольная ведомость',
        ATTENDANCE_SHEET: 'Ведомость посещаемости',
        COURSEWORK_SHEET: 'Ведомость курсовой работы'
    };

    async function load() {
        const events = await window.CsasApi.request('/api/admin/audit-events');
        const body = document.getElementById('audit-events-body');
        body.replaceChildren();
        events.forEach(event => {
            const row = node('tr');
            const timestamp = new Intl.DateTimeFormat('ru-RU', {
                dateStyle: 'short',
                timeStyle: 'medium'
            }).format(new Date(event.createdAt));
            row.append(
                node('td', {}, timestamp),
                node('td', {}, event.actorUsername),
                node('td', {}, label(event.actorRole)),
                node('td', {}, actions[event.actionType] || event.actionType),
                node('td', {}, `${entities[event.entityType] || event.entityType} ${event.entityId ?? ''}`.trim()),
                node('td', {}, event.details || '')
            );
            body.append(row);
        });
        if (events.length === 0) {
            const row = node('tr');
            row.append(node('td', {className: 'empty-state', attributes: {colspan: '6'}}, 'Записей пока нет.'));
            body.append(row);
        }
        setNotice(document.querySelector('[data-global-notice]'), `Показаны последние действия: ${events.length}.`);
    }

    window.CsasPages = window.CsasPages || {};
    window.CsasPages.adminAuditEvents = async () => {
        try {
            await load();
        } catch (error) {
            setNotice(document.querySelector('[data-global-notice]'), message(error), 'error');
        }
    };
})();
