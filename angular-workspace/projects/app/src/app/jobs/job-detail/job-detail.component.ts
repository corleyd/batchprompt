import { Component, OnInit } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { JobService } from '../../services/job.service';
import { CommonModule } from '@angular/common';
import { IconsModule } from '../../icons/icons.module';
import { DownloadButtonComponent } from '../../shared/components';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatButtonModule } from '@angular/material/button';

@Component({
  selector: 'app-job-detail',
  templateUrl: './job-detail.component.html',
  styleUrls: ['./job-detail.component.scss'],
  standalone: true,
  imports: [CommonModule, IconsModule, DownloadButtonComponent, MatProgressBarModule, MatButtonModule]
})
export class JobDetailComponent implements OnInit {
  job: any;
  loading = true;
  error = false;
  errorMessage = '';
  Math = Math; // Make Math available in the template

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private jobService: JobService
  ) {}

  ngOnInit(): void {
    this.route.paramMap.subscribe(params => {
      const jobUuid = params.get('jobUuid');
      if (jobUuid) {
        this.loadJobDetails(jobUuid);
      } else {
        this.error = true;
        this.errorMessage = 'Job ID not found';
        this.loading = false;
      }
    });
  }

  loadJobDetails(jobUuid: string): void {
    this.loading = true;
    this.error = false;
    
    this.jobService.getJobById(jobUuid).subscribe({
      next: (response) => {
        this.job = response;
        this.loading = false;
      },
      error: (err) => {
        console.error('Error loading job details', err);
        this.error = true;
        this.errorMessage = 'Failed to load job details. Please try again.';
        this.loading = false;
      }
    });
  }

  submitJob(): void {
    if (this.job && this.job.jobUuid) {
      this.jobService.submitJob(this.job.jobUuid).subscribe({
        next: () => {
          this.loadJobDetails(this.job.jobUuid);
        },
        error: (err) => {
          console.error('Error submitting job', err);
          this.errorMessage = 'Failed to submit job. Please try again.';
        }
      });
    }
  }

  continueProcessing(): void {
    if (this.job && this.job.jobUuid) {
      this.jobService.continueProcessingJob(this.job.jobUuid).subscribe({
        next: () => {
          this.loadJobDetails(this.job.jobUuid);
        },
        error: (err) => {
          console.error('Error continuing job processing', err);
          this.errorMessage = 'Failed to continue processing. Please try again.';
        }
      });
    }
  }

  cancelJob(): void {
    if (this.job && this.job.jobUuid) {
      this.jobService.cancelJob(this.job.jobUuid).subscribe({
        next: () => {
          this.router.navigate(['/dashboard/jobs']);
        },
        error: (err) => {
          console.error('Error canceling job', err);
          this.errorMessage = 'Failed to cancel job. Please try again.';
        }
      });
    }
  }

  getStatusClass(status: string): string {
    switch(status?.toUpperCase()) {
      case 'COMPLETED':
        return 'status-completed';
      case 'COMPLETED_WITH_ERRORS':
        return 'status-completed-with-errors';
      case 'FAILED':
        return 'status-failed';
      case 'VALIDATION_FAILED':
        return 'status-validation-failed';
      case 'PROCESSING':
        return 'status-processing';
      case 'PENDING_VALIDATION':
      case 'VALIDATING': 
      case 'PENDING_OUTPUT':
      case 'GENERATING_OUTPUT':
        return 'status-pending';
      case 'VALIDATED':
        return 'status-validated';
      case 'INSUFFICIENT_CREDITS':
        return 'status-insufficient-credits';
      default:
        return '';
    }
  }

  isPendingState(): boolean {
    const status = this.job?.status?.toUpperCase();
    return status === 'PENDING_VALIDATION' || 
           status === 'VALIDATING' || 
           status === 'PENDING_OUTPUT' || 
           status === 'GENERATING_OUTPUT';
  }

  isValidated(): boolean {
    return this.job?.status?.toUpperCase() === 'VALIDATED';
  }

  isValidationFailed(): boolean {
    return this.job?.status?.toUpperCase() === 'VALIDATION_FAILED';
  }

  isFailed(): boolean {
    return this.job?.status?.toUpperCase() === 'FAILED';
  }

  isCompleted(): boolean {
    const status = this.job?.status?.toUpperCase();
    return status === 'COMPLETED' || status === 'COMPLETED_WITH_ERRORS';
  }

  isInsufficientCredits(): boolean {
    return this.job?.status?.toUpperCase() === 'INSUFFICIENT_CREDITS';
  }

  isProcessing(): boolean {
    return this.job?.status?.toUpperCase() === 'PROCESSING';
  }

  goBack(): void {
    this.router.navigate(['/dashboard/jobs']);
  }

  formatDate(date: string): string {
    if (!date) return '';
    return new Date(date).toLocaleString();
  }
}
