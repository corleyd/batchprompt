import { Component, Input, Output, EventEmitter, OnInit, OnChanges, SimpleChanges, TemplateRef, ContentChildren, QueryList } from '@angular/core';
import { TableColumn, TableConfig, TableSortEvent, TablePageEvent, TablePaginationConfig } from './table-models';

@Component({
  selector: 'app-generic-table',
  templateUrl: './generic-table.component.html',
  styleUrls: ['./generic-table.component.scss']
})
export class GenericTableComponent<T> implements OnInit, OnChanges {
  @Input() data: T[] = [];
  @Input() config!: TableConfig<T>;
  @Input() loading = false;
  @Input() error = false;
  
  // Pagination
  @Input() totalItems = 0;
  @Input() totalPages = 0;
  @Input() currentPage = 0;
  @Input() paginationConfig: TablePaginationConfig = {
    pageSize: 10,
    pageSizeOptions: [5, 10, 20, 50]
  };

  // Sorting
  @Input() sortField: string = '';
  @Input() sortDirection: 'asc' | 'desc' = 'desc';

  // Events
  @Output() pageChange = new EventEmitter<TablePageEvent>();
  @Output() sortChange = new EventEmitter<TableSortEvent>();
  @Output() refresh = new EventEmitter<void>();
  @Output() rowClick = new EventEmitter<T>();

  // Custom templates
  @Input() emptyStateTemplate?: TemplateRef<any>;
  @Input() loadingTemplate?: TemplateRef<any>;
  @Input() errorTemplate?: TemplateRef<any>;
  @Input() customCellTemplates: {[key: string]: TemplateRef<any>} = {};

  ngOnInit() {
    // Set default sort from config
    if (this.config && this.config.defaultSortField) {
      this.sortField = this.config.defaultSortField;
      this.sortDirection = this.config.defaultSortDirection || 'desc';
    }
  }

  ngOnChanges(changes: SimpleChanges) {
    // Respond to changes in inputs if needed
  }

  updateSort(field: string): void {
    if (this.sortField === field) {
      // Toggle direction if clicking the same column
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      // Default to descending when changing columns
      this.sortField = field;
      this.sortDirection = 'desc';
    }
    
    // Emit sort event
    this.sortChange.emit({
      field: this.sortField,
      direction: this.sortDirection
    });
  }

  getSortIcon(field: string): string {
    if (this.sortField !== field) {
      return 'sort';
    }
    return this.sortDirection === 'asc' ? 'arrow_upward' : 'arrow_downward';
  }

  // Pagination methods
  goToPage(page: number): void {
    if (page >= 0 && page < this.totalPages) {
      this.currentPage = page;
      this.pageChange.emit({
        pageIndex: this.currentPage,
        pageSize: this.paginationConfig.pageSize
      });
    }
  }

  previousPage(): void {
    if (this.currentPage > 0) {
      this.currentPage--;
      this.pageChange.emit({
        pageIndex: this.currentPage,
        pageSize: this.paginationConfig.pageSize
      });
    }
  }

  nextPage(): void {
    if (this.currentPage < this.totalPages - 1) {
      this.currentPage++;
      this.pageChange.emit({
        pageIndex: this.currentPage,
        pageSize: this.paginationConfig.pageSize
      });
    }
  }

  onPageSizeChange(event: Event): void {
    const selectElement = event.target as HTMLSelectElement;
    this.paginationConfig.pageSize = parseInt(selectElement.value, 10);
    this.currentPage = 0; // Reset to first page
    this.pageChange.emit({
      pageIndex: this.currentPage,
      pageSize: this.paginationConfig.pageSize
    });
  }

  refreshData(): void {
    this.refresh.emit();
  }

  onRowClick(item: T): void {
    this.rowClick.emit(item);
  }

  getCellValue(item: T, column: TableColumn<T>): string {
    if (column.cellFormatter) {
      return column.cellFormatter(item);
    }
    
    // Handle nested properties using dot notation (e.g., "user.name")
    return this.getPropertyValueByPath(item, column.key);
  }

  private getPropertyValueByPath(obj: any, path: string): string {
    return path.split('.').reduce((prev, curr) => {
      return prev ? prev[curr] : '';
    }, obj) || '';
  }

  // Check if the column has a custom template
  hasCustomTemplate(column: TableColumn<T>): boolean {
    return column.cellTemplate !== undefined && 
           this.customCellTemplates[column.cellTemplate] !== undefined;
  }

  // Get template context
  getTemplateContext(item: T) {
    return { $implicit: item, item };
  }
}