const resolveApiBaseUrl = (): string => {
    if (typeof window === 'undefined') {
        return '/velocity/';
    }

    const host = window.location.hostname || 'localhost';
    const port = window.location.port || '';
    if (port === '4200') {
        return `http://${host}:8080/velocity/`;
    }

    return '/velocity/';
};

export class urls {
    static API_URL = resolveApiBaseUrl();
    static SIGNIN_URL = 'api/auth/login';
    static FORGOT_URL = 'auth/forgot';
    static COUNTRY_URL = 'country';
    static CITY_URL = 'city';
}



