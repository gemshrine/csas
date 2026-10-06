(() => {
    const {node, label, setNotice, message, withPending} = window.CsasUi;
    const type = document.body.dataset.catalogType;
    const catalogs = {
        groups: {
            title: 'Группы', endpoint: '/api/groups',
            fields: [
                ['name', 'Название группы', 'text', true],
                ['yearOfAdmission', 'Год набора', 'number', true],
                ['specialty', 'Специальность', 'text', true],
                ['course', 'Курс обучения', 'number', true],
                ['educationForm', 'Форма финансирования', 'select', true, [['BUDGET', 'Бюджет'], ['CONTRACT', 'Договор']]],
                ['studyForm', 'Форма обучения', 'select', true, [['FULL_TIME', 'Очная'], ['PART_TIME', 'Очно-заочная'], ['EXTRAMURAL', 'Заочная']]],
                ['curatorId', 'Куратор группы', 'deans', false]
            ],
            columns: [['name', 'Название'], ['specialty', 'Специальность'], ['course', 'Курс'],
                ['educationForm', 'Финансирование'], ['studyForm', 'Форма обучения'], ['curator', 'Куратор']]
        },
        students: {
            title: 'Студенты', endpoint: '/api/students',
            fields: [
                ['lastName', 'Фамилия', 'text', true], ['firstName', 'Имя', 'text', true],
                ['patronymic', 'Отчество', 'text', false], ['recordBookNumber', 'Номер зачётки', 'text', true],
                ['birthDate', 'Дата рождения', 'date', false], ['email', 'Электронная почта', 'email', false],
                ['phone', 'Телефон', 'tel', false],
                ['gender', 'Пол', 'select', false, [['MALE', 'Мужской'], ['FEMALE', 'Женский']]],
                ['educationForm', 'Форма финансирования', 'select', false, [['BUDGET', 'Бюджет'], ['CONTRACT', 'Договор']]],
                ['studyForm', 'Форма обучения', 'select', false, [['FULL_TIME', 'Очная'], ['PART_TIME', 'Очно-заочная'], ['EXTRAMURAL', 'Заочная']]],
                ['admissionDate', 'Дата поступления', 'date', false],
                ['status', 'Статус обучения', 'select', false, [['STUDYING', 'Обучается'], ['ACADEMIC_LEAVE', 'Академический отпуск'], ['EXPELLED', 'Отчислен']]],
                ['groupId', 'Группа', 'groups', false]
            ],
            columns: [['recordBookNumber', 'Зачётка'], ['lastName', 'Фамилия'], ['firstName', 'Имя'],
                ['group', 'Группа'], ['status', 'Статус']]
        },
        teachers: {
            title: 'Преподаватели', endpoint: '/api/teachers',
            fields: [
                ['lastName', 'Фамилия', 'text', true], ['firstName', 'Имя', 'text', true],
                ['patronymic', 'Отчество', 'text', false], ['department', 'Кафедра', 'text', true],
                ['position', 'Должность', 'text', true], ['academicDegree', 'Учёная степень', 'text', false],
                ['email', 'Электронная почта', 'email', false], ['phone', 'Телефон', 'tel', false]
            ],
            columns: [['lastName', 'Фамилия'], ['firstName', 'Имя'], ['department', 'Кафедра'],
                ['position', 'Должность'], ['email', 'Электронная почта']]
        },
        disciplines: {
            title: 'Дисциплины', endpoint: '/api/disciplines',
            fields: [
                ['name', 'Название', 'text', true], ['description', 'Описание', 'textarea', false],
                ['lectureHours', 'Часы лекций', 'number', true], ['practiceHours', 'Часы практик', 'number', true],
                ['labHours', 'Часы лабораторных', 'number', true],
                ['controlForm', 'Форма контроля', 'select', true,
                    [['EXAM', 'Экзамен'], ['DIFF_CREDIT', 'Дифференцированный зачёт'], ['CREDIT', 'Зачёт']]]
            ],
            columns: [['name', 'Название'], ['lectureHours', 'Лекции'], ['practiceHours', 'Практики'],
                ['labHours', 'Лабораторные'], ['controlForm', 'Форма контроля']]
        },
        semesters: {
            title: 'Семестры', endpoint: '/api/semesters',
            fields: [
                ['name', 'Название', 'text', true], ['startDate', 'Дата начала', 'date', true],
                ['endDate', 'Дата окончания', 'date', true]
            ],
            columns: [['name', 'Название'], ['startDate', 'Начало'], ['endDate', 'Окончание']]
        }
    };
    const config = catalogs[type];
    let records = [];
    let groups = [];
    let deans = [];
    let editingId = null;

    function valueFor(record, key) {
        if (key === 'group') {
            return record.group?.name || 'Не назначена';
        }
        if (key === 'curator') {
            return record.curatorUsername || 'Не назначен';
        }
        if (['educationForm', 'studyForm', 'status', 'gender', 'controlForm'].includes(key)) {
            return label(record[key]);
        }
        return record[key] ?? '';
    }

    function renderTable() {
        const head = document.getElementById('catalog-head');
        const body = document.getElementById('catalog-body');
        const query = document.getElementById('catalog-search').value.trim().toLocaleLowerCase('ru');
        const visible = records.filter(record => config.columns.some(([key]) =>
            String(valueFor(record, key)).toLocaleLowerCase('ru').includes(query)));
        head.replaceChildren();
        const header = node('tr');
        config.columns.forEach(([, title]) => header.append(node('th', {}, title)));
        header.append(node('th', {}, 'Действия'));
        head.append(header);
        body.replaceChildren();
        visible.forEach(record => {
            const row = node('tr');
            config.columns.forEach(([key], index) => {
                const cell = node('td');
                if (index === 0) {
                    cell.append(node('a', {href: `/${type}/${record.id}`}, valueFor(record, key)));
                } else {
                    cell.textContent = valueFor(record, key);
                }
                row.append(cell);
            });
            const actions = node('td', {className: 'inline-actions'});
            const edit = node('button', {className: 'button button-secondary', type: 'button'}, 'Изменить');
            edit.addEventListener('click', () => showForm(record));
            const remove = node('button', {className: 'button button-danger', type: 'button'}, 'Удалить');
            remove.addEventListener('click', () => deleteRecord(record));
            actions.append(edit, remove);
            row.append(actions);
            body.append(row);
        });
        if (visible.length === 0) {
            const row = node('tr');
            row.append(node('td', {className: 'empty-state', attributes: {colspan: String(config.columns.length + 1)}},
                records.length === 0 ? 'Записи отсутствуют.' : 'Поиск не дал результатов.'));
            body.append(row);
        }
    }

    function displayName(record) {
        return record.name || `${record.lastName} ${record.firstName}` || record.id;
    }

    function showDetails(record, currentUser) {
        const panel = document.getElementById('catalog-detail-panel');
        const detail = document.getElementById('catalog-detail');
        detail.replaceChildren();
        document.getElementById('catalog-detail-title').textContent = `Карточка: ${displayName(record)}`;
        config.fields.forEach(([key, title]) => {
            const value = key === 'groupId'
                ? valueFor(record, 'group')
                : key === 'curatorId' ? valueFor(record, 'curator') : valueFor(record, key);
            detail.append(node('dt', {}, title), node('dd', {}, value));
        });
        const reportActions = document.getElementById('catalog-report-actions');
        reportActions.replaceChildren();
        if (type === 'students' && currentUser.role === 'DEAN') {
            reportActions.append(node('a', {
                className: 'button button-secondary',
                href: `/api/reports/students/${record.id}/academic-record.xlsx`,
                attributes: {download: ''}
            }, 'Скачать успеваемость XLSX'));
            reportActions.append(node('a', {
                className: 'button button-secondary',
                href: `/api/reports/students/${record.id}/academic-record.pdf`,
                attributes: {download: ''}
            }, 'Скачать успеваемость PDF'));
            reportActions.hidden = false;
        } else {
            reportActions.hidden = true;
        }
        document.getElementById('edit-detail-button').onclick = () => showForm(record);
        document.getElementById('delete-detail-button').onclick = () => deleteRecord(record);
        panel.hidden = false;
    }

    async function deleteRecord(record) {
        if (!window.confirm(`Удалить запись «${displayName(record)}»?`)) {
            return;
        }
        try {
            await window.CsasApi.request(`${config.endpoint}/${record.id}`, {method: 'DELETE'});
            window.location.assign(`/${type}`);
        } catch (error) {
            setNotice(document.querySelector('[data-global-notice]'), message(error), 'error');
        }
    }

    function showForm(record = null) {
        editingId = record?.id ?? null;
        const panel = document.getElementById('catalog-form-panel');
        const form = document.getElementById('catalog-form');
        document.getElementById('catalog-detail-panel').hidden = true;
        form.replaceChildren();
        document.getElementById('catalog-form-title').textContent = editingId ? 'Изменить запись' : 'Новая запись';
        config.fields.forEach(([key, title, kind, required, options]) => {
            const wrapper = node('label', {className: 'field'});
            wrapper.append(node('span', {}, title));
            let input;
            if (kind === 'select' || kind === 'groups' || kind === 'deans') {
                input = node('select', {attributes: {name: key}});
                input.append(node('option', {value: ''}, 'Не выбрано'));
                const values = kind === 'groups'
                    ? groups.map(group => [String(group.id), group.name])
                    : kind === 'deans'
                        ? deans.map(dean => [String(dean.id), dean.username])
                        : options;
                values.forEach(([value, text]) => input.append(node('option', {value}, text)));
            } else if (kind === 'textarea') {
                input = node('textarea', {attributes: {name: key, rows: '3'}});
            } else {
                input = node('input', {type: kind, attributes: {name: key}});
                if (kind === 'number') {
                    input.min = '0';
                }
            }
            input.required = required;
            const value = key === 'groupId' ? record?.group?.id : record?.[key];
            if (value !== undefined && value !== null) {
                input.value = value;
            }
            wrapper.append(input);
            form.append(wrapper);
        });
        const actions = node('div', {className: 'field-full form-actions'});
        actions.append(node('button', {className: 'button', type: 'submit'}, editingId ? 'Сохранить изменения' : 'Добавить запись'));
        const cancel = node('button', {className: 'button button-secondary', type: 'button'}, 'Отмена');
        cancel.addEventListener('click', () => {
            panel.hidden = true;
            form.reset();
        });
        actions.append(cancel);
        form.append(actions);
        panel.hidden = false;
        panel.scrollIntoView({behavior: 'smooth', block: 'start'});
    }

    function payloadFrom(form) {
        const data = Object.fromEntries(new FormData(form).entries());
        config.fields.forEach(([key, , kind]) => {
            if (kind === 'number' || kind === 'groups' || kind === 'deans') {
                data[key] = data[key] === '' ? null : Number(data[key]);
            } else if (data[key] === '') {
                data[key] = null;
            }
        });
        return data;
    }

    async function load() {
        records = await window.CsasApi.request(config.endpoint);
        if (type === 'students') {
            groups = await window.CsasApi.request('/api/groups');
        }
        if (type === 'groups') {
            deans = await window.CsasApi.request('/api/groups/curators');
        }
        renderTable();
    }

    window.CsasPages = window.CsasPages || {};
    window.CsasPages.catalog = async currentUser => {
        if (!config) {
            setNotice(document.querySelector('[data-global-notice]'), 'Неизвестный справочник.', 'error');
            return;
        }
        document.title = `${config.title} | Учёт успеваемости`;
        document.getElementById('catalog-title').textContent = config.title;
        document.getElementById('catalog-description').textContent = 'Просмотр и редактирование записей справочника.';
        document.getElementById('new-item-button').addEventListener('click', () => showForm());
        document.getElementById('catalog-search').addEventListener('input', renderTable);
        document.getElementById('catalog-form').addEventListener('submit', async event => {
            event.preventDefault();
            const form = event.currentTarget;
            const button = form.querySelector('button[type="submit"]');
            const method = editingId ? 'PUT' : 'POST';
            const path = editingId ? `${config.endpoint}/${editingId}` : config.endpoint;
            try {
                await withPending(button, 'Сохранение', () => window.CsasApi.request(path, {
                    method,
                    json: payloadFrom(form)
                }));
                document.getElementById('catalog-form-panel').hidden = true;
                await load();
                setNotice(document.querySelector('[data-global-notice]'), 'Запись сохранена.', 'success');
            } catch (error) {
                setNotice(document.querySelector('[data-global-notice]'), message(error), 'error');
            }
        });
        try {
            await load();
            const id = Number(window.location.pathname.split('/').at(-1));
            if (id && Number.isFinite(id)) {
                const record = records.find(item => item.id === id);
                if (record) {
                    showDetails(record, currentUser);
                } else {
                    setNotice(document.querySelector('[data-global-notice]'), 'Запись не найдена.', 'error');
                }
            }
            setNotice(document.querySelector('[data-global-notice]'), `Записей: ${records.length}.`);
        } catch (error) {
            setNotice(document.querySelector('[data-global-notice]'), message(error), 'error');
        }
    };
})();
