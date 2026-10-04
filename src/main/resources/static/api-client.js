(() => {
    let csrfToken = null;

    class ApiError extends Error {
        constructor(status, payload) {
            super(payload?.message || `Ошибка HTTP ${status}`);
            this.name = 'ApiError';
            this.status = status;
            this.payload = payload;
        }
    }

    function apiPath(path) {
        const url = new URL(path, window.location.origin);
        if (url.origin !== window.location.origin || !url.pathname.startsWith('/api/')) {
            throw new TypeError('Разрешены только запросы к API этого приложения');
        }
        return `${url.pathname}${url.search}`;
    }

    async function responseBody(response) {
        if (response.status === 204) {
            return null;
        }

        const contentType = response.headers.get('content-type') || '';
        if (contentType.includes('application/json')) {
            return response.json();
        }

        const body = await response.text();
        return body || null;
    }

    async function refreshCsrfToken() {
        const response = await fetch('/api/auth/csrf', {
            method: 'GET',
            credentials: 'same-origin',
            headers: {Accept: 'application/json'}
        });
        const payload = await responseBody(response);
        if (!response.ok) {
            throw new ApiError(response.status, payload);
        }
        if (!payload?.headerName || !payload?.token) {
            throw new Error('Сервер не вернул CSRF-токен');
        }
        csrfToken = payload;
        return csrfToken;
    }

    async function request(path, options = {}) {
        const target = apiPath(path);
        const method = (options.method || 'GET').toUpperCase();
        const safeMethods = new Set(['GET', 'HEAD', 'OPTIONS', 'TRACE']);
        const headers = new Headers(options.headers || {});
        headers.set('Accept', 'application/json');

        let body = options.body;
        if (Object.prototype.hasOwnProperty.call(options, 'json')) {
            if (body !== undefined) {
                throw new TypeError('Передай либо body, либо json');
            }
            body = JSON.stringify(options.json);
            headers.set('Content-Type', 'application/json');
        }

        if (!safeMethods.has(method)) {
            const token = csrfToken || await refreshCsrfToken();
            headers.set(token.headerName, token.token);
        }

        const fetchOptions = {...options};
        delete fetchOptions.json;
        const response = await fetch(target, {
            ...fetchOptions,
            method,
            headers,
            body,
            credentials: 'same-origin'
        });
        const payload = await responseBody(response);
        if (!response.ok) {
            throw new ApiError(response.status, payload);
        }

        if (target === '/api/auth/login' && method === 'POST') {
            csrfToken = null;
            try {
                await refreshCsrfToken();
            } catch {
                csrfToken = null;
            }
        } else if (target === '/api/auth/logout' && method === 'POST') {
            csrfToken = null;
        }

        return payload;
    }

    window.CsasApi = Object.freeze({request, refreshCsrfToken, ApiError});
})();
