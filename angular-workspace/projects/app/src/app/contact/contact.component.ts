import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { FeedbackService, FeedbackRequest } from '../services/feedback.service';
import { UiNotificationService } from '../services/ui-notification.service';

@Component({
  selector: 'app-contact',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './contact.component.html',
  styleUrls: ['./contact.component.scss']
})
export class ContactComponent {
  feedbackForm: FormGroup;
  isSubmitting = false;
  isSubmitted = false;

  constructor(
    private fb: FormBuilder,
    private feedbackService: FeedbackService,
    private uiNotificationService: UiNotificationService
  ) {
    this.feedbackForm = this.fb.group({
      name: ['', [Validators.required, Validators.maxLength(100)]],
      email: ['', [Validators.required, Validators.email]],
      subject: ['', [Validators.required, Validators.maxLength(200)]],
      message: ['', [Validators.required, Validators.maxLength(2000)]]
    });
  }

  onSubmit() {
    if (this.feedbackForm.valid) {
      this.isSubmitting = true;
      
      const feedbackData: FeedbackRequest = {
        name: this.feedbackForm.get('name')?.value,
        email: this.feedbackForm.get('email')?.value,
        subject: this.feedbackForm.get('subject')?.value,
        message: this.feedbackForm.get('message')?.value
      };

      this.feedbackService.sendFeedback(feedbackData).subscribe({
        next: () => {
          this.isSubmitting = false;
          this.isSubmitted = true;
          this.feedbackForm.reset();
          this.uiNotificationService.showSuccess('Feedback sent successfully! We\'ll get back to you soon.');
        },
        error: (error) => {
          this.isSubmitting = false;
          console.error('Error sending feedback:', error);
          this.uiNotificationService.showError('Failed to send feedback. Please try again or contact us directly.');
        }
      });
    } else {
      this.markFormGroupTouched(this.feedbackForm);
    }
  }

  markFormGroupTouched(formGroup: FormGroup) {
    Object.values(formGroup.controls).forEach(control => {
      control.markAsTouched();
    });
  }

  resetForm() {
    this.isSubmitted = false;
    this.feedbackForm.reset();
  }
}
