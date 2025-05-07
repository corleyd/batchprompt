import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { UserService } from '../../../services/user.service';
import { FileService } from '../../../files/file.service';
import { JobService } from '../../../services/job.service';
import { PromptService } from '../../../services/prompt.service';
import { MatTabsModule } from '@angular/material/tabs';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { GenericTableModule } from '../../../shared/components/generic-table/generic-table.module';
import { TableConfig, TableSortEvent, TablePageEvent } from '../../../shared/components/generic-table/table-models';

@Component({
  selector: 'app-user-details',
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    MatTabsModule,
    MatButtonModule,
    MatIconModule,
    GenericTableModule
  ],
  templateUrl: './user-details.component.html',
  styleUrls: ['./user-details.component.scss']
})
export class UserDetailsComponent implements OnInit {
  userId: string | null = null;
  userName: string = '';
  userEmail: string = '';
  userFiles: any[] = [];
  userJobs: any[] = [];
  userPrompts: any[] = [];
  
  loadingUser = true;
  loadingFiles = true;
  loadingJobs = true;
  loadingPrompts = true;
  
  activeTab = 0; // 0: files, 1: jobs, 2: prompts

  // Pagination
  filesPage = 0;
  filesPageSize = 10;
  filesTotalPages = 0;
  filesTotalItems = 0;
  
  jobsPage = 0;
  jobsPageSize = 10;
  jobsTotalPages = 0;
  jobsTotalItems = 0;
  
  promptsPage = 0;
  promptsPageSize = 10;
  promptsTotalPages = 0;
  promptsTotalItems = 0;
  
  // Sorting
  filesSortField = 'createdAt';
  filesSortDirection: 'asc' | 'desc' = 'desc';
  
  jobsSortField = 'createdAt';
  jobsSortDirection: 'asc' | 'desc' = 'desc';
  
  promptsSortField = 'createTimestamp';
  promptsSortDirection: 'asc' | 'desc' = 'desc';
  
  // Table configurations
  filesTableConfig: TableConfig<any> = {
    columns: [
      { key: 'fileName', header: 'File Name', sortable: true },
      { key: 'fileType', header: 'Type', sortable: true },
      { key: 'status', header: 'Status', sortable: true, 
        cellFormatter: (item) => item.status,
        cssClass: 'status-cell' },
      { key: 'createdAt', header: 'Created', sortable: true,
        cellFormatter: (item) => new Date(item.createdAt).toLocaleString() },
      { key: 'actions', header: 'Actions', sortable: false, cellTemplate: 'fileActions' }
    ],
    defaultSortField: 'createdAt',
    defaultSortDirection: 'desc'
  };
  
  jobsTableConfig: TableConfig<any> = {
    columns: [
      { key: 'jobUuid', header: 'ID', sortable: true },
      { key: 'fileName', header: 'File Name', sortable: true },
      { key: 'modelId', header: 'Model', sortable: true },
      { key: 'status', header: 'Status', sortable: true,
        cellFormatter: (item) => item.status,
        cssClass: 'status-cell' },
      { key: 'createdAt', header: 'Created', sortable: true,
        cellFormatter: (item) => new Date(item.createdAt).toLocaleString() },
      { key: 'actions', header: 'Actions', sortable: false, cellTemplate: 'jobActions' }
    ],
    defaultSortField: 'createdAt',
    defaultSortDirection: 'desc'
  };
  
  promptsTableConfig: TableConfig<any> = {
    columns: [
      { key: 'name', header: 'Name', sortable: true },
      { key: 'description', header: 'Description', sortable: true },
      { key: 'outputMethod', header: 'Output Method', sortable: true },
      { key: 'createTimestamp', header: 'Created', sortable: true,
        cellFormatter: (item) => new Date(item.createTimestamp).toLocaleString() },
      { key: 'actions', header: 'Actions', sortable: false, cellTemplate: 'promptActions' }
    ],
    defaultSortField: 'createTimestamp',
    defaultSortDirection: 'desc'
  };

  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private userService: UserService,
    private fileService: FileService,
    private jobService: JobService,
    private promptService: PromptService
  ) { }

  ngOnInit(): void {
    this.route.paramMap.subscribe(params => {
      this.userId = params.get('userId');
      if (this.userId) {
        this.loadUserDetails();
        this.loadUserFiles();
        this.loadUserJobs();
        this.loadUserPrompts();
      } else {
        this.router.navigate(['/admin/users']);
      }
    });
  }

  loadUserDetails(): void {
    if (!this.userId) return;
    
    this.loadingUser = true;
    this.userService.getUserByAuth0Id(this.userId).subscribe({
      next: (user) => {
        this.userName = user.name;
        this.userEmail = user.email;
        this.loadingUser = false;
      },
      error: (error) => {
        console.error('Error loading user details:', error);
        this.loadingUser = false;
      }
    });
  }

  loadUserFiles(): void {
    if (!this.userId) return;
    
    this.loadingFiles = true;
    this.fileService.getUserFiles(this.userId, this.filesPage, this.filesPageSize, 
                                 this.filesSortField, this.filesSortDirection).subscribe({
      next: (response) => {
        this.userFiles = response.content || [];
        this.filesTotalItems = response.totalElements || this.userFiles.length;
        this.filesTotalPages = response.totalPages || 1;
        this.loadingFiles = false;
      },
      error: (error) => {
        console.error('Error loading user files:', error);
        this.loadingFiles = false;
      }
    });
  }

  loadUserJobs(): void {
    if (!this.userId) return;
    
    this.loadingJobs = true;
    this.jobService.getUserJobs(this.userId, this.jobsPage, this.jobsPageSize,
                               this.jobsSortField, this.jobsSortDirection).subscribe({
      next: (response) => {
        this.userJobs = response.content || [];
        this.jobsTotalItems = response.totalElements || this.userJobs.length;
        this.jobsTotalPages = response.totalPages || 1;
        this.loadingJobs = false;
      },
      error: (error) => {
        console.error('Error loading user jobs:', error);
        this.loadingJobs = false;
      }
    });
  }

  loadUserPrompts(): void {
    if (!this.userId) return;
    
    this.loadingPrompts = true;
    this.promptService.getUserPrompts(this.userId, this.promptsPage, this.promptsPageSize,
                                     this.promptsSortField, this.promptsSortDirection).subscribe({
      next: (response) => {
        this.userPrompts = response.content || [];
        this.promptsTotalItems = response.totalElements || this.userPrompts.length;
        this.promptsTotalPages = response.totalPages || 1;
        this.loadingPrompts = false;
      },
      error: (error) => {
        console.error('Error loading user prompts:', error);
        this.loadingPrompts = false;
      }
    });
  }

  onTabChange(event: any): void {
    this.activeTab = event.index;
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

  goBack(): void {
    this.router.navigate(['/admin/users']);
  }

  getUserInitials(): string {
    if (!this.userName) {
      return 'U';
    }
    
    const nameParts = this.userName.split(' ');
    if (nameParts.length > 1) {
      // Get first letter of first and last name
      return (nameParts[0][0] + nameParts[nameParts.length - 1][0]).toUpperCase();
    } else {
      // If only one name, return the first letter
      return nameParts[0][0].toUpperCase();
    }
  }

  // Handle sorting events
  onFilesSortChange(event: TableSortEvent): void {
    this.filesSortField = event.field;
    this.filesSortDirection = event.direction;
    this.loadUserFiles();
  }

  onJobsSortChange(event: TableSortEvent): void {
    this.jobsSortField = event.field;
    this.jobsSortDirection = event.direction;
    this.loadUserJobs();
  }

  onPromptsSortChange(event: TableSortEvent): void {
    this.promptsSortField = event.field;
    this.promptsSortDirection = event.direction;
    this.loadUserPrompts();
  }

  // Handle pagination events
  onFilesPageChange(event: TablePageEvent): void {
    this.filesPage = event.pageIndex;
    this.filesPageSize = event.pageSize;
    this.loadUserFiles();
  }

  onJobsPageChange(event: TablePageEvent): void {
    this.jobsPage = event.pageIndex;
    this.jobsPageSize = event.pageSize;
    this.loadUserJobs();
  }

  onPromptsPageChange(event: TablePageEvent): void {
    this.promptsPage = event.pageIndex;
    this.promptsPageSize = event.pageSize;
    this.loadUserPrompts();
  }

  // Handle refresh
  refreshFiles(): void {
    this.loadUserFiles();
  }

  refreshJobs(): void {
    this.loadUserJobs();
  }

  refreshPrompts(): void {
    this.loadUserPrompts();
  }
}
