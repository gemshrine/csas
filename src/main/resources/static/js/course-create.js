(() => {
    const {node, setNotice, message, withPending} = window.CsasUi;

    function fill(select, values, idField, nameField) {
        select.replaceChildren(node('option', {value: ''}, 'Выберите значение'));
        values.forEach(value => select.append(node('option', {value: value[idField]}, value[nameField])));
    }

    window.CsasPages = window.CsasPages || {};
    window.CsasPages.courseCreate = async () => {
        const notice = document.querySelector('[data-global-notice]');
        const form = document.getElementById('course-create-form');
        try {
            const [disciplines, groups, teachers, semesters] = await Promise.all([
                window.CsasApi.request('/api/disciplines'),
                window.CsasApi.request('/api/groups'),
                window.CsasApi.request('/api/teachers'),
                window.CsasApi.request('/api/semesters')
            ]);
            fill(form.elements.disciplineId, disciplines, 'id', 'name');
            fill(form.elements.groupId, groups, 'id', 'name');
            fill(form.elements.teacherId, teachers, 'id', 'lastName');
            fill(form.elements.startSemesterId, semesters, 'id', 'name');
            fill(form.elements.endSemesterId, semesters, 'id', 'name');
            teachers.forEach((teacher, index) => {
                form.elements.teacherId.options[index + 1].textContent =
                    `${teacher.lastName} ${teacher.firstName} ${teacher.patronymic || ''}`.trim();
            });
            form.addEventListener('submit', async event => {
                event.preventDefault();
                const button = form.querySelector('button[type="submit"]');
                const data = Object.fromEntries(new FormData(form).entries());
                const payload = {
                    disciplineId: Number(data.disciplineId),
                    groupId: Number(data.groupId),
                    startSemesterId: Number(data.startSemesterId),
                    endSemesterId: Number(data.endSemesterId),
                    courseType: data.courseType,
                    teacherId: Number(data.teacherId)
                };
                try {
                    const course = await withPending(button, 'Создание', () => window.CsasApi.request(
                        '/api/courses', {method: 'POST', json: payload}
                    ));
                    window.location.assign(`/courses/${course.id}`);
                } catch (error) {
                    setNotice(notice, message(error), 'error');
                }
            });
            setNotice(notice, 'Выберите параметры курса.');
        } catch (error) {
            setNotice(notice, message(error), 'error');
        }
    };
})();
