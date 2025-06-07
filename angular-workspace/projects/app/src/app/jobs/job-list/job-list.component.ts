import { Component, OnInit, TemplateRef, ViewChild, HostListener } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { JobService } from '../../services/job.service';
import { TableConfig, TableColumn, TableSortEvent, TablePageEvent } from '../../shared/components/generic-table/table-models';
import { ConfirmationDialogService } from '../../shared/services/confirmation-dialog.service';

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

  // Context menu control
  activeContextMenuJob: string | null = null;
  menuPosition: { top: number, left: number } | null = null;
  
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
    private confirmationDialogService: ConfirmationDialogService
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

  // Delete a job
  async deleteJob(job: any): Promise<void> {
    // Close the context menu first
    this.activeContextMenuJob = null;
    this.menuPosition = null;
    
    try {
      const confirmed = await this.confirmationDialogService.confirmDelete(job.fileName);
      
      if (confirmed) {
        this.jobService.deleteJob(job.jobUuid).subscribe({
          next: () => {
            // Remove the job from the local array
            this.jobs = this.jobs.filter(j => j.jobUuid !== job.jobUuid);
            // Reload the jobs to ensure accurate pagination
            this.loadJobs();
          },
          error: (err) => {
            console.error('Error deleting job', err);
            // You could also use the confirmation dialog service for error messages
            this.confirmationDialogService.confirm({
              title: 'Error',
              message: 'Failed to delete job. Please try again.',
              confirmText: 'OK',
              cancelText: '',
              isDangerous: false
            });
          }
        });
      }
    } catch (error) {
      console.error('Error showing confirmation dialog', error);
    }
  }

  // Check if a job can be deleted (only completed, failed, or cancelled jobs)
  canDeleteJob(job: any): boolean {
    const deletableStatuses = ['COMPLETED', 'FAILED', 'CANCELLED', 'INSUFFICIENT_CREDITS'];
    return deletableStatuses.includes(job.status?.toUpperCase());
  }

  // Check if a job can be cancelled (based on backend logic and job-detail component)
  canCancelJob(job: any): boolean {
    const cancellableStatuses = ['SUBMITTED', 'PROCESSING', 'VALIDATING', 'PENDING_VALIDATION', 'INSUFFICIENT_CREDITS', 'VALIDATED'];
    return cancellableStatuses.includes(job.status?.toUpperCase());
  }

  // Check if a job can be submitted (validated jobs)
  canSubmitJob(job: any): boolean {
    return job.status?.toUpperCase() === 'VALIDATED';
  }

  // Cancel a job
  async cancelJob(job: any): Promise<void> {
    // Close the context menu first
    this.activeContextMenuJob = null;
    this.menuPosition = null;
    
    try {
      const confirmed = await this.confirmationDialogService.confirm({
        title: 'Cancel Job',
        message: `Are you sure you want to cancel the job "${job.fileName}"? This will stop the processing.`,
        confirmText: 'Cancel Job',
        cancelText: 'Keep Running',
        isDangerous: true
      });
      
      if (confirmed) {
        this.jobService.cancelJob(job.jobUuid).subscribe({
          next: () => {
            // Reload the jobs to get updated status
            this.loadJobs();
          },
          error: (err) => {
            console.error('Error cancelling job', err);
            this.confirmationDialogService.confirm({
              title: 'Error',
              message: 'Failed to cancel job. Please try again.',
              confirmText: 'OK',
              cancelText: '',
              isDangerous: false
            });
          }
        });
      }
    } catch (error) {
      console.error('Error showing confirmation dialog', error);
    }
  }

  // Submit a job
  async submitJob(job: any): Promise<void> {
    // Close the context menu first
    this.activeContextMenuJob = null;
    this.menuPosition = null;
    
    try {
      const confirmed = await this.confirmationDialogService.confirm({
        title: 'Submit Job',
        message: `Are you sure you want to submit the job "${job.fileName}" for processing?`,
        confirmText: 'Submit Job',
        cancelText: 'Cancel',
        isDangerous: false
      });
      
      if (confirmed) {
        this.jobService.submitJob(job.jobUuid).subscribe({
          next: () => {
            // Reload the jobs to get updated status
            this.loadJobs();
          },
          error: (err) => {
            console.error('Error submitting job', err);
            this.confirmationDialogService.confirm({
              title: 'Error',
              message: 'Failed to submit job. Please try again.',
              confirmText: 'OK',
              cancelText: '',
              isDangerous: false
            });
          }
        });
      }
    } catch (error) {
      console.error('Error showing confirmation dialog', error);
    }
  }

  // Context menu methods
  toggleContextMenu(event: MouseEvent, job: any): void {
    event.stopPropagation();
    // Toggle the menu - close if already open, open if closed
    if (this.activeContextMenuJob === job.jobUuid) {
      this.activeContextMenuJob = null;
      this.menuPosition = null;
    } else {
      this.activeContextMenuJob = job.jobUuid;
      
      // Calculate position based on the click event
      // We want to position it near the "..." button that was clicked
      const rect = (event.target as HTMLElement).getBoundingClientRect();
      this.menuPosition = {
        top: rect.bottom + window.scrollY,
        left: rect.right - 150 + window.scrollX // 150 is the approximate width of the menu
      };
    }
  }
  
  // Helper method to get the currently active job
  getActiveJob(): any {
    return this.jobs.find(j => j.jobUuid === this.activeContextMenuJob);
  }

  // Close the context menu when clicking outside
  @HostListener('document:click')
  closeContextMenu(): void {
    this.activeContextMenuJob = null;
    this.menuPosition = null;
  }
}
