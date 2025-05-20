import { Component, Input, OnInit, OnDestroy } from '@angular/core';
import { Router } from '@angular/router';
import { FileService } from '../file.service';
import { interval, Subscription, Subject } from 'rxjs';
import { switchMap, takeWhile, takeUntil } from 'rxjs/operators';
import { NotificationService } from '../../services/notification.service';

@Component({
  selector: 'app-file-status',
  templateUrl: './file-status.component.html',
  styleUrls: ['./file-status.component.scss'],
})
export class FileStatusComponent implements OnInit, OnDestroy {
  @Input() file: any;
  
  statusPolling?: Subscription;
  statusMessage = '';
  private destroy$ = new Subject<void>();

  constructor(
    private fileService: FileService,
    private router: Router,
    private notificationService: NotificationService
  ) {}

  ngOnInit(): void {
    this.updateStatusMessage();
    
    // Subscribe to file-specific notifications
    if (this.file && this.file.fileUuid) {
      this.notificationService.subscribeTo("/files/" + this.file.fileUuid)
        .pipe(takeUntil(this.destroy$))
        .subscribe(notification => {
          console.log('File notification received:', notification);
        });
    }
    
    // Fallback polling for processing files in case WebSocket fails
    // Poll less frequently since we have WebSocket notifications
    if (this.file && this.file.status === 'PROCESSING') {
      this.pollFileStatus();
    }
  }

  ngOnDestroy(): void {
    if (this.statusPolling) {
      this.statusPolling.unsubscribe();
    }
    this.destroy$.next();
    this.destroy$.complete();
  }

  pollFileStatus(): void {
    // Reduced polling frequency since we have WebSocket notifications
    this.statusPolling = interval(15000) // 15 seconds instead of 5 seconds
      .pipe(
        switchMap(() => this.fileService.getFileStatus(this.file.id)),
        takeWhile(status => status.status === 'PROCESSING', true),
        takeUntil(this.destroy$)
      )
      .subscribe({
        next: (updatedStatus) => {
          this.file.status = updatedStatus.status;
          this.updateStatusMessage();
          if (updatedStatus.status !== 'PROCESSING') {
            this.statusPolling?.unsubscribe();
          }
        },
        error: (err) => {
          console.error('Error checking file status:', err);
          this.statusMessage = 'Error checking status';
          this.statusPolling?.unsubscribe();
        }
      });
  }

  updateStatusMessage(): void {
    switch (this.file.status) {
      case 'PROCESSING':
        this.statusMessage = 'Processing...';
        break;
      case 'COMPLETED':
        this.statusMessage = 'Completed';
        break;
      case 'FAILED':
        this.statusMessage = 'Failed: ' + (this.file.errorMessage || 'Unknown error');
        break;
      default:
        this.statusMessage = this.file.status;
    }
  }

  checkStatus(): void {
    this.fileService.getFileStatus(this.file.id).subscribe({
      next: (status) => {
        this.file.status = status.status;
        this.updateStatusMessage();
      },
      error: (err) => {
        console.error('Error refreshing status:', err);
      }
    });
  }
  
  createJob(): void {
    if (this.file && this.file.fileUuid) {
      this.router.navigate(['/dashboard/jobs/submit', this.file.fileUuid]);
    }
  }
}
