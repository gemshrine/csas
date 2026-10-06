(() => {
    const {node, label, setNotice, message, withPending, confirmAndRun} = window.CsasUi;
    const sheetId = Number(document.body.dataset.sheetId);
    let sheet;
    let user;

    async function load() {
        sheet = await window.CsasApi.request(`/api/coursework-sheets/${sheetId}`);
    }

    function render() {
        document.getElementById('sheet-title').textContent = `Ведомость курсовой работы №${sheet.sheetId}`;
        document.getElementById('sheet-summary').textContent =
            `${sheet.course.disciplineName}, ${sheet.course.groupName}, ${label(sheet.status)}`;
        const reportLink = document.getElementById('coursework-report-link');
        reportLink.href = `/api/reports/coursework-sheets/${sheetId}/xlsx`;
        reportLink.hidden = user.role !== 'TEACHER' && user.role !== 'DEAN';
        const pdfLink = document.getElementById('coursework-report-pdf-link');
        pdfLink.href = `/api/reports/coursework-sheets/${sheetId}/pdf`;
        pdfLink.hidden = user.role !== 'TEACHER' && user.role !== 'DEAN';
        const body = document.getElementById('coursework-results');
        body.replaceChildren();
        const editable = user.role === 'TEACHER' && sheet.status === 'OPEN';
        sheet.results.forEach(result => {
            const row = node('tr');
            row.append(node('td', {}, result.studentName));
            const scoreCell = node('td');
            if (editable) {
                const form = node('form', {className: 'inline-actions'});
                const input = node('input', {
                    type: 'number',
                    value: result.score ?? '',
                    attributes: {min: '0', max: '100', step: '1', required: 'required',
                        'aria-label': `Балл: ${result.studentName}`}
                });
                const button = node('button', {className: 'button button-secondary', type: 'submit'}, 'Сохранить');
                form.append(input, button);
                form.addEventListener('submit', async event => {
                    event.preventDefault();
                    try {
                        await withPending(button, 'Сохранение', () => window.CsasApi.request(
                            `/api/coursework-sheets/${sheetId}/results/${result.studentId}`,
                            {method: 'PUT', json: {score: Number(input.value)}}
                        ));
                        await refresh('Балл сохранён.');
                    } catch (error) {
                        setNotice(document.querySelector('[data-global-notice]'), message(error), 'error');
                    }
                });
                scoreCell.append(form);
            } else {
                scoreCell.textContent = result.score ?? 'Не внесён';
            }
            row.append(scoreCell, node('td', {}, result.grade ? label(result.grade) : 'Не выставлена'));
            body.append(row);
        });
        if (sheet.results.length === 0) {
            const row = node('tr');
            row.append(node('td', {className: 'empty-state', attributes: {colspan: '3'}},
                'В ведомости нет студентов.'));
            body.append(row);
        }
        renderActions();
    }

    function renderActions() {
        const panel = document.getElementById('coursework-actions');
        const buttons = document.getElementById('coursework-action-buttons');
        const reopenForm = document.getElementById('reopen-form');
        buttons.replaceChildren();
        reopenForm.hidden = true;
        if (user.role === 'TEACHER' && sheet.status === 'OPEN') {
            buttons.append(actionButton('Закрыть ведомость', 'закрыть ведомость', async () => {
                await window.CsasApi.request(`/api/coursework-sheets/${sheetId}/close`, {method: 'POST'});
                await refresh('Ведомость закрыта.');
            }));
        }
        if (user.role === 'DEAN' && sheet.status === 'CLOSED') {
            const reopen = node('button', {className: 'button button-secondary', type: 'button'}, 'Переоткрыть');
            reopen.addEventListener('click', async () => {
                reopenForm.hidden = false;
                reopen.disabled = true;
                try {
                    const semesters = await window.CsasApi.request('/api/semesters');
                    const select = reopenForm.elements.semesterId;
                    select.replaceChildren();
                    const endingSemester = semesters.find(semester => semester.name === sheet.course.endSemesterName);
                    const nextSemester = endingSemester
                        ? semesters.filter(semester => semester.startDate > endingSemester.endDate)
                            .sort((left, right) => left.startDate.localeCompare(right.startDate))[0]
                        : null;
                    if (!nextSemester) {
                        throw new Error('Следующий семестр не найден в справочнике.');
                    }
                    select.append(node('option', {value: nextSemester.id}, nextSemester.name));
                } catch (error) {
                    reopenForm.hidden = true;
                    setNotice(document.querySelector('[data-global-notice]'), message(error), 'error');
                } finally {
                    reopen.disabled = false;
                }
            });
            buttons.append(reopen);
            buttons.append(actionButton('Архивировать ведомость', 'архивировать ведомость', async () => {
                await window.CsasApi.request(`/api/coursework-sheets/${sheetId}/archive`, {method: 'POST'});
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
    window.CsasPages.courseworkSheet = async currentUser => {
        user = currentUser;
        document.getElementById('reopen-form').addEventListener('submit', async event => {
            event.preventDefault();
            const form = event.currentTarget;
            const button = form.querySelector('button[type="submit"]');
            const semesterId = Number(new FormData(form).get('semesterId'));
            try {
                await withPending(button, 'Переоткрытие', () => window.CsasApi.request(
                    `/api/coursework-sheets/${sheetId}/reopen`, {method: 'POST', json: {semesterId}}
                ));
                await refresh('Ведомость переоткрыта.');
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
