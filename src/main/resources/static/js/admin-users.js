(() => {
    const {node, label, setNotice, message, withPending} = window.CsasUi;
    const roles = ['ADMIN', 'DEAN', 'TEACHER', 'STUDENT'];
    let users = [];
    let profiles = {teachers: [], students: []};
    let editingUser;

    function notice(text, kind = '') {
        setNotice(document.querySelector('[data-global-notice]'), text, kind);
    }

    function reportError(error) {
        if (error?.status === 401) {
            const returnUrl = encodeURIComponent(window.location.pathname + window.location.search);
            window.location.assign(`/login?returnUrl=${returnUrl}`);
            return;
        }
        notice(message(error), 'error');
    }

    function profileOptions(role) {
        if (role === 'TEACHER') {
            return profiles.teachers;
        }
        if (role === 'STUDENT') {
            return profiles.students;
        }
        return [];
    }

    function fillProfileSelect(select, role, currentUserId = null, currentProfileId = null, currentProfileName = null) {
        const requiresProfile = role === 'TEACHER' || role === 'STUDENT';
        select.replaceChildren(node('option', {value: ''}, requiresProfile ? 'Выберите профиль' : 'Не требуется'));
        select.required = requiresProfile;
        if (!requiresProfile) {
            return;
        }

        const linkedProfileIds = new Set(users
            .filter(user => user.role === role && user.id !== currentUserId && user.profileId !== null)
            .map(user => user.profileId));
        const options = profileOptions(role).filter(profile => !linkedProfileIds.has(profile.id));
        if (currentProfileId !== null && !options.some(profile => profile.id === currentProfileId)) {
            options.unshift({id: currentProfileId, name: currentProfileName || 'Текущий профиль'});
        }
        options.forEach(profile => select.append(node('option', {value: profile.id}, profile.name)));
        if (currentProfileId !== null) {
            select.value = String(currentProfileId);
        }
    }

    async function load() {
        [users, profiles] = await Promise.all([
            window.CsasApi.request('/api/admin/users'),
            window.CsasApi.request('/api/admin/users/profiles')
        ]);
        render();
    }

    function roleForm(user) {
        const form = node('form', {className: 'account-role-form'});
        const select = node('select', {attributes: {'aria-label': `Роль: ${user.username}`} });
        roles.forEach(role => select.append(node('option', {value: role}, label(role))));
        select.value = user.role;
        const profileSelect = node('select', {
            className: 'profile-select',
            attributes: {'aria-label': `Профиль: ${user.username}`}
        });
        fillProfileSelect(profileSelect, user.role, user.id, user.profileId, user.profileName);
        profileSelect.hidden = user.role !== 'TEACHER' && user.role !== 'STUDENT';
        select.addEventListener('change', () => {
            fillProfileSelect(profileSelect, select.value, user.id,
                select.value === user.role ? user.profileId : null,
                select.value === user.role ? user.profileName : null);
            profileSelect.hidden = select.value !== 'TEACHER' && select.value !== 'STUDENT';
        });
        const save = node('button', {className: 'button button-secondary', type: 'submit'}, 'Сохранить');
        form.append(select, profileSelect, save);
        form.addEventListener('submit', async event => {
            event.preventDefault();
            const role = select.value;
            const profileId = profileSelect.value ? Number(profileSelect.value) : null;
            if (role === user.role && profileId === user.profileId) {
                return;
            }
            if ((role === 'TEACHER' || role === 'STUDENT') && profileId === null) {
                notice('Для этой роли нужно выбрать профиль.', 'error');
                return;
            }
            if (!window.confirm(`Сохранить изменения пользователя ${user.username}? Его активные сессии завершатся.`)) {
                select.value = user.role;
                fillProfileSelect(profileSelect, user.role, user.id, user.profileId, user.profileName);
                profileSelect.hidden = user.role !== 'TEACHER' && user.role !== 'STUDENT';
                return;
            }
            try {
                const body = {role};
                if (profileId !== null) {
                    body.profileId = profileId;
                }
                await withPending(save, 'Сохранение', () => window.CsasApi.request(
                    `/api/admin/users/${user.id}/role`, {method: 'PUT', json: body}
                ));
                await load();
                notice('Данные учётной записи изменены.', 'success');
            } catch (error) {
                reportError(error);
            }
        });
        return form;
    }

    function openEditDialog(user) {
        editingUser = user;
        const dialog = document.getElementById('account-edit-dialog');
        const form = document.getElementById('account-edit-form');
        form.elements.username.value = user.username;
        form.elements.password.value = '';
        dialog.showModal();
    }

    function render() {
        const body = document.getElementById('users-body');
        body.replaceChildren();
        users.forEach(user => {
            const row = node('tr');
            row.append(node('td', {}, user.username));

            const roleCell = node('td');
            roleCell.append(roleForm(user));

            row.append(roleCell);
            row.append(node('td', {}, user.profileName || 'Нет'));
            row.append(node('td', {}, user.enabled ? 'Активна' : 'Отключена'));

            const actions = node('td');
            const actionButtons = node('div', {className: 'inline-actions'});
            const edit = node('button', {className: 'button button-secondary', type: 'button'}, 'Изменить');
            edit.addEventListener('click', () => openEditDialog(user));
            const toggle = node('button', {
                className: 'button button-secondary',
                type: 'button'
            }, user.enabled ? 'Отключить' : 'Включить');
            toggle.addEventListener('click', async () => {
                if (user.enabled && !window.confirm(`Отключить учётную запись ${user.username}?`)) {
                    return;
                }
                try {
                    await withPending(toggle, 'Сохранение', () => window.CsasApi.request(
                        `/api/admin/users/${user.id}`,
                        {method: 'PATCH', json: {enabled: !user.enabled}}
                    ));
                    await load();
                    notice(user.enabled ? 'Учётная запись отключена.' : 'Учётная запись включена.', 'success');
                } catch (error) {
                    reportError(error);
                }
            });
            const remove = node('button', {className: 'button button-danger', type: 'button'}, 'Удалить');
            remove.addEventListener('click', async () => {
                if (!window.confirm(`Удалить учётную запись ${user.username}? Профиль студента или преподавателя сохранится.`)) {
                    return;
                }
                try {
                    await withPending(remove, 'Удаление', () => window.CsasApi.request(
                        `/api/admin/users/${user.id}`, {method: 'DELETE'}
                    ));
                    await load();
                    notice('Учётная запись удалена.', 'success');
                } catch (error) {
                    reportError(error);
                }
            });
            actionButtons.append(edit, toggle, remove);
            actions.append(actionButtons);
            row.append(actions);
            body.append(row);
        });
        if (users.length === 0) {
            const row = node('tr');
            row.append(node('td', {className: 'empty-state', attributes: {colspan: '5'}},
                'Учётные записи не найдены.'));
            body.append(row);
        }
    }

    function connectCreateForm() {
        const form = document.getElementById('account-create-form');
        const roleSelect = form.elements.role;
        const profileField = document.getElementById('create-profile-field');
        const profileSelect = form.elements.profileId;
        const updateProfileField = () => {
            fillProfileSelect(profileSelect, roleSelect.value);
            profileField.hidden = roleSelect.value !== 'TEACHER' && roleSelect.value !== 'STUDENT';
        };
        roleSelect.addEventListener('change', updateProfileField);
        updateProfileField();
        form.addEventListener('submit', async event => {
            event.preventDefault();
            const payload = {
                username: form.elements.username.value.trim(),
                password: form.elements.password.value,
                role: roleSelect.value
            };
            if (roleSelect.value === 'TEACHER' || roleSelect.value === 'STUDENT') {
                if (!profileSelect.value) {
                    notice('Для этой роли нужно выбрать свободный профиль.', 'error');
                    return;
                }
                payload.profileId = Number(profileSelect.value);
            }
            const submit = form.querySelector('button[type="submit"]');
            try {
                await withPending(submit, 'Создание', () => window.CsasApi.request(
                    '/api/admin/users', {method: 'POST', json: payload}
                ));
                form.reset();
                updateProfileField();
                await load();
                notice('Учётная запись создана.', 'success');
            } catch (error) {
                reportError(error);
            }
        });
    }

    function connectEditDialog() {
        const dialog = document.getElementById('account-edit-dialog');
        const form = document.getElementById('account-edit-form');
        document.getElementById('account-edit-cancel').addEventListener('click', () => dialog.close());
        form.addEventListener('submit', async event => {
            event.preventDefault();
            if (!editingUser) {
                return;
            }
            const username = form.elements.username.value.trim();
            const password = form.elements.password.value;
            const payload = {};
            if (username !== editingUser.username) {
                payload.username = username;
            }
            if (password) {
                payload.password = password;
            }
            if (Object.keys(payload).length === 0) {
                dialog.close();
                return;
            }
            const submit = form.querySelector('button[type="submit"]');
            try {
                await withPending(submit, 'Сохранение', () => window.CsasApi.request(
                    `/api/admin/users/${editingUser.id}`, {method: 'PATCH', json: payload}
                ));
                dialog.close();
                await load();
                notice('Данные учётной записи изменены.', 'success');
            } catch (error) {
                reportError(error);
            }
        });
    }

    window.CsasPages = window.CsasPages || {};
    window.CsasPages.adminUsers = async () => {
        try {
            await load();
            connectCreateForm();
            connectEditDialog();
            notice(`Учётных записей: ${users.length}.`);
        } catch (error) {
            reportError(error);
        }
    };
})();
