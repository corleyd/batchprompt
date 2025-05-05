# Generic Table Component

A reusable, configurable table component that provides consistent UI, sorting, and pagination functionality across the application.

## Features

- Configurable columns with custom header and content
- Sorting functionality with customizable sort behavior
- Pagination with configurable page sizes
- Loading, empty, and error states with customizable templates
- Fully typed with generics for type-safety

## Basic Usage

```typescript
// In your component TS file
import { Component } from '@angular/core';
import { TableConfig } from '../../shared/components/generic-table/table-models';

@Component({
  selector: 'app-your-component',
  templateUrl: './your-component.component.html'
})
export class YourComponent {
  // Your data array
  items: YourDataType[] = [];
  
  // Table configuration
  tableConfig: TableConfig<YourDataType> = {
    columns: [
      { key: 'id', header: 'ID', sortable: true },
      { key: 'name', header: 'Name', sortable: true },
      { key: 'status', header: 'Status', sortable: true, cellTemplate: 'statusTemplate' },
      { key: 'createdAt', header: 'Created', sortable: true, 
        cellFormatter: (item) => new Date(item.createdAt).toLocaleString() }
    ],
    defaultSortField: 'createdAt',
    defaultSortDirection: 'desc'
  };

  // Pagination settings
  paginationConfig = {
    pageSize: 10,
    pageSizeOptions: [5, 10, 20, 50]
  };
  
  // Table state variables
  loading = false;
  error = false;
  currentPage = 0;
  totalItems = 0;
  totalPages = 0;
  sortField = 'createdAt';
  sortDirection = 'desc';
  
  // Event handlers
  onPageChange(event: TablePageEvent): void {
    this.currentPage = event.pageIndex;
    this.loadData();
  }
  
  onSortChange(event: TableSortEvent): void {
    this.sortField = event.field;
    this.sortDirection = event.direction;
    this.currentPage = 0;
    this.loadData();
  }
  
  loadData(): void {
    // Implement your data loading logic here
  }
}
```

```html
<!-- In your component HTML file -->
<div>
  <h1>Your Page Title</h1>
  
  <!-- Custom cell templates -->
  <ng-template #statusTemplate let-item>
    <span class="status-badge" [ngClass]="getStatusClass(item.status)">
      {{ item.status }}
    </span>
  </ng-template>
  
  <!-- Generic Table -->
  <app-generic-table
    [data]="items"
    [config]="tableConfig"
    [loading]="loading"
    [error]="error"
    [totalItems]="totalItems"
    [totalPages]="totalPages"
    [currentPage]="currentPage"
    [paginationConfig]="paginationConfig"
    [sortField]="sortField"
    [sortDirection]="sortDirection"
    [customCellTemplates]="{
      'statusTemplate': statusTemplate
    }"
    (pageChange)="onPageChange($event)"
    (sortChange)="onSortChange($event)"
    (refresh)="loadData()"
  ></app-generic-table>
</div>
```

## API Reference

### Inputs

| Input | Type | Description |
|-------|------|-------------|
| data | T[] | Array of data items to display in the table |
| config | TableConfig<T> | Configuration object for the table |
| loading | boolean | Whether the table is in a loading state |
| error | boolean | Whether an error occurred while loading data |
| totalItems | number | Total number of items across all pages |
| totalPages | number | Total number of pages |
| currentPage | number | Current active page (0-based) |
| paginationConfig | TablePaginationConfig | Configuration for pagination |
| sortField | string | Current field being sorted |
| sortDirection | 'asc' \| 'desc' | Current sort direction |
| emptyStateTemplate | TemplateRef<any> | Custom template for empty state |
| loadingTemplate | TemplateRef<any> | Custom template for loading state |
| errorTemplate | TemplateRef<any> | Custom template for error state |
| customCellTemplates | {[key: string]: TemplateRef<any>} | Map of custom cell templates |

### Outputs

| Output | Type | Description |
|--------|------|-------------|
| pageChange | EventEmitter<TablePageEvent> | Emitted when the page changes |
| sortChange | EventEmitter<TableSortEvent> | Emitted when the sort changes |
| refresh | EventEmitter<void> | Emitted when the refresh button is clicked |
| rowClick | EventEmitter<T> | Emitted when a row is clicked |

### Models

```typescript
export interface TableColumn<T> {
  key: string;
  header: string;
  sortable?: boolean;
  cellTemplate?: string;
  cellFormatter?: (item: T) => string;
  sortFn?: (a: T, b: T) => number;
  cssClass?: string;
}

export interface TableConfig<T> {
  columns: TableColumn<T>[];
  defaultSortField?: string;
  defaultSortDirection?: 'asc' | 'desc';
}

export interface TablePaginationConfig {
  pageSize: number;
  pageSizeOptions: number[];
}

export interface TableSortEvent {
  field: string;
  direction: 'asc' | 'desc';
}

export interface TablePageEvent {
  pageIndex: number;
  pageSize: number;
}
```