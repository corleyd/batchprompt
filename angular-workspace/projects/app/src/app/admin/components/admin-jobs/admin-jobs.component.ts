import { Component, OnInit } from '@angular/core';
import { JobService } from '../../../services/job.service';
import { HttpParams } from '@angular/common/http';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environments/environment';
import { TableConfig, TableSortEvent, TablePageEvent } from '../../../shared/components/generic-table/table-models';

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
  sortDirection: ('asc' | 'desc') = 'desc';
  
  private apiUrl: string;

  // Table configuration
  tableConfig: TableConfig<any> = {
    columns: [
      { key: 'jobUuid', header: 'Job ID', sortable: true },
      { key: 'userId', header: 'User ID', sortable: true },
      { key: 'fileName', header: 'File Name', sortable: true },
      { key: 'modelId', header: 'Model', sortable: true },
      { key: 'status', header: 'Status', sortable: true, cellTemplate: 'statusTemplate' },
      { key: 'taskCount', header: 'Tasks', sortable: true, 
        cellFormatter: (item) => `${item.completedTaskCount}/${item.taskCount}` },
      { key: 'createdAt', header: 'Created', sortable: true, 
        cellFormatter: (item) => new Date(item.createdAt).toLocaleString() },
      { key: 'updatedAt', header: 'Updated', sortable: true, 
        cellFormatter: (item) => new Date(item.updatedAt).toLocaleString() }
    ],
    defaultSortField: 'updatedAt',
    defaultSortDirection: 'desc'
  };
  
  paginationConfig = {
    pageSize: 10,
    pageSizeOptions: [10, 20, 50, 100]
  };

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

  // Event handlers for generic table
  onPageChange(event: TablePageEvent): void {
    this.currentPage = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadJobs();
  }

  onSortChange(event: TableSortEvent): void {
    this.sortField = event.field;
    this.sortDirection = event.direction;
    this.currentPage = 0; // Reset to first page when sorting changes
    this.loadJobs();
  }

  // Handle row click event if needed
  onRowClick(job: any): void {
    // Add any action when a row is clicked
    console.log('Job clicked', job);
  }
}