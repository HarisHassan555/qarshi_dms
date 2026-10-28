const DEFAULT_BACKEND_PORT = '8080';
const CONTEXT_PATH = 'velocity';

/** Menu URL from DB → single app route path (no context path, no leading slash). */
export function normalizeAppMenuPath(menuUrl: string | undefined | null): string {
    let raw = (menuUrl ?? '').toString().trim();
    if (!raw) {
        return 'Dashboard';
    }
    if (/^https?:\/\//i.test(raw)) {
        return raw;
    }
    raw = raw.replace(/^\/+/, '');
    raw = raw.replace(new RegExp(`^${CONTEXT_PATH}/?`, 'i'), '');
    return raw || 'Dashboard';
}

/** Absolute router link for base href /velocity/ (one segment or nested path). */
export function appMenuRouterLink(menuUrl: string | undefined | null): string[] {
    const path = normalizeAppMenuPath(menuUrl);
    if (/^https?:\/\//i.test(path)) {
        return [];
    }
    const segments = path.split('/').filter(Boolean);
    if (segments.length === 0) {
        return ['/Dashboard'];
    }
    return ['/' + segments.join('/')];
}

const resolveApiBaseUrl = (): string => {
    if (typeof window === 'undefined') {
        return `/${CONTEXT_PATH}/`;
    }

    // ng serve (4200): same-origin /velocity/… → proxy.conf.js → Tomcat :8080 (no CORS).
    // Do not call :8080 from the browser on 4200 unless Tomcat CORS is fully verified.
    return `/${CONTEXT_PATH}/`;
};

const resolveWsBaseUrl = (): string => {
    if (typeof window === 'undefined') {
        return `ws://localhost:${DEFAULT_BACKEND_PORT}/${CONTEXT_PATH}/`;
    }

    const { hostname, port, protocol, host } = window.location;
    const wsProto = protocol === 'https:' ? 'wss:' : 'ws:';

    if (port === '4200') {
        return `${wsProto}//${hostname}:${DEFAULT_BACKEND_PORT}/${CONTEXT_PATH}/`;
    }

    return `${wsProto}//${host}/${CONTEXT_PATH}/`;
};

export class urls {
    static API_URL = resolveApiBaseUrl();
    static WS_BASE_URL = resolveWsBaseUrl();
    static SIGNIN_URL = 'api/auth/login';
    static FORGOT_URL = 'auth/forgot';
    static COUNTRY_URL = 'country';
    static CITY_URL = 'city';
}
