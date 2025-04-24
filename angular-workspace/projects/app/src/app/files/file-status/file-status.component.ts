import { Component, Input, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { FileService } from '../file.service';
import { interval, Subscription } from 'rxjs';
import { switchMap, takeWhile } from 'rxjs/operators';
@Component({
  selector: 'app-file-status',
  templateUrl: './file-status.component.html',
  styleUrls: ['./file-status.component.scss']
})
export class FileStatusComponent implements OnInit {
  @Input() file: any;
  
  statusPolling?: Subscription;
  statusMessage = '';

  constructor(
    private fileService: FileService,
    private router: Router
  ) {}

  ngOnInit(): void {
    // Poll for status updates if the file is in a processing state
    if (this.file && this.file.status === 'PROCESSING') {
      this.pollFileStatus();
    } else {
      this.updateStatusMessage();
    }
  }

  ngOnDestroy(): void {
    if (this.statusPolling) {
      this.statusPolling.unsubscribe();
    }
  }

  pollFileStatus(): void {
    this.statusPolling = interval(5000)
      .pipe(
        switchMap(() => this.fileService.getFileStatus(this.file.id)),
        takeWhile(status => status.status === 'PROCESSING', true)
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
  
  downloadFile(): void {
    this.fileService.getDownloadUrl(this.file.fileUuid).subscribe({
      next: (downloadUrl) => {
        console.log('Download URL:', downloadUrl, this.file);
        const a = document.createElement('a');
        a.style.display = 'none';
        a.href = downloadUrl;
        a.download = this.file.fileName || 'download';
        a.target = '_self';
        a.click();

        setTimeout(() => {
          document.body.removeChild(a);
        }, 1000);
      },
      error: (err) => {
        console.error('Error initiating file download:', err);
      }
    });
  }

  createJob(): void {
    if (this.file && this.file.fileUuid) {
      this.router.navigate(['/jobs/submit', this.file.fileUuid]);
    }
  }
}
