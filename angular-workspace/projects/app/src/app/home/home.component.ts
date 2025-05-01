import { Component, OnInit } from '@angular/core';
import { RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '@auth0/auth0-angular';
import { FileService } from '../files/file.service';
import { JobService } from '../services/job.service';
import { catchError, switchMap, tap, of } from 'rxjs';

@Component({
  selector: 'app-home',
  standalone: true,
  imports: [CommonModule, RouterModule],
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
    private jobService: JobService
  ) {}

  ngOnInit(): void {
    this.loadRecentFiles();
    this.loadRecentJobs();
  }

  loadRecentFiles(): void {
    this.loading = true;
    this.fileService.getUserFiles(0, 5, 'createdAt', 'desc')
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
    this.jobService.getUserJobs(0, 5, 'createdAt', 'desc')
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
}