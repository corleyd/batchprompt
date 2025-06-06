import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { WaitlistService, WaitlistSignupDto, WaitlistEntryDto } from '../services/waitlist.service';
import { UiNotificationService } from '../services/ui-notification.service';

@Component({
  selector: 'app-waitlist-signup',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './waitlist-signup.component.html',
  styleUrls: ['./waitlist-signup.component.scss']
})
export class WaitlistSignupComponent {
  signupForm: FormGroup;
  isLoading = false;
  isSubmitted = false;
  waitlistEntry: WaitlistEntryDto | null = null;

  constructor(
    private fb: FormBuilder,
    private waitlistService: WaitlistService,
    private uiNotificationService: UiNotificationService
  ) {
    this.signupForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]],
      name: [''],
      company: [''],
      useCase: ['']
    });
  }

  onSubmit() {
    if (this.signupForm.valid) {
      this.isLoading = true;
      const signupData: WaitlistSignupDto = this.signupForm.value;

      this.waitlistService.joinWaitlist(signupData).subscribe({
        next: (entry) => {
          this.waitlistEntry = entry;
          this.isSubmitted = true;
          this.isLoading = false;
          this.uiNotificationService.showSuccess('Access request submitted successfully!');
        },
        error: (error) => {
          this.isLoading = false;
          this.uiNotificationService.showError('Failed to submit access request. Please try again.');
          console.error('Waitlist signup error:', error);
        }
      });
    }
  }

  reset() {
    this.signupForm.reset();
    this.isSubmitted = false;
    this.waitlistEntry = null;
  }

  getStatusText(status: string): string {
    switch (status) {
      case 'PENDING': return 'Awaiting Review';
      case 'INVITED': return 'Invitation Sent';
      case 'REGISTERED': return 'Active User';
      default: return status;
    }
  }
}