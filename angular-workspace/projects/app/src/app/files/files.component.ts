import { Component, OnInit, HostListener } from '@angular/core';
import { Router } from '@angular/router';
import { FileService } from './file.service';
import { TableConfig, TableSortEvent, TablePageEvent } from '../shared/components/generic-table/table-models';
import { ConfirmationDialogService } from '../shared/services/confirmation-dialog.service';

@Component({
  selector: 'app-files',
  templateUrl: './files.component.html',
  styleUrls: ['./files.component.scss']
})
export class FilesComponent implements OnInit {
  files: any[] = [];
  loading = false;
  error = false;
  
  // Pagination properties
  currentPage = 0;
  pageSize = 10;
  totalItems = 0;
  totalPages = 0;
  
  // Sorting properties
  sortBy = 'createdAt';
  sortDirection: 'asc' | 'desc' = 'desc';
  
  // Filter properties
  fileTypeFilter?: string = 'UPLOAD';
  statusFilter?: string;
  
  // Options for dropdowns
  fileTypes = [ 
    { value: 'UPLOAD', label: 'Uploaded Files' },
    { value: 'RESULT', label: 'Job Result Files' }
  ];
  
  statusTypes = [ 
    { value: 'READY', label: 'Available for Jobs' },
    { value: 'VALIDATING', label: 'Pending Validation' },
    { value: 'PROCESSING', label: 'Processing' },
    { value: 'FAILED', label: 'Failed' }
  ];
  
  // Context menu control
  activeContextMenuFile: string | null = null;
  
  // Table configuration
  tableConfig: TableConfig<any> = {
    columns: [
      { key: 'fileName', header: 'File Name', sortable: true },
      { key: 'type', header: 'Type', sortable: true },
      { key: 'status', header: 'Status', sortable: true, cellTemplate: 'statusTemplate' },
      { key: 'createdAt', header: 'Created', sortable: true, 
        cellFormatter: (item) => new Date(item.createdAt).toLocaleString() },
      { key: 'fileSize', header: 'Size', sortable: false, 
        cellFormatter: (item) => `${item.fileSize.toLocaleString()} bytes` },
      { key: 'actions', header: 'Actions', sortable: false, cellTemplate: 'actionsTemplate' }
    ],
    defaultSortField: 'createdAt',
    defaultSortDirection: 'desc'
  };
  
  paginationConfig = {
    pageSize: 10,
    pageSizeOptions: [5, 10, 20, 50]
  };
  
  constructor(
    private fileService: FileService,
    private router: Router,
    private confirmationDialogService: ConfirmationDialogService
  ) { }

  ngOnInit(): void {
    this.loadFiles();
  }

  loadFiles(): void {
    this.loading = true;
    this.error = false;
    
    this.fileService.getUserFiles(
      undefined, // Assuming we want to load files for the current user
      this.currentPage,
      this.pageSize,
      this.sortBy,
      this.sortDirection,
      this.fileTypeFilter,
      this.statusFilter
    ).subscribe({
      next: (data) => {
        this.files = data.content;
        this.totalItems = data.totalElements;
        this.totalPages = data.totalPages;
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading files:', error);
        this.error = true;
        this.loading = false;
      }
    });
  }

  refreshFiles(): void {
    this.loadFiles();
  }
  
  // Handle pagination events from the generic table
  onPageChange(event: TablePageEvent): void {
    this.currentPage = event.pageIndex;
    this.pageSize = event.pageSize;
    this.loadFiles();
  }
  
  // Handle sort events from the generic table
  onSortChange(event: TableSortEvent): void {
    this.sortBy = event.field;
    this.sortDirection = event.direction;
    this.currentPage = 0; // Reset to first page
    this.loadFiles();
  }
  
  applyFilters(): void {
    this.currentPage = 0; // Reset to first page when filtering
    this.loadFiles();
  }
  
  clearFilters(): void {
    this.fileTypeFilter = undefined;
    this.statusFilter = undefined;
    this.currentPage = 0; // Reset to first page when clearing filters
    this.loadFiles();
  }
  
  // File actions
  createJob(file: any): void {
    if (file && file.fileUuid) {
      this.router.navigate(['/dashboard/jobs/submit', file.fileUuid]);
    }
  }

  async deleteFile(file: any): Promise<void> {
    if (!file || !file.fileUuid) return;
    
    try {
      const confirmed = await this.confirmationDialogService.confirmDelete(file.fileName);
      
      if (confirmed) {
        this.fileService.deleteFile(file.fileUuid).subscribe({
          next: () => {
            this.refreshFiles();
          },
          error: (error: any) => {
            console.error('Error deleting file:', error);
            let errorMessage = 'Failed to delete file. Please try again.';
            
            // Handle specific error cases
            if (error.status === 409) {
              // Conflict - business rule violation
              errorMessage = error.error || 'Cannot delete this file due to active jobs or current processing state.';
            } else if (error.status === 403) {
              errorMessage = 'You do not have permission to delete this file.';
            } else if (error.status === 404) {
              errorMessage = 'File not found.';
            }
            
            this.confirmationDialogService.confirm({
              title: 'Cannot Delete File',
              message: errorMessage,
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

  navigateToFileStatus(file: any): void {
    if (file && file.fileUuid) {
      this.router.navigate(['/dashboard/files/status', file.fileUuid]);
    }
  }

  // Context menu properties
  menuPosition: { top: number, left: number } | null = null;
  
  // Context menu methods
  toggleContextMenu(event: MouseEvent, file: any): void {
    event.stopPropagation();
    // Toggle the menu - close if already open, open if closed
    if (this.activeContextMenuFile === file.fileUuid) {
      this.activeContextMenuFile = null;
      this.menuPosition = null;
    } else {
      this.activeContextMenuFile = file.fileUuid;
      
      // Calculate position based on the click event
      // We want to position it near the "..." button that was clicked
      const rect = (event.target as HTMLElement).getBoundingClientRect();
      this.menuPosition = {
        top: rect.bottom + window.scrollY,
        left: rect.right - 150 + window.scrollX // 150 is the approximate width of the menu
      };
    }
  }
  
  // Helper method to get the currently active file
  getActiveFile(): any {
    return this.files.find(f => f.fileUuid === this.activeContextMenuFile);
  }

  // Close the context menu when clicking outside
  @HostListener('document:click')
  closeContextMenu(): void {
    this.activeContextMenuFile = null;
  }

  // Handle file download
  downloadFile(file: any): void {
    if (file && file.fileUuid && file.status !== 'PROCESSING') {
      this.fileService.downloadFile(file.fileUuid, file.fileName).subscribe({
        next: (response: any) => {
          // Create a blob from the response
          const blob = new Blob([response], { type: 'application/octet-stream' });
          
          // Create a temporary link element to trigger the download
          const link = document.createElement('a');
          link.href = window.URL.createObjectURL(blob);
          link.download = file.fileName;
          
          // Append to body, click the link, then remove it
          document.body.appendChild(link);
          link.click();
          document.body.removeChild(link);
        },
        error: (error: any) => {
          console.error('Error downloading file:', error);
          alert('Failed to download file. Please try again.');
        }
      });
    }
  }
  
  // Handle row click event
  onRowClick(file: any): void {
    this.navigateToFileStatus(file);
  }
}
