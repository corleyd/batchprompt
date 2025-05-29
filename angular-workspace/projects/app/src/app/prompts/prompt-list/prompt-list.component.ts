import { CommonModule } from '@angular/common';
import { Component, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '@auth0/auth0-angular';
import { switchMap } from 'rxjs';
import { IconsModule } from '../../icons/icons.module';
import { Prompt } from '../../models/prompt.model';
import { PromptService } from '../../services/prompt.service';

@Component({
  selector: 'app-prompt-list',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule, IconsModule],
  templateUrl: './prompt-list.component.html',
  styleUrls: ['./prompt-list.component.scss']
})
export class PromptListComponent implements OnInit {
  prompts: Prompt[] = [];
  loading = true;
  error = false;
  
  // Pagination
  currentPage = 0;
  pageSize = 10;
  totalItems = 0;
  totalPages = 0;
  
  // Sorting
  sortField = 'createTimestamp';
  sortDirection = 'desc';
  
  // Available sort options
  sortOptions = [
    { value: 'name', label: 'Name' },
    { value: 'createTimestamp', label: 'Created Date' },
    { value: 'lastJobRunTimestamp', label: 'Last Job Run' },
    { value: 'jobRunCount', label: 'Job Runs' }
  ];

  constructor(
    private promptService: PromptService,
    public auth: AuthService,
    private router: Router
  ) {}

  ngOnInit(): void {
    this.loadPrompts();
  }

  loadPrompts(): void {
    this.loading = true;
    this.auth.user$
      .pipe(
        switchMap(user => {
          if (user?.sub) {
            return this.promptService.getUserPrompts(
              user.sub, 
              this.currentPage, 
              this.pageSize, 
              this.sortField, 
              this.sortDirection
            );
          } else {
            return this.promptService.getAllPrompts();
          }
        })
      )
      .subscribe({
        next: (response) => {
          // Handle paginated response
          if (response.content) {
            // This is a paginated response
            this.prompts = response.content;
            this.totalItems = response.totalElements;
            this.totalPages = response.totalPages;
          } else {
            // This is just an array (from getAllPrompts)
            this.prompts = response;
          }
          this.loading = false;
        },
        error: (err) => {
          console.error('Error loading prompts', err);
          this.error = true;
          this.loading = false;
        }
      });
  }

  deletePrompt(promptUuid: string): void {
    if (confirm('Are you sure you want to delete this prompt?')) {
      this.promptService.deletePrompt(promptUuid).subscribe({
        next: () => {
          this.prompts = this.prompts.filter(p => p.promptUuid !== promptUuid);
          // Reload the current page if it's now empty and not the first page
          if (this.prompts.length === 0 && this.currentPage > 0) {
            this.goToPage(this.currentPage - 1);
          } else {
            this.loadPrompts(); // Reload current page to update counts
          }
        },
        error: (err) => {
          console.error('Error deleting prompt', err);
        }
      });
    }
  }
  
  goToPage(page: number): void {
    if (page >= 0 && page < this.totalPages) {
      this.currentPage = page;
      this.loadPrompts();
    }
  }
  
  changePageSize(size: number): void {
    this.pageSize = size;
    this.currentPage = 0; // Reset to first page
    this.loadPrompts();
  }
  
  changeSorting(field: string): void {
    if (this.sortField === field) {
      // Toggle direction if same field
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      this.sortField = field;
      this.sortDirection = 'desc'; // Default to descending for new sort field
    }
    this.loadPrompts();
  }
  
  getSortIcon(field: string): string {
    if (this.sortField !== field) {
      return 'fa-sort';
    }
    return this.sortDirection === 'asc' ? 'fa-sort-up' : 'fa-sort-down';
  }

  viewJobs(prompt: Prompt): void {
    if (prompt && prompt.promptUuid) {
      this.router.navigate(['/dashboard/jobs'], { queryParams: { promptUuid: prompt.promptUuid } });
    }    
  }

}