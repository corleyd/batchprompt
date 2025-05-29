import { Component, OnInit, TemplateRef, ViewChild } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { JobService } from '../../services/job.service';
import { TableConfig, TableColumn, TableSortEvent, TablePageEvent } from '../../shared/components/generic-table/table-models';

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
  sortDirection : ('desc' | 'asc' ) = 'desc';

  promptUuid?: string;
  
  // Table configuration
  tableConfig: TableConfig<any> = {
    columns: [
      { key: 'fileName', header: 'File Name', sortable: true, cssClass: 'file-name-cell' },
      { key: 'promptName', header: 'Prompt', sortable: true },
      { key: 'modelId', header: 'Model', sortable: true },
      { key: 'status', header: 'Status', sortable: true, cellTemplate: 'statusTemplate' },
      { key: 'createdAt', header: 'Created', sortable: true, 
        cellFormatter: (item) => this.formatDate(item.createdAt) },
      { key: 'actions', header: 'Actions', sortable: false, cellTemplate: 'actionsTemplate' }
    ],
    defaultSortField: 'updatedAt',
    defaultSortDirection: 'desc'
  };
  
  paginationConfig = {
    pageSize: 10,
    pageSizeOptions: [5, 10, 20, 50]
  };

  constructor(
    private jobService: JobService,
    private router: Router,
    private route: ActivatedRoute,
  ) {
  }

  ngOnInit(): void {
      this.route.queryParamMap.subscribe(params => {
        this.promptUuid = params.get('promptUuid') || undefined;
        this.loadJobs();
      });
  }

  loadJobs(): void {
    this.loading = true;
    this.error = false;
    
    this.jobService.getUserJobs(undefined, this.promptUuid, this.currentPage, this.pageSize, this.sortField, this.sortDirection).subscribe({
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
      case 'QUEUED':
        return 'status-queued';
      default:
        return '';
    }
  }

  // Format the date for better display
  formatDate(date: string): string {
    return new Date(date).toLocaleString();
  }

  // Handle pagination events from the generic table
  onPageChange(event: TablePageEvent): void {
    this.currentPage = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadJobs();
  }

  // Handle sort events from the generic table
  onSortChange(event: TableSortEvent): void {
    this.sortField = event.field;
    this.sortDirection = event.direction;
    this.currentPage = 0; // Reset to first page
    this.loadJobs();
  }

  // Handle row click event if needed
  onRowClick(job: any): void {
    // Navigate to job details page
    this.router.navigate(['/dashboard/jobs', job.jobUuid]);
  }
}
