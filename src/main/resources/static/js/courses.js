(() => {
    const {node, label, setNotice, message} = window.CsasUi;
    let courses = [];

    function renderCourse(course) {
        const row = document.createElement('tr');
        row.append(node('td', {}, course.disciplineName));
        row.append(node('td', {}, course.groupName));
        row.append(node('td', {}, label(course.courseType)));
        row.append(node('td', {}, `${course.startSemesterName} по ${course.endSemesterName}`));
        row.append(node('td', {}, label(course.courseStatus)));

        const actions = node('td', {className: 'inline-actions'});
        actions.append(node('a', {href: `/courses/${course.courseId}`}, 'Открыть курс'));
        actions.append(node('a', {
            href: `/api/reports/courses/${course.courseId}/xlsx`,
            attributes: {download: ''}
        }, 'Скачать XLSX'));
        actions.append(node('a', {
            href: `/api/reports/courses/${course.courseId}/pdf`,
            attributes: {download: ''}
        }, 'Скачать PDF'));
        if (course.canReadControlSheet && course.controlSheetId !== null) {
            actions.append(node('a', {href: `/control-sheets/${course.controlSheetId}`}, 'Контроль'));
        }
        if (course.canReadAttendanceSheet && course.attendanceSheetId !== null) {
            actions.append(node('a', {href: `/attendance-sheets/${course.attendanceSheetId}`}, 'Посещаемость'));
        }
        if (course.canReadCourseworkSheet && course.courseworkSheetId !== null) {
            actions.append(node('a', {href: `/coursework-sheets/${course.courseworkSheetId}`}, 'Курсовая'));
        }
        row.append(actions);
        return row;
    }

    function render() {
        const body = document.getElementById('courses-body');
        const query = document.getElementById('course-search').value.trim().toLocaleLowerCase('ru');
        const visible = courses.filter(course =>
            `${course.disciplineName} ${course.groupName}`.toLocaleLowerCase('ru').includes(query));
        body.replaceChildren();
        if (visible.length === 0) {
            const row = node('tr');
            const cell = node('td', {attributes: {colspan: '6'}}, 'Курсы не найдены.');
            cell.className = 'empty-state';
            row.append(cell);
            body.append(row);
            return;
        }
        visible.forEach(course => body.append(renderCourse(course)));
    }

    window.CsasPages = window.CsasPages || {};
    window.CsasPages.courses = async () => {
        const notice = document.querySelector('[data-global-notice]');
        try {
            courses = await window.CsasApi.request('/api/courses');
            render();
            setNotice(notice, `${courses.length} курсов.`);
        } catch (error) {
            setNotice(notice, message(error), 'error');
        }
        document.getElementById('course-search').addEventListener('input', render);
    };
})();
