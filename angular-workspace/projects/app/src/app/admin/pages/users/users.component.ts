import { Component, OnInit, TemplateRef, ViewChild } from '@angular/core';
import { UserService } from '../../../services/user.service';
import { FileService } from '../../../files/file.service';
import { JobService } from '../../../services/job.service';
import { PromptService } from '../../../services/prompt.service';
import { Router } from '@angular/router';
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

  // User's files, jobs, and prompts tracking
  selectedUserId: string | null = null;
  userFiles: any[] = [];
  userJobs: any[] = [];
  userPrompts: any[] = [];
  loadingUserResources = false;
  showUserResources = false;
  activeResourceTab = 'files'; // 'files', 'jobs', or 'prompts'

  constructor(
    private userService: UserService,
    private fileService: FileService,
    private jobService: JobService,
    private promptService: PromptService,
    private router: Router
  ) {}

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
    // Navigate to user details page when a row is clicked
    this.router.navigate(['/admin/users', user.userId]);
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

  viewUserFiles(userId: string, event: Event): void {
    event.stopPropagation(); // Prevent row click event
    this.selectedUserId = userId;
    this.activeResourceTab = 'files';
    this.showUserResources = true;
    this.loadUserFiles(userId);
  }

  viewUserJobs(userId: string, event: Event): void {
    event.stopPropagation(); // Prevent row click event
    this.selectedUserId = userId;
    this.activeResourceTab = 'jobs';
    this.showUserResources = true;
    this.loadUserJobs(userId);
  }

  viewUserPrompts(userId: string, event: Event): void {
    event.stopPropagation(); // Prevent row click event
    this.selectedUserId = userId;
    this.activeResourceTab = 'prompts';
    this.showUserResources = true;
    this.loadUserPrompts(userId);
  }

  loadUserFiles(userId: string): void {
    this.loadingUserResources = true;
    this.userFiles = [];
    
    // Using the admin endpoint to get files for a specific user
    // Note: This assumes your backend has an admin endpoint to get files by userId
    this.fileService.getUserFiles(userId).subscribe({
      next: (response) => {
        this.userFiles = response.content || response;
        this.loadingUserResources = false;
      },
      error: (error) => {
        console.error('Error loading user files:', error);
        this.loadingUserResources = false;
      }
    });
  }

  loadUserJobs(userId: string): void {
    this.loadingUserResources = true;
    this.userJobs = [];
    
    // Using the admin endpoint to get jobs for a specific user
    this.jobService.getUserJobs(userId).subscribe({
      next: (response) => {
        this.userJobs = response.content || response;
        this.loadingUserResources = false;
      },
      error: (error) => {
        console.error('Error loading user jobs:', error);
        this.loadingUserResources = false;
      }
    });
  }

  loadUserPrompts(userId: string): void {
    this.loadingUserResources = true;
    this.userPrompts = [];
    
    // Using the admin endpoint to get prompts for a specific user
    this.promptService.getUserPrompts(userId).subscribe({
      next: (prompts) => {
        this.userPrompts = prompts;
        this.loadingUserResources = false;
      },
      error: (error) => {
        console.error('Error loading user prompts:', error);
        this.loadingUserResources = false;
      }
    });
  }

  closeUserResources(): void {
    this.showUserResources = false;
    this.selectedUserId = null;
  }

  changeResourceTab(tab: string): void {
    this.activeResourceTab = tab;
    if (this.selectedUserId) {
      if (tab === 'files') {
        this.loadUserFiles(this.selectedUserId);
      } else if (tab === 'jobs') {
        this.loadUserJobs(this.selectedUserId);
      } else if (tab === 'prompts') {
        this.loadUserPrompts(this.selectedUserId);
      }
    }
  }

  downloadFile(fileUuid: string, fileName: string): void {
    this.fileService.downloadFile(fileUuid, fileName).subscribe({
      next: () => {
        // Download is handled by the service
      },
      error: (error) => {
        console.error('Error downloading file:', error);
        alert('Failed to download file. Please try again.');
      }
    });
  }

  viewJobDetails(jobUuid: string): void {
    // Navigate to job details page
    this.router.navigate(['/dashboard/jobs', jobUuid]);
  }

  viewPromptDetails(promptUuid: string): void {
    // Navigate to prompt details page
    this.router.navigate(['/dashboard/prompts/edit', promptUuid]);
  }
}