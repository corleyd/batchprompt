import { Component, OnInit, TemplateRef, ViewChild } from '@angular/core';
import { UserService } from '../../../services/user.service';
import { TableConfig, TableColumn, TableSortEvent, TablePageEvent } from '../../../shared/components/generic-table/table-models';

@Component({
  selector: 'app-users',
  templateUrl: './users.component.html',
  styleUrls: ['./users.component.scss']
})
export class UsersComponent implements OnInit {
  users: any[] = [];
  loading = false;
  error = false;
  
  // Pagination
  page = 0;
  size = 10;
  totalUsers = 0;
  
  // Search & Filters
  searchTerm = '';
  roleFilter = '';
  statusFilter = '';
  
  // Table configuration
  tableConfig!: TableConfig<any>;
  sortBy = 'name';
  sortDirection: 'asc' | 'desc' = 'asc';
  
  // Make Math available to the template
  Math = Math;
  
  @ViewChild('statusTemplate', { static: true }) statusTemplate!: TemplateRef<any>;
  @ViewChild('roleTemplate', { static: true }) roleTemplate!: TemplateRef<any>;
  @ViewChild('actionsTemplate', { static: true }) actionsTemplate!: TemplateRef<any>;

  constructor(private userService: UserService) {}

  ngOnInit(): void {
    this.initializeTableConfig();
    this.loadUsers();
  }

  initializeTableConfig(): void {
    this.tableConfig = {
      columns: [
        { key: 'userUuid', header: 'ID', sortable: true },
        { key: 'name', header: 'Name', sortable: true },
        { key: 'email', header: 'Email', sortable: true },
        { 
          key: 'role', 
          header: 'Role', 
          sortable: true,
          cellTemplate: 'roleTemplate'
        },
        { 
          key: 'enabled', 
          header: 'Status', 
          sortable: true,
          cellTemplate: 'statusTemplate'
        },
        { 
          key: 'actions', 
          header: 'Actions',
          sortable: false,
          cellTemplate: 'actionsTemplate'
        }
      ],
      defaultSortField: 'name',
      defaultSortDirection: 'asc'
    };
  }

  loadUsers(): void {
    this.loading = true;
    this.userService.getAllUsers(this.page, this.size, this.sortBy, this.sortDirection).subscribe({
      next: (response) => {
        this.users = response.content;
        this.totalUsers = response.totalElements;
        this.loading = false;
        this.error = false;
      },
      error: (error) => {
        console.error('Error loading users:', error);
        this.loading = false;
        this.error = true;
      }
    });
  }

  searchUsers(): void {
    if (this.searchTerm.trim()) {
      this.loading = true;
      this.userService.searchUsersByName(this.searchTerm, this.page, this.size, this.sortBy, this.sortDirection).subscribe({
        next: (response) => {
          this.users = response.content;
          this.totalUsers = response.totalElements;
          this.loading = false;
          this.error = false;
        },
        error: (error) => {
          console.error('Error searching users:', error);
          this.loading = false;
          this.error = true;
        }
      });
    } else {
      this.loadUsers();
    }
  }

  applyFilters(): void {
    // In a real implementation, these filters would be passed to the backend
    // For now we'll just reload the users
    this.page = 0; // Reset to first page when applying filters
    this.loadUsers();
  }

  onPageChange(event: TablePageEvent): void {
    this.page = event.pageIndex;
    this.size = event.pageSize;
    this.loadUsers();
  }

  onSortChange(event: TableSortEvent): void {
    this.sortBy = event.field;
    this.sortDirection = event.direction;
    this.page = 0; // Reset to first page when sorting changes
    this.loadUsers();
  }

  onRefresh(): void {
    this.loadUsers();
  }

  onUserRowClick(user: any): void {
    // Optional: handle row click event
    console.log('User clicked:', user);
  }

  deleteUser(userUuid: string, event: Event): void {
    event.stopPropagation(); // Prevent row click event
    if (confirm('Are you sure you want to delete this user?')) {
      this.userService.deleteUser(userUuid).subscribe({
        next: () => {
          this.loadUsers();
        },
        error: (error) => {
          console.error('Error deleting user:', error);
        }
      });
    }
  }

  editUser(userUuid: string, event: Event): void {
    event.stopPropagation(); // Prevent row click event
    // Implementation for editing a user
    console.log('Edit user:', userUuid);
  }
}