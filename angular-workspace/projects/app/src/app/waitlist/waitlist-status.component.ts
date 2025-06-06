import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { WaitlistService, WaitlistEntryDto } from '../services/waitlist.service';
import { UiNotificationService } from '../services/ui-notification.service';

@Component({
  selector: 'app-waitlist-status',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './waitlist-status.component.html',
  styleUrls: ['./waitlist-status.component.scss']
})
export class WaitlistStatusComponent {
  statusForm: FormGroup;
  isLoading = false;
  waitlistEntry: WaitlistEntryDto | null = null;
  notFound = false;

  constructor(
    private fb: FormBuilder,
    private waitlistService: WaitlistService,
    private uiNotificationService: UiNotificationService
  ) {
    this.statusForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]]
    });
  }

  onSubmit() {
    if (this.statusForm.valid) {
      this.isLoading = true;
      this.notFound = false;
      const email = this.statusForm.get('email')?.value;

      this.waitlistService.getWaitlistStatus(email).subscribe({
        next: (entry) => {
          this.waitlistEntry = entry;
          this.isLoading = false;
        },
        error: (error) => {
          this.isLoading = false;
          if (error.status === 404) {
            this.notFound = true;
          } else {
            this.uiNotificationService.showError('Failed to check status. Please try again.');
          }
          console.error('Status check error:', error);
        }
      });
    }
  }

  reset() {
    this.statusForm.reset();
    this.waitlistEntry = null;
    this.notFound = false;
  }

  getStatusText(status: string): string {
    switch (status) {
      case 'PENDING': return 'Under Review';
      case 'INVITED': return 'Approved';
      case 'REGISTERED': return 'Active';
      default: return status;
    }
  }

  formatDate(dateString: string): string {
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'long',
      day: 'numeric'
    });
  }
}