import { Component, OnInit } from '@angular/core';
import { FileService } from './file.service';

@Component({
  selector: 'app-files',
  templateUrl: './files.component.html',
  styleUrls: ['./files.component.scss']
})
export class FilesComponent implements OnInit {
  files: any[] = [];
  loading = false;
  
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
  
  constructor(private fileService: FileService) { }

  ngOnInit(): void {
    this.loadFiles();
  }

  loadFiles(): void {
    this.loading = true;
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
}
