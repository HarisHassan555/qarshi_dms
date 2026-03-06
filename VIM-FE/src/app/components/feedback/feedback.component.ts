import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { NotificationService } from 'src/app/NotificationService';

@Component({
  selector: 'app-feedback',
  templateUrl: './feedback.component.html',
  styleUrls: ['./feedback.component.css']
})
export class FeedbackComponent implements OnInit {
  feedbackForm!: FormGroup;
  isSubmitting: boolean = false;
  showThankYou: boolean = false;

  constructor(
    private fb: FormBuilder,
    private router: Router,
    private notificationService: NotificationService
  ) {}

  ngOnInit(): void {
    this.initializeForm();
  }

  initializeForm(): void {
    this.feedbackForm = this.fb.group({
      rating: [5, [Validators.required]],
      comment: ['', [Validators.required, Validators.minLength(10)]],
      name: [''],
      email: ['']
    });
  }

  onSubmit(): void {
    if (this.feedbackForm.invalid) {
      this.markFormGroupTouched(this.feedbackForm);
      this.notificationService.showMessage('Please fill all required fields', 'warning');
      return;
    }

    this.isSubmitting = true;

    // Simulate feedback submission
    setTimeout(() => {
      const feedbackData = this.feedbackForm.value;
      console.log('Feedback submitted:', feedbackData);
      
      // Store feedback (in real app, send to backend)
      const feedbacks = JSON.parse(localStorage.getItem('feedbacks') || '[]');
      feedbacks.push({
        ...feedbackData,
        date: new Date().toISOString()
      });
      localStorage.setItem('feedbacks', JSON.stringify(feedbacks));

      this.isSubmitting = false;
      this.showThankYou = true;
      
      // Auto redirect after 3 seconds
      setTimeout(() => {
        this.router.navigate(['/product-catalog']);
      }, 3000);
    }, 1000);
  }

  markFormGroupTouched(formGroup: FormGroup): void {
    Object.keys(formGroup.controls).forEach(key => {
      const control = formGroup.get(key);
      control?.markAsTouched();
    });
  }

  skipFeedback(): void {
    this.router.navigate(['/product-catalog']);
  }
}







