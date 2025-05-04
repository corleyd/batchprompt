import { Component, OnInit } from '@angular/core';
import { UserService } from '../../../services/user.service';

@Component({
  selector: 'app-users',
  templateUrl: './users.component.html',
  styleUrls: ['./users.component.scss']
})
export class UsersComponent implements OnInit {
  users: any[] = [];
  loading = false;
  page = 0;
  size = 10;
  totalUsers = 0;
  searchTerm = '';
  roleFilter = '';
  statusFilter = '';
  sortBy = 'name';
  sortDirection = 'asc';
  Math = Math; // Make Math available to the template

  constructor(private userService: UserService) {}

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers(): void {
    this.loading = true;
    this.userService.getAllUsers(this.page, this.size, this.sortBy, this.sortDirection).subscribe({
      next: (response) => {
        this.users = response.content;
        this.totalUsers = response.totalElements;
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading users:', error);
        this.loading = false;
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
        },
        error: (error) => {
          console.error('Error searching users:', error);
          this.loading = false;
        }
      });
    } else {
      this.loadUsers();
    }
  }

  applyFilters(): void {
    // In a real implementation, these filters would be passed to the backend
    // For now we'll just reload the users
    this.loadUsers();
  }

  onPageChange(page: number): void {
    this.page = page;
    this.loadUsers();
  }

  onSortChange(column: string): void {
    if (this.sortBy === column) {
      // Toggle direction if clicking the same column
      this.sortDirection = this.sortDirection === 'asc' ? 'desc' : 'asc';
    } else {
      // Default to ascending when changing columns
      this.sortBy = column;
      this.sortDirection = 'asc';
    }
    
    // Reset to first page when sorting changes
    this.page = 0;
    this.loadUsers();
  }

  onPageSizeChange(): void {
    this.page = 0; // Reset to first page when changing page size
    this.loadUsers();
  }

  getPageNumbers(): number[] {
    const totalPages = Math.ceil(this.totalUsers / this.size);
    if (totalPages <= 5) {
      // If 5 or fewer pages, show all
      return Array.from({ length: totalPages }, (_, i) => i);
    } else {
      // Show 5 pages centered around current page where possible
      let startPage = Math.max(0, this.page - 2);
      const endPage = Math.min(totalPages - 1, startPage + 4);
      
      // Adjust start page if we're near the end
      if (endPage - startPage < 4) {
        startPage = Math.max(0, endPage - 4);
      }
      
      return Array.from({ length: endPage - startPage + 1 }, (_, i) => startPage + i);
    }
  }

  deleteUser(userUuid: string): void {
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
}