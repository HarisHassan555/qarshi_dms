import { AbstractControl, ValidatorFn } from '@angular/forms';
import { Observable, of } from 'rxjs';
import { map } from 'rxjs/operators';

export function alphabetOnlyAsyncValidator(): ValidatorFn {
    return (control: AbstractControl): Observable<{ [key: string]: boolean } | null> => {
        // Simulate an asynchronous check
        return of(control.value).pipe(
            map(value => {
                const isValid = /^[a-zA-Z\s]*$/.test(value);
                return isValid ? null : { 'alphabetOnly': true };
            })
        );
    };
}
