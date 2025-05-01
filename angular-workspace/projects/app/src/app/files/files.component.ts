import { Component, OnInit, HostListener } from '@angular/core';
import { Router } from '@angular/router';
import { FileService } from './file.service';

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
  sortDirection = 'desc';
  
  // Filter properties
  fileTypeFilter?: string;
  statusFilter?: string;
  
  // Options for dropdowns
  fileTypes = ['UPLOAD', 'RESULT'];
  statusTypes = ['READY', 'VALIDATION', 'PROCESSING', 'FAILED'];
  
  // Context menu control
  activeContextMenuFile: string | null = null;
  
  constructor(
    private fileService: FileService,
    private router: Router
  ) { }

  ngOnInit(): void {
    this.loadFiles();
  }

  loadFiles(): void {
    this.loading = true;
    this.error = false;
    
    this.fileService.getUserFiles(
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
  
  changePage(page: number): void {
    this.currentPage = page;
    this.loadFiles();
  }
  
  changePageSize(size: number): void {
    this.pageSize = size;
    this.currentPage = 0; // Reset to first page when changing page size
    this.loadFiles();
  }
  
  sort(column: string): void {
    if (this.sortBy === column) {
      // Toggle direction if already sorting by this column
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortBy = column;
      this.sortDirection = 'asc'; // Default to ascending when switching columns
    }
    this.currentPage = 0; // Reset to first page when sorting
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
  
  getPages(): number[] {
    return Array(this.totalPages).fill(0).map((_, i) => i);
  }

  // New methods for the updated template
  createJob(file: any): void {
    if (file && file.fileUuid) {
      this.router.navigate(['/dashboard/jobs/submit', file.fileUuid]);
    }
  }

  deleteFile(file: any): void {
    if (file && file.fileUuid && confirm('Are you sure you want to delete this file?')) {
      this.fileService.deleteFile(file.fileUuid).subscribe({
        next: () => {
          this.refreshFiles();
        },
        error: (error: any) => {
          console.error('Error deleting file:', error);
          alert('Failed to delete file. Please try again.');
        }
      });
    }
  }

  navigateToFileStatus(file: any): void {
    if (file && file.fileUuid) {
      this.router.navigate(['/dashboard/files/status', file.fileUuid]);
    }
  }

  // Context menu methods
  toggleContextMenu(event: MouseEvent, file: any): void {
    event.stopPropagation();
    // Toggle the menu - close if already open, open if closed
    this.activeContextMenuFile = this.activeContextMenuFile === file.fileUuid ? null : file.fileUuid;
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
}
