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

export interface PaginatedResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  number: number;
  size: number;
}

export interface TableSortEvent {
  field: string;
  direction: 'asc' | 'desc';
}

export interface TablePageEvent {
  pageIndex: number;
  pageSize: number;
}