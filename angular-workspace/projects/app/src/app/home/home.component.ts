import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '@auth0/auth0-angular';
import { catchError, of } from 'rxjs';
import { FileService } from '../files/file.service';
import { IconsModule } from '../icons/icons.module';
import { JobService } from '../services/job.service';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, RouterModule, IconsModule],
  templateUrl: './home.component.html',
  styleUrls: ['./home.component.scss']
})
export class HomeComponent implements OnInit {
  recentFiles: any[] = [];
  recentJobs: any[] = [];
  loading = false;
  error = false;

  constructor(
    public auth: AuthService, 
    private fileService: FileService,
    private jobService: JobService,
    public router: Router
  ) {}

  ngOnInit(): void {
    this.loadRecentFiles();
    this.loadRecentJobs();
  }

  loadRecentFiles(): void {
    this.loading = true;
    this.fileService.getUserFiles(undefined, 0, 5, 'createdAt', 'desc', 'UPLOAD')
      .pipe(
        catchError(err => {
          console.error('Error loading recent files:', err);
          return of({ content: [] });
        })
      )
      .subscribe({
        next: (response) => {
          this.recentFiles = response.content || [];
          this.loading = false;
        }
      });
  }

  loadRecentJobs(): void {
    this.loading = true;
    this.jobService.getUserJobs(undefined, undefined, 0, 5, 'createdAt', 'desc')
      .pipe(
        catchError(err => {
          console.error('Error loading recent jobs:', err);
          return of({ content: [] });
        })
      )
      .subscribe({
        next: (response) => {
          this.recentJobs = response.content || [];
          this.loading = false;
        }
      });
  }

  getStatusClass(status: string): string {
    switch(status.toUpperCase()) {
      case 'COMPLETED':
        return 'status-completed';
      case 'FAILED':
        return 'status-failed';
      case 'PROCESSING':
        return 'status-processing';
      case 'PENDING':
        return 'status-pending';
      default:
        return '';
    }
  }

  downloadJobResults(job: any): void {
    // Implement the logic to download job results.
    // This might involve calling a method in JobService.
    console.log('Downloading results for job:', job);
    this.fileService.downloadFile(job.resultFileUuid).subscribe(() => {
      console.log('Download started for job:', job);
    });
  }
}