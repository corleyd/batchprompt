import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { JobService } from '../../services/job.service';

@Component({
  selector: 'app-job-list',
  templateUrl: './job-list.component.html',
  styleUrls: ['./job-list.component.scss']
})
export class JobListComponent implements OnInit {
  jobs: any[] = [];
  loading = true;
  error = false;

  // Pagination
  currentPage = 0;
  pageSize = 10;
  totalItems = 0;
  totalPages = 0;

  // Sorting
  sortField = 'updatedAt';
  sortDirection = 'desc';
  sortOptions = [
    { value: 'jobUuid', label: 'Job ID' },
    { value: 'fileName', label: 'File Name' },
    { value: 'promptName', label: 'Prompt' },
    { value: 'modelName', label: 'Model' },
    { value: 'status', label: 'Status' },
    { value: 'createdAt', label: 'Created' },
    { value: 'updatedAt', label: 'Updated' }
  ];

  constructor(
    private jobService: JobService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadJobs();
  }

  loadJobs(): void {
    this.loading = true;
    this.error = false;
    
    this.jobService.getUserJobs(this.currentPage, this.pageSize, this.sortField, this.sortDirection).subscribe({
      next: (response) => {
        this.jobs = response.content;
        this.totalItems = response.totalElements;
        this.totalPages = response.totalPages;
        this.loading = false;
      },
      error: (err) => {
        console.error('Error loading jobs', err);
        this.error = true;
        this.loading = false;
      }
    });
  }

  refreshJobs(): void {
    this.loadJobs();
  }

  createNewJob(): void {
    this.router.navigate(['/dashboard/jobs/submit']);
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

  // Pagination methods
  goToPage(page: number): void {
    if (page >= 0 && page < this.totalPages) {
      this.currentPage = page;
      this.loadJobs();
    }
  }

  previousPage(): void {
    if (this.currentPage > 0) {
      this.currentPage--;
      this.loadJobs();
    }
  }

  nextPage(): void {
    if (this.currentPage < this.totalPages - 1) {
      this.currentPage++;
      this.loadJobs();
    }
  }

  // Sorting methods
  updateSort(field: string): void {
    if (this.sortField === field) {
      // Toggle direction if clicking the same column
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      // Default to descending when changing columns
      this.sortField = field;
      this.sortDirection = 'desc';
    }
    
    // Reset to first page when sorting changes
    this.currentPage = 0;
    this.loadJobs();
  }

  getSortIcon(field: string): string {
    if (this.sortField !== field) {
      return 'sort';
    }
    return this.sortDirection === 'asc' ? 'arrow_upward' : 'arrow_downward';
  }
}
