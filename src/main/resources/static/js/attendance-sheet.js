(() => {
    const {node, label, setNotice, message, withPending, confirmAndRun} = window.CsasUi;
    const sheetId = Number(document.body.dataset.sheetId);
    let sheet;
    let user;

    async function load() {
        sheet = await window.CsasApi.request(`/api/attendance-sheets/${sheetId}`);
    }

    function render() {
        document.getElementById('sheet-title').textContent = `Ведомость посещаемости №${sheet.id}`;
        document.getElementById('sheet-summary').textContent =
            `${sheet.course.disciplineName}, ${sheet.course.groupName}, ${label(sheet.status)}`;
        const reportLink = document.getElementById('attendance-report-link');
        reportLink.href = `/api/reports/attendance-sheets/${sheetId}/xlsx`;
        reportLink.hidden = user.role !== 'TEACHER' && user.role !== 'DEAN';
        const pdfLink = document.getElementById('attendance-report-pdf-link');
        pdfLink.href = `/api/reports/attendance-sheets/${sheetId}/pdf`;
        pdfLink.hidden = user.role !== 'TEACHER' && user.role !== 'DEAN';
        const canEdit = user.role === 'TEACHER' && sheet.status === 'OPEN';
        document.getElementById('lecture-form-panel').hidden = !canEdit;
        const container = document.getElementById('lectures');
        container.replaceChildren();
        if (sheet.lectures.length === 0) {
            container.append(node('p', {className: 'empty-state'}, 'Лекции ещё не добавлены.'));
        }
        sheet.lectures.forEach(lecture => container.append(renderLecture(lecture, canEdit)));
        renderActions();
    }

    function renderLecture(lecture, canEdit) {
        const section = node('section', {className: 'work-editor'});
        section.append(node('h3', {}, `${lecture.date} ${lecture.title}`));
        const wrap = node('div', {className: 'data-table-wrap'});
        const table = node('table', {className: 'data-table'});
        const head = node('thead');
        const header = node('tr');
        header.append(node('th', {}, 'Студент'), node('th', {}, 'Посещение'));
        head.append(header);
        table.append(head);
        const body = node('tbody');
        lecture.attendance.forEach(record => {
            const row = node('tr');
            row.append(node('td', {}, record.studentName));
            const cell = node('td');
            if (canEdit) {
                const select = node('select', {attributes: {'aria-label': `Посещение: ${record.studentName}`}});
                select.append(node('option', {value: ''}, 'Не отмечено'));
                select.append(node('option', {value: 'PRESENT'}, 'Был'));
                select.append(node('option', {value: 'ABSENT'}, 'Не был'));
                select.value = record.status || '';
                select.addEventListener('change', async () => {
                    if (!select.value) {
                        return;
                    }
                    select.disabled = true;
                    try {
                        await window.CsasApi.request(
                            `/api/attendance-sheets/${sheetId}/lectures/${lecture.id}/attendance/${record.studentId}`,
                            {method: 'PUT', json: {status: select.value}}
                        );
                        await refresh('Отметка посещаемости сохранена.');
                    } catch (error) {
                        select.disabled = false;
                        setNotice(document.querySelector('[data-global-notice]'), message(error), 'error');
                    }
                });
                cell.append(select);
            } else {
                cell.textContent = label(record.status || 'Не отмечено');
            }
            row.append(cell);
            body.append(row);
        });
        table.append(body);
        wrap.append(table);
        section.append(wrap);
        return section;
    }

    function renderActions() {
        const panel = document.getElementById('attendance-actions');
        const buttons = document.getElementById('attendance-action-buttons');
        buttons.replaceChildren();
        if (user.role === 'TEACHER' && sheet.status === 'OPEN') {
            buttons.append(actionButton('Закрыть ведомость', 'закрыть ведомость', async () => {
                await window.CsasApi.request(`/api/attendance-sheets/${sheetId}/close`, {method: 'POST'});
                await refresh('Ведомость закрыта.');
            }));
        }
        if (user.role === 'DEAN' && sheet.status === 'CLOSED') {
            buttons.append(actionButton('Архивировать ведомость', 'архивировать ведомость', async () => {
                await window.CsasApi.request(`/api/attendance-sheets/${sheetId}/archive`, {method: 'POST'});
                await refresh('Ведомость архивирована.');
            }, true));
        }
        panel.hidden = buttons.childElementCount === 0;
    }

    function actionButton(title, confirmation, callback, secondary = false) {
        const button = node('button', {
            className: secondary ? 'button button-secondary' : 'button',
            type: 'button'
        }, title);
        button.addEventListener('click', async () => {
            try {
                await confirmAndRun(button, confirmation, callback);
            } catch (error) {
                setNotice(document.querySelector('[data-global-notice]'), message(error), 'error');
            }
        });
        return button;
    }

    async function refresh(text) {
        await load();
        render();
        setNotice(document.querySelector('[data-global-notice]'), text, 'success');
    }

    window.CsasPages = window.CsasPages || {};
    window.CsasPages.attendanceSheet = async currentUser => {
        user = currentUser;
        document.getElementById('lecture-form').addEventListener('submit', async event => {
            event.preventDefault();
            const form = event.currentTarget;
            const button = form.querySelector('button[type="submit"]');
            const data = Object.fromEntries(new FormData(form).entries());
            try {
                await withPending(button, 'Добавление', () => window.CsasApi.request(
                    `/api/attendance-sheets/${sheetId}/lectures`, {method: 'POST', json: data}
                ));
                form.reset();
                await refresh('Лекция добавлена.');
            } catch (error) {
                setNotice(document.querySelector('[data-global-notice]'), message(error), 'error');
            }
        });
        try {
            await load();
            render();
            setNotice(document.querySelector('[data-global-notice]'), 'Данные ведомости загружены.');
        } catch (error) {
            setNotice(document.querySelector('[data-global-notice]'), message(error), 'error');
        }
    };
})();
