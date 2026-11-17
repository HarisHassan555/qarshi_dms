import { Component } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { finalize, timeout } from 'rxjs/operators';
import { AuthService } from '../services/auth/auth.service';
import { NotificationService } from '../NotificationService';

type ForgotStep = 'email' | 'otp' | 'password';

@Component({
    moduleId: module.id,
    templateUrl: './forgot-password.html',
})
export class ForgotPasswordComponent {
    step: ForgotStep = 'email';
    isSubmitting = false;
    maskedEmail = '';
    private verifiedOtp = '';

    emailForm = this.formBuilder.group({
        email: ['', [Validators.required, Validators.email]],
    });

    otpForm = this.formBuilder.group({
        otp: ['', [Validators.required, Validators.pattern(/^\d{6}$/)]],
    });

    passwordForm = this.formBuilder.group({
        newPassword: ['', [Validators.required, Validators.minLength(4)]],
        confirmPassword: ['', [Validators.required]],
    });

    constructor(
        private formBuilder: FormBuilder,
        private authService: AuthService,
        private router: Router,
        private notificationService: NotificationService,
    ) {}

    goToSignIn(): void {
        void this.router.navigate(['/auth/signin']);
    }

    submitEmail(): void {
        if (this.emailForm.invalid) {
            this.emailForm.markAllAsTouched();
            return;
        }
        const email = String(this.emailForm.value.email || '').trim();
        this.isSubmitting = true;
        this.authService.requestForgotPasswordOtp(email).pipe(
            timeout(20000),
            finalize(() => {
                this.isSubmitting = false;
            })
        ).subscribe({
            next: (response: any) => {
                if (String(response?.status || '').toLowerCase() === 'success') {
                    this.maskedEmail = response.maskedEmail || email;
                    this.step = 'otp';
                    this.notificationService.showMessage(
                        response.message || 'OTP is on its way. Check your inbox shortly.',
                        'success'
                    );
                    return;
                }
                this.notificationService.showMessage(response?.message || 'Could not send OTP.', 'danger');
            },
            error: (err) => {
                const message = err?.name === 'TimeoutError'
                    ? 'Request timed out. Please try again.'
                    : (err?.error?.message || 'Could not send OTP.');
                this.notificationService.showMessage(message, 'danger');
            },
        });
    }

    submitOtp(): void {
        if (this.otpForm.invalid) {
            this.otpForm.markAllAsTouched();
            return;
        }
        const email = String(this.emailForm.value.email || '').trim();
        const otp = String(this.otpForm.value.otp || '').trim();
        this.isSubmitting = true;
        this.authService.verifyForgotPasswordOtp(email, otp).subscribe({
            next: (response: any) => {
                this.isSubmitting = false;
                if (String(response?.status || '').toLowerCase() === 'success') {
                    this.verifiedOtp = otp;
                    this.step = 'password';
                    this.notificationService.showMessage(response.message || 'OTP verified.', 'success');
                    return;
                }
                this.notificationService.showMessage(response?.message || 'Invalid OTP.', 'danger');
            },
            error: (err) => {
                this.isSubmitting = false;
                const message = err?.error?.message || 'Invalid OTP.';
                this.notificationService.showMessage(message, 'danger');
            },
        });
    }

    submitNewPassword(): void {
        const newPassword = String(this.passwordForm.value.newPassword || '');
        const confirmPassword = String(this.passwordForm.value.confirmPassword || '');
        if (this.passwordForm.invalid) {
            this.passwordForm.markAllAsTouched();
            return;
        }
        if (newPassword !== confirmPassword) {
            this.notificationService.showMessage('Passwords do not match.', 'danger');
            return;
        }
        const email = String(this.emailForm.value.email || '').trim();
        this.isSubmitting = true;
        this.authService.resetForgotPassword(email, this.verifiedOtp, newPassword).subscribe({
            next: (response: any) => {
                this.isSubmitting = false;
                if (String(response?.status || '').toLowerCase() === 'success') {
                    this.notificationService.showMessage(response.message || 'Password updated.', 'success');
                    void this.router.navigate(['/auth/signin']);
                    return;
                }
                this.notificationService.showMessage(response?.message || 'Could not update password.', 'danger');
            },
            error: (err) => {
                this.isSubmitting = false;
                const message = err?.error?.message || 'Could not update password.';
                this.notificationService.showMessage(message, 'danger');
            },
        });
    }
}