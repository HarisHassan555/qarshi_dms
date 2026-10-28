import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { Observable, of } from 'rxjs';
import { catchError, map } from 'rxjs/operators';

export interface NavbarWeatherSnapshot {
    kind: 'sun' | 'moon' | 'cloudy';
    label: string;
    tempC: string;
}

/** Default: Lahore area (matches Open-Meteo query in product spec). */
const DEFAULT_LATITUDE = 31.52;
const DEFAULT_LONGITUDE = 74.36;

const OPEN_METEO_FORECAST = 'https://api.open-meteo.com/v1/forecast';

interface OpenMeteoCurrentResponse {
    current?: {
        temperature_2m?: number;
        weather_code?: number;
        is_day?: number;
        precipitation?: number;
    };
}

@Injectable({ providedIn: 'root' })
export class OpenMeteoWeatherService {
    constructor(private http: HttpClient) {}

    getNavbarWeather(latitude = DEFAULT_LATITUDE, longitude = DEFAULT_LONGITUDE): Observable<NavbarWeatherSnapshot | null> {
        const params = new HttpParams()
            .set('latitude', String(latitude))
            .set('longitude', String(longitude))
            .set(
                'current',
                'temperature_2m,relative_humidity_2m,apparent_temperature,is_day,precipitation,weather_code,wind_speed_10m'
            );

        return this.http.get<OpenMeteoCurrentResponse>(OPEN_METEO_FORECAST, { params }).pipe(
            map((body) => this.mapCurrent(body)),
            catchError(() => of(null))
        );
    }

    private mapCurrent(body: OpenMeteoCurrentResponse): NavbarWeatherSnapshot | null {
        const current = body?.current;
        if (!current || current.temperature_2m == null) {
            return null;
        }
        const code = current.weather_code ?? 0;
        const isDay = current.is_day === 1;
        const label = this.weatherCodeLabel(code, current.precipitation);
        const kind = this.weatherIconKind(code, isDay);
        const temp = Math.round(current.temperature_2m);
        return {
            kind,
            label,
            tempC: String(temp),
        };
    }

    private weatherIconKind(code: number, isDay: boolean): 'sun' | 'moon' | 'cloudy' {
        if (code === 0) {
            return isDay ? 'sun' : 'moon';
        }
        if (code === 1) {
            return isDay ? 'sun' : 'moon';
        }
        if (code === 2) {
            return 'cloudy';
        }
        return 'cloudy';
    }

    private weatherCodeLabel(code: number, precipitation?: number): string {
        if (precipitation != null && precipitation > 0) {
            return 'Precipitation';
        }
        switch (code) {
            case 0:
                return 'Clear';
            case 1:
                return 'Mainly clear';
            case 2:
                return 'Partly cloudy';
            case 3:
                return 'Overcast';
            case 45:
            case 48:
                return 'Fog';
            case 51:
            case 53:
            case 55:
                return 'Drizzle';
            case 56:
            case 57:
                return 'Freezing drizzle';
            case 61:
            case 63:
            case 65:
                return 'Rain';
            case 66:
            case 67:
                return 'Freezing rain';
            case 71:
            case 73:
            case 75:
                return 'Snow';
            case 77:
                return 'Snow grains';
            case 80:
            case 81:
            case 82:
                return 'Rain showers';
            case 85:
            case 86:
                return 'Snow showers';
            case 95:
                return 'Thunderstorm';
            case 96:
            case 99:
                return 'Thunderstorm w/ hail';
            default:
                return 'Weather';
        }
    }
}
