(() => {
    const {node, label, setNotice, message, withPending} = window.CsasUi;
    const roles = ['ADMIN', 'DEAN', 'TEACHER', 'STUDENT'];
    let users = [];

    async function load() {
        users = await window.CsasApi.request('/api/admin/users');
        render();
    }

    function render() {
        const body = document.getElementById('users-body');
        body.replaceChildren();
        users.forEach(user => {
            const row = node('tr');
            row.append(node('td', {}, user.username));
            const roleCell = node('td');
            const form = node('form', {className: 'inline-actions'});
            const select = node('select', {attributes: {'aria-label': `Роль: ${user.username}`}});
            roles.forEach(role => select.append(node('option', {value: role}, label(role))));
            select.value = user.role;
            form.append(select);
            const save = node('button', {className: 'button button-secondary', type: 'submit'}, 'Сохранить');
            form.append(save);
            form.addEventListener('submit', async event => {
                event.preventDefault();
                if (select.value === user.role) {
                    return;
                }
                if (!window.confirm(`Изменить роль пользователя ${user.username}? Его активные сессии завершатся.`)) {
                    select.value = user.role;
                    return;
                }
                try {
                    await withPending(save, 'Сохранение', () => window.CsasApi.request(
                        `/api/admin/users/${user.id}/role`, {method: 'PUT', json: {role: select.value}}
                    ));
                    await load();
                    setNotice(document.querySelector('[data-global-notice]'), 'Роль пользователя изменена.', 'success');
                } catch (error) {
                    setNotice(document.querySelector('[data-global-notice]'), message(error), 'error');
                }
            });
            roleCell.append(form);
            row.append(roleCell, node('td', {}, user.enabled ? 'Активна' : 'Отключена'));
            body.append(row);
        });
        if (users.length === 0) {
            const row = node('tr');
            row.append(node('td', {className: 'empty-state', attributes: {colspan: '3'}},
                'Учётные записи не найдены.'));
            body.append(row);
        }
    }

    window.CsasPages = window.CsasPages || {};
    window.CsasPages.adminUsers = async () => {
        try {
            await load();
            setNotice(document.querySelector('[data-global-notice]'), `Учётных записей: ${users.length}.`);
        } catch (error) {
            setNotice(document.querySelector('[data-global-notice]'), message(error), 'error');
        }
    };
})();
