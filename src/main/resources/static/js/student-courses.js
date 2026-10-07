(() => {
    const {node, label, formatPercentage, setNotice, message} = window.CsasUi;

    function resultFor(course) {
        if (course.courseType === 'COURSEWORK') {
            return course.courseworkScore === null
                ? 'Баллы не выставлены'
                : `${course.courseworkScore} баллов, оценка ${label(course.courseworkGrade)}`;
        }
        if (course.controlResult) {
            return course.controlResult.finalScore !== null
                ? `${course.controlResult.finalScore}, оценка ${course.controlResult.finalGrade}`
                : label(course.controlResult.passFail);
        }
        if (course.courseType === 'SEMINAR') {
            return course.rating === null ? 'Рейтинг не рассчитан' : `Рейтинг ${formatPercentage(course.rating)}`;
        }
        return 'Нет итогового результата';
    }

    window.CsasPages = window.CsasPages || {};
    window.CsasPages.studentCourses = async () => {
        try {
            const courses = await window.CsasApi.request('/api/me/courses');
            const body = document.getElementById('student-courses');
            body.replaceChildren();
            courses.forEach(course => {
                const row = node('tr');
                row.append(
                    node('td', {}, course.disciplineName),
                    node('td', {}, course.groupName),
                    node('td', {}, label(course.courseType)),
                    node('td', {}, `${course.startSemesterName} по ${course.endSemesterName}`),
                    node('td', {}, resultFor(course)),
                    node('td', {}, label(course.status))
                );
                const action = node('td');
                action.append(node('a', {href: `/me/courses/${course.courseId}`}, 'Подробности'));
                row.append(action);
                body.append(row);
            });
            if (courses.length === 0) {
                const row = node('tr');
                row.append(node('td', {className: 'empty-state', attributes: {colspan: '7'}},
                    'Курсы не назначены.'));
                body.append(row);
            }
            setNotice(document.querySelector('[data-global-notice]'), `Курсов: ${courses.length}.`);
        } catch (error) {
            setNotice(document.querySelector('[data-global-notice]'), message(error), 'error');
        }
    };
})();
