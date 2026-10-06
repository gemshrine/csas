(() => {
    const roleNames = {
        ADMIN: 'Администратор',
        DEAN: 'Деканат',
        TEACHER: 'Преподаватель',
        STUDENT: 'Студент'
    };

    const enumNames = {
        SEMINAR: 'Практика',
        LECTURE: 'Лекции',
        COURSEWORK: 'Курсовая работа',
        ADMIN: 'Администратор',
        IN_PROGRESS: 'В работе',
        COMPLETED: 'Завершён',
        OPEN: 'Открыта',
        CLOSED: 'Закрыта',
        ARCHIVED: 'В архиве',
        EXAM: 'Экзамен',
        DIFF_CREDIT: 'Дифференцированный зачёт',
        CREDIT: 'Зачёт',
        PASS: 'Зачёт',
        FAIL: 'Незачёт',
        PRESENT: 'Был',
        ABSENT: 'Не был',
        FIVE: '5',
        FOUR: '4',
        THREE: '3',
        NOT_PASSED: 'Не сдана',
        STUDYING: 'Обучается',
        ACADEMIC_LEAVE: 'Академический отпуск',
        EXPELLED: 'Отчислен',
        BUDGET: 'Бюджет',
        CONTRACT: 'Договор',
        FULL_TIME: 'Очная',
        PART_TIME: 'Очно-заочная',
        EXTRAMURAL: 'Заочная',
        SYSTEM: 'Система',
        MALE: 'Мужской',
        FEMALE: 'Женский'
    };

    function message(error) {
        if (!error?.status) {
            console.error(error);
            if (error instanceof TypeError || !error?.message) {
                return 'Не удалось связаться с сервером. Проверьте соединение и попробуйте ещё раз.';
            }
            return error.message;
        }
        const detail = error.payload?.message;
        const messages = {
            400: 'Проверьте введённые данные.',
            401: 'Сеанс завершён. Войдите в систему ещё раз.',
            403: 'У вашей учётной записи недостаточно прав.',
            404: 'Запрашиваемая запись не найдена.',
            409: 'Операция невозможна в текущем состоянии.',
            422: 'Введённые данные не подходят для этой операции.'
        };
        if (error.status >= 500) {
            console.error(error);
            return 'На сервере произошла ошибка. Попробуйте позже.';
        }
        return `${messages[error.status] || 'Не удалось выполнить запрос.'}${detail ? ` ${detail}` : ''} Код: ${error.status}.`;
    }

    function setNotice(target, text, kind = '') {
        if (!target) {
            return;
        }
        target.textContent = text || '';
        target.classList.toggle('notice-error', kind === 'error');
        target.classList.toggle('notice-success', kind === 'success');
    }

    function node(tag, options = {}, text = '') {
        const element = document.createElement(tag);
        if (options.className) {
            element.className = options.className;
        }
        if (options.href) {
            element.href = options.href;
        }
        if (options.type) {
            element.type = options.type;
        }
        if (options.value !== undefined) {
            element.value = options.value;
        }
        if (options.disabled) {
            element.disabled = true;
        }
        if (options.attributes) {
            Object.entries(options.attributes).forEach(([name, value]) => element.setAttribute(name, value));
        }
        element.textContent = text;
        return element;
    }

    function label(value) {
        return enumNames[value] || value || 'Не указано';
    }

    async function withPending(button, pendingText, action) {
        const originalText = button.textContent;
        button.disabled = true;
        button.textContent = pendingText;
        try {
            return await action();
        } finally {
            button.disabled = false;
            button.textContent = originalText;
        }
    }

    async function confirmAndRun(button, actionName, action) {
        if (!window.confirm(`Подтвердите действие: ${actionName}.`)) {
            return false;
        }
        await withPending(button, 'Выполняется', action);
        return true;
    }

    function formJson(form) {
        return Object.fromEntries(new FormData(form).entries());
    }

    async function initialize() {
        const pageName = document.body.dataset.page;
        if (!pageName) {
            return;
        }
        try {
            const user = await window.CsasApi.request('/api/auth/me');
            document.querySelectorAll('[data-roles]').forEach(link => {
                const allowed = link.dataset.roles.split(',');
                link.hidden = !allowed.includes(user.role);
            });
            const userName = document.querySelector('[data-user-name]');
            const userRole = document.querySelector('[data-user-role]');
            const brand = document.querySelector('.site-brand');
            if (brand) {
                brand.href = user.role === 'STUDENT'
                    ? '/me/courses'
                    : user.role === 'ADMIN' ? '/admin/users' : '/courses';
            }
            if (userName) {
                userName.textContent = user.username;
            }
            if (userRole) {
                userRole.textContent = roleNames[user.role] || user.role;
            }
            const logout = document.querySelector('[data-logout]');
            logout?.addEventListener('click', async () => {
                logout.disabled = true;
                try {
                    await window.CsasApi.request('/api/auth/logout', {method: 'POST'});
                    window.location.assign('/login');
                } catch (error) {
                    logout.disabled = false;
                    const notice = document.querySelector('[data-global-notice]');
                    setNotice(notice, message(error), 'error');
                }
            });
            const page = window.CsasPages?.[pageName];
            if (page) {
                await page(user);
            }
        } catch (error) {
            if (error.status === 401) {
                const returnUrl = encodeURIComponent(window.location.pathname + window.location.search);
                window.location.assign(`/login?returnUrl=${returnUrl}`);
                return;
            }
            setNotice(document.querySelector('[data-global-notice]'), message(error), 'error');
        }
    }

    window.CsasUi = Object.freeze({message, setNotice, node, label, withPending, confirmAndRun, formJson});
    document.addEventListener('DOMContentLoaded', initialize);
})();
