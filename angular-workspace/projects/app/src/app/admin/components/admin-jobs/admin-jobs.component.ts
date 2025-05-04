import { Component, OnInit } from '@angular/core';
import { JobService } from '../../../services/job.service';
import { HttpParams } from '@angular/common/http';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environments/environment';

@Component({
  selector: 'app-admin-jobs',
  templateUrl: './admin-jobs.component.html',
  styleUrls: ['./admin-jobs.component.scss']
})
export class AdminJobsComponent implements OnInit {
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
  
  private apiUrl: string;

  constructor(
    private jobService: JobService,
    private http: HttpClient
  ) {
    this.apiUrl = `${environment.apiBaseUrl}/api/jobs`;
  }

  ngOnInit(): void {
    this.loadJobs();
  }

  loadJobs(): void {
    this.loading = true;
    this.error = false;
    
    // Use a direct call to get all jobs for admin instead of user-specific jobs
    let params = new HttpParams()
      .set('page', this.currentPage.toString())
      .set('size', this.pageSize.toString())
      .set('sort', this.sortField)
      .set('direction', this.sortDirection);
    
    this.http.get<any>(`${this.apiUrl}`, { params }).subscribe({
      next: (response) => {
        // Handle different response structures
        if (Array.isArray(response)) {
          // Direct array response
          this.jobs = response;
          this.totalItems = response.length;
          this.totalPages = Math.ceil(response.length / this.pageSize);
        } else if (response && response.content) {
          // Page object response with content property
          this.jobs = response.content;
          this.totalItems = response.totalElements || 0;
          this.totalPages = response.totalPages || 0;
        } else {
          // Unexpected response format
          console.error('Unexpected API response format:', response);
          this.jobs = [];
          this.totalItems = 0;
          this.totalPages = 0;
          this.error = true;
        }
        this.loading = false;
      },
      error: (err) => {
        console.error('Error loading jobs', err);
        this.jobs = []; // Ensure jobs is always an array
        this.error = true;
        this.loading = false;
      }
    });
  }

  refreshJobs(): void {
    this.loadJobs();
  }

  getStatusClass(status: string): string {
    switch(status.toUpperCase()) {
      case 'COMPLETED':
        return 'status-completed';
      case 'FAILED':
        return 'status-failed';
      case 'PROCESSING':
        return 'status-processing';
      case 'PENDING_OUTPUT':
      case 'SUBMITTED':
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