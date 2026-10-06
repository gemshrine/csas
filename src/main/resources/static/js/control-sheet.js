(() => {
    const {node, label, setNotice, message, withPending, confirmAndRun} = window.CsasUi;
    const sheetId = Number(document.body.dataset.sheetId);
    let sheet;
    let user;

    async function load() {
        sheet = await window.CsasApi.request(`/api/control-sheets/${sheetId}`);
    }

    function resultInput(student, button) {
        const form = node('form', {className: 'inline-actions'});
        if (sheet.controlForm === 'CREDIT') {
            const select = node('select', {attributes: {'aria-label': `Результат: ${student.studentName}`}});
            select.append(node('option', {value: ''}, 'Выберите результат'));
            select.append(node('option', {value: 'PASS'}, 'Зачёт'));
            select.append(node('option', {value: 'FAIL'}, 'Незачёт'));
            select.value = student.passFail || '';
            form.append(select);
            form.addEventListener('submit', async event => {
                event.preventDefault();
                if (!select.value) {
                    return;
                }
                await save(button, `/api/control-sheets/${sheetId}/results/${student.studentId}/pass-fail`,
                    {result: select.value, reason: ''});
            });
        } else {
            const input = node('input', {
                type: 'number',
                value: student.numericScore ?? '',
                attributes: {min: '0', max: '100', step: '1', required: 'required',
                    'aria-label': `Балл: ${student.studentName}`}
            });
            form.append(input);
            form.addEventListener('submit', async event => {
                event.preventDefault();
                if (input.value === '') {
                    return;
                }
                await save(button, `/api/control-sheets/${sheetId}/results/${student.studentId}/numeric`,
                    {score: Number(input.value), reason: ''});
            });
        }
        button.textContent = 'Сохранить';
        form.append(button);
        return form;
    }

    async function save(button, path, json) {
        try {
            await withPending(button, 'Сохранение', () => window.CsasApi.request(path, {method: 'PUT', json}));
            await refresh('Результат сохранён.');
        } catch (error) {
            setNotice(document.querySelector('[data-global-notice]'), message(error), 'error');
        }
    }

    function render() {
        document.getElementById('sheet-title').textContent = `Контрольная ведомость №${sheet.id}`;
        const scale = sheet.gradingScale
            ? `Шкала оценивания: 5 от ${sheet.gradingScale.grade5From}, 4 от ${sheet.gradingScale.grade4From}, 3 от ${sheet.gradingScale.grade3From}.`
            : '';
        document.getElementById('sheet-summary').textContent =
            `${sheet.course.disciplineName}, ${sheet.course.groupName}, ${label(sheet.controlForm)}, ${label(sheet.status)}. ${scale}`;
        const head = document.getElementById('control-results-head');
        const body = document.getElementById('control-results-body');
        head.replaceChildren();
        body.replaceChildren();
        const editable = user.role === 'TEACHER' && sheet.status === 'OPEN';
        const headers = ['Студент', sheet.controlForm === 'CREDIT' ? 'Результат' : 'Балл'];
        if (sheet.status === 'CLOSED' || sheet.status === 'ARCHIVED') {
            headers.push('Итоговый балл', 'Итоговая оценка');
        }
        if (editable) {
            headers.push('Действия');
        }
        const headerRow = node('tr');
        headers.forEach(text => headerRow.append(node('th', {}, text)));
        head.append(headerRow);

        sheet.students.forEach(student => {
            const row = node('tr');
            row.append(node('td', {}, student.studentName));
            if (editable) {
                const cell = node('td');
                const button = node('button', {className: 'button button-secondary', type: 'submit'});
                cell.append(resultInput(student, button));
                row.append(cell);
            } else {
                const currentResult = student.passFail
                    ? label(student.passFail)
                    : student.numericScore ?? 'Не внесено';
                row.append(node('td', {}, currentResult));
            }
            if (sheet.status === 'CLOSED' || sheet.status === 'ARCHIVED') {
                row.append(node('td', {}, student.finalScore ?? 'Не рассчитан'));
                row.append(node('td', {}, student.finalGrade ?? 'Не выставлена'));
            }
            body.append(row);
        });
        if (sheet.students.length === 0) {
            const row = node('tr');
            row.append(node('td', {className: 'empty-state', attributes: {colspan: String(headers.length)}},
                'В ведомости нет студентов.'));
            body.append(row);
        }
        renderActions();
    }

    function renderActions() {
        const panel = document.getElementById('control-actions');
        const buttons = document.getElementById('control-action-buttons');
        buttons.replaceChildren();
        panel.hidden = false;
        if (user.role === 'TEACHER' && sheet.status === 'OPEN') {
            buttons.append(actionButton('Закрыть ведомость', 'закрыть ведомость', async () => {
                await window.CsasApi.request(`/api/control-sheets/${sheetId}/close`, {method: 'POST'});
                await refresh('Ведомость закрыта.');
            }));
        }
        if (user.role === 'DEAN' && sheet.status === 'CLOSED') {
            buttons.append(actionButton('Переоткрыть ведомость', 'переоткрыть ведомость', async () => {
                await window.CsasApi.request(`/api/control-sheets/${sheetId}/reopen`, {method: 'POST'});
                await refresh('Ведомость переоткрыта.');
            }, true));
            buttons.append(actionButton('Архивировать ведомость', 'архивировать ведомость', async () => {
                await window.CsasApi.request(`/api/control-sheets/${sheetId}/archive`, {method: 'POST'});
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
                if (await confirmAndRun(button, confirmation, callback)) {
                    return;
                }
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
    window.CsasPages.controlSheet = async currentUser => {
        user = currentUser;
        try {
            await load();
            render();
            setNotice(document.querySelector('[data-global-notice]'), 'Данные ведомости загружены.');
        } catch (error) {
            setNotice(document.querySelector('[data-global-notice]'), message(error), 'error');
        }
    };
})();
