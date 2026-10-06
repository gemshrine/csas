(() => {
    const {node, label, setNotice, message} = window.CsasUi;
    const courseId = Number(document.body.dataset.courseId);

    function addDetail(container, title, value) {
        const paragraph = node('p');
        paragraph.append(node('strong', {}, `${title}: `), document.createTextNode(value));
        container.append(paragraph);
    }

    window.CsasPages = window.CsasPages || {};
    window.CsasPages.studentCourse = async () => {
        try {
            const course = await window.CsasApi.request(`/api/me/courses/${courseId}`);
            document.getElementById('course-title').textContent = course.disciplineName;
            document.getElementById('course-summary').textContent =
                `${course.groupName}, ${label(course.courseType)}, ${course.startSemesterName} по ${course.endSemesterName}, ${label(course.status)}`;
            const result = document.getElementById('student-course-result');
            result.replaceChildren(node('h2', {}, 'Итоговые данные'));
            if (course.courseType === 'SEMINAR') {
                addDetail(result, 'Рейтинг', course.rating === null ? 'Не рассчитан' : `${course.rating}%`);
                if (course.controlResult) {
                    addDetail(result, 'Форма контроля', label(course.controlResult.controlForm));
                    addDetail(result, 'Состояние ведомости', label(course.controlResult.sheetStatus));
                    if (course.controlResult.sheetStatus === 'CLOSED') {
                        if (course.controlResult.finalScore !== null) {
                            addDetail(result, 'Итоговый балл', String(course.controlResult.finalScore));
                            addDetail(result, 'Итоговая оценка', String(course.controlResult.finalGrade));
                        } else {
                            addDetail(result, 'Результат', label(course.controlResult.passFail));
                        }
                    } else {
                        addDetail(result, 'Результат контроля', 'Ещё не выставлен');
                    }
                } else {
                    addDetail(result, 'Контроль', 'Ведомость ещё не создана');
                }
            } else if (course.courseType === 'COURSEWORK') {
                addDetail(result, 'Балл', course.courseworkScore === null ? 'Не выставлен' : String(course.courseworkScore));
                addDetail(result, 'Оценка', label(course.courseworkGrade));
            } else {
                addDetail(result, 'Результат', 'Итоговая оценка выставляется в семинарной части курса.');
            }

            const worksPanel = document.getElementById('student-works-panel');
            worksPanel.hidden = course.courseType !== 'SEMINAR';
            const works = document.getElementById('student-works');
            works.replaceChildren();
            course.works.forEach(work => {
                const row = node('tr');
                row.append(
                    node('td', {}, `${work.number}. ${work.title}`),
                    node('td', {}, work.date),
                    node('td', {}, work.score === null ? 'Не выставлен' : work.score),
                    node('td', {}, work.maxScore)
                );
                works.append(row);
            });
            if (course.works.length === 0 && course.courseType === 'SEMINAR') {
                const row = node('tr');
                row.append(node('td', {className: 'empty-state', attributes: {colspan: '4'}},
                    'Работы пока не добавлены.'));
                works.append(row);
            }
            setNotice(document.querySelector('[data-global-notice]'), 'Успеваемость загружена.');
        } catch (error) {
            setNotice(document.querySelector('[data-global-notice]'), message(error), 'error');
        }
    };
})();
