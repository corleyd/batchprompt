import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { WaitlistService, WaitlistEntryDto } from '../../../services/waitlist.service';
import { UiNotificationService } from '../../../services/ui-notification.service';

@Component({
  selector: 'app-waitlist-management',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="waitlist-management">
      <div class="header">
        <h2>Access Request Management</h2>
        <div class="actions">
          <button class="btn btn-primary" (click)="inviteNextUsers()" [disabled]="isLoading">
            Approve Next {{ inviteCount }} Users
          </button>
          <input 
            type="number" 
            [(ngModel)]="inviteCount" 
            min="1" 
            max="50" 
            class="invite-count-input"
            placeholder="Count">
        </div>
      </div>

      <div class="stats-cards">
        <div class="stat-card">
          <h3>{{ getTotalPending() }}</h3>
          <p>Under Review</p>
        </div>
        <div class="stat-card">
          <h3>{{ getTotalInvited() }}</h3>
          <p>Approved</p>
        </div>
        <div class="stat-card">
          <h3>{{ getTotalRegistered() }}</h3>
          <p>Active Users</p>
        </div>
        <div class="stat-card">
          <h3>{{ entries.length }}</h3>
          <p>Total</p>
        </div>
      </div>

      <div class="filters">
        <select [(ngModel)]="selectedStatus" (change)="filterEntries()" class="status-filter">
          <option value="">All Statuses</option>
          <option value="PENDING">Under Review</option>
          <option value="INVITED">Approved</option>
          <option value="REGISTERED">Active</option>
        </select>
        
        <input 
          type="text" 
          [(ngModel)]="searchTerm" 
          (input)="filterEntries()"
          placeholder="Search by email or name..."
          class="search-input">
      </div>

      <div class="loading" *ngIf="isLoading">
        <i class="fas fa-spinner fa-spin"></i>
        <p>Loading waitlist entries...</p>
      </div>

      <div class="entries-table" *ngIf="!isLoading">
        <table>
          <thead>
            <tr>
              <th>Email</th>
              <th>Name</th>
              <th>Company</th>
              <th>Status</th>
              <th>Joined</th>
              <th>Actions</th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let entry of filteredEntries" [class]="'status-' + entry.status.toLowerCase()">
              <td class="email">{{ entry.email }}</td>
              <td>{{ entry.name || '-' }}</td>
              <td>{{ entry.company || '-' }}</td>
              <td>
                <span class="status-badge" [class]="'status-' + entry.status.toLowerCase()">
                  {{ getStatusText(entry.status) }}
                </span>
              </td>
              <td>{{ formatDate(entry.createdAt) }}</td>
              <td class="actions">
                <button 
                  *ngIf="entry.status === 'PENDING'"
                  class="btn btn-sm btn-success"
                  (click)="inviteUser(entry)"
                  [disabled]="isInviting">
                  Approve
                </button>
                <span *ngIf="entry.status === 'INVITED'" class="invited-date">
                  Approved {{ formatDate(entry.invitedAt!) }}
                </span>
                <span *ngIf="entry.status === 'REGISTERED'" class="registered-date">
                  Registered {{ formatDate(entry.registeredAt!) }}
                </span>
              </td>
            </tr>
          </tbody>
        </table>

        <div class="empty-state" *ngIf="filteredEntries.length === 0">
          <p>No access requests found matching your criteria.</p>
        </div>
      </div>

      <div class="use-case-modal" *ngIf="selectedEntry" (click)="closeModal()">
        <div class="modal-content" (click)="$event.stopPropagation()">
          <div class="modal-header">
            <h3>{{ selectedEntry.name || selectedEntry.email }}</h3>
            <button class="close-btn" (click)="closeModal()">&times;</button>
          </div>
          <div class="modal-body">
            <div class="detail-group">
              <label>Email:</label>
              <span>{{ selectedEntry.email }}</span>
            </div>
            <div class="detail-group" *ngIf="selectedEntry.company">
              <label>Company:</label>
              <span>{{ selectedEntry.company }}</span>
            </div>
            <div class="detail-group" *ngIf="selectedEntry.useCase">
              <label>Use Case:</label>
              <p class="use-case-text">{{ selectedEntry.useCase }}</p>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .waitlist-management {
      padding: 2rem;
      max-width: 1400px;
      margin: 0 auto;
    }

    .header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 2rem;
      flex-wrap: wrap;
      gap: 1rem;
    }

    .header h2 {
      margin: 0;
      color: #1a202c;
      font-size: 1.875rem;
      font-weight: 700;
    }

    .actions {
      display: flex;
      gap: 0.5rem;
      align-items: center;
    }

    .invite-count-input {
      width: 80px;
      padding: 0.5rem;
      border: 1px solid #d1d5db;
      border-radius: 4px;
      font-size: 0.875rem;
    }

    .stats-cards {
      display: grid;
      grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
      gap: 1rem;
      margin-bottom: 2rem;
    }

    .stat-card {
      background: white;
      padding: 1.5rem;
      border-radius: 8px;
      box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
      border: 1px solid #e5e7eb;
      text-align: center;
    }

    .stat-card h3 {
      font-size: 2rem;
      font-weight: 700;
      color: #3b82f6;
      margin: 0 0 0.5rem 0;
    }

    .stat-card p {
      margin: 0;
      color: #6b7280;
      font-size: 0.875rem;
      text-transform: uppercase;
      letter-spacing: 0.05em;
    }

    .filters {
      display: flex;
      gap: 1rem;
      margin-bottom: 2rem;
      flex-wrap: wrap;
    }

    .status-filter, .search-input {
      padding: 0.75rem;
      border: 1px solid #d1d5db;
      border-radius: 6px;
      font-size: 1rem;
    }

    .status-filter {
      min-width: 150px;
    }

    .search-input {
      flex: 1;
      min-width: 250px;
    }

    .loading {
      text-align: center;
      padding: 3rem;
      color: #6b7280;
    }

    .loading i {
      font-size: 2rem;
      margin-bottom: 1rem;
    }

    .entries-table {
      background: white;
      border-radius: 8px;
      overflow: hidden;
      box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
      border: 1px solid #e5e7eb;
    }

    table {
      width: 100%;
      border-collapse: collapse;
    }

    th, td {
      padding: 1rem;
      text-align: left;
      border-bottom: 1px solid #e5e7eb;
    }

    th {
      background: #f9fafb;
      font-weight: 600;
      color: #374151;
      font-size: 0.875rem;
      text-transform: uppercase;
      letter-spacing: 0.05em;
    }

    tr:hover {
      background: #f9fafb;
    }

    .email {
      font-family: monospace;
      font-size: 0.875rem;
    }

    .status-badge {
      padding: 0.25rem 0.75rem;
      border-radius: 9999px;
      font-size: 0.75rem;
      font-weight: 600;
      text-transform: uppercase;
    }

    .status-pending {
      background: #fef3c7;
      color: #92400e;
    }

    .status-invited {
      background: #d1fae5;
      color: #065f46;
    }

    .status-registered {
      background: #dbeafe;
      color: #1e40af;
    }

    .btn {
      padding: 0.5rem 1rem;
      border-radius: 4px;
      border: none;
      font-size: 0.875rem;
      font-weight: 500;
      cursor: pointer;
      transition: all 0.2s;
    }

    .btn:disabled {
      opacity: 0.5;
      cursor: not-allowed;
    }

    .btn-primary {
      background: #3b82f6;
      color: white;
    }

    .btn-primary:hover:not(:disabled) {
      background: #2563eb;
    }

    .btn-success {
      background: #059669;
      color: white;
    }

    .btn-success:hover:not(:disabled) {
      background: #047857;
    }

    .btn-sm {
      padding: 0.375rem 0.75rem;
      font-size: 0.75rem;
    }

    .invited-date, .registered-date {
      font-size: 0.75rem;
      color: #6b7280;
    }

    .empty-state {
      text-align: center;
      padding: 3rem;
      color: #6b7280;
    }

    .use-case-modal {
      position: fixed;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: rgba(0, 0, 0, 0.5);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 1000;
    }

    .modal-content {
      background: white;
      border-radius: 8px;
      padding: 0;
      max-width: 500px;
      width: 90%;
      max-height: 80vh;
      overflow-y: auto;
    }

    .modal-header {
      display: flex;
      justify-content: space-between;
      align-items: center;
      padding: 1.5rem;
      border-bottom: 1px solid #e5e7eb;
    }

    .modal-header h3 {
      margin: 0;
      color: #1a202c;
    }

    .close-btn {
      background: none;
      border: none;
      font-size: 1.5rem;
      cursor: pointer;
      color: #6b7280;
      padding: 0;
      width: 30px;
      height: 30px;
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .modal-body {
      padding: 1.5rem;
    }

    .detail-group {
      margin-bottom: 1rem;
    }

    .detail-group label {
      display: block;
      font-weight: 600;
      color: #374151;
      margin-bottom: 0.25rem;
      font-size: 0.875rem;
    }

    .use-case-text {
      background: #f9fafb;
      padding: 1rem;
      border-radius: 6px;
      border: 1px solid #e5e7eb;
      white-space: pre-wrap;
      line-height: 1.5;
    }

    @media (max-width: 768px) {
      .header {
        flex-direction: column;
        align-items: stretch;
      }

      .filters {
        flex-direction: column;
      }

      .entries-table {
        overflow-x: auto;
      }

      table {
        min-width: 800px;
      }
    }
  `]
})
export class WaitlistManagementComponent implements OnInit {
  entries: WaitlistEntryDto[] = [];
  filteredEntries: WaitlistEntryDto[] = [];
  isLoading = false;
  isInviting = false;
  inviteCount = 10;
  selectedStatus = '';
  searchTerm = '';
  selectedEntry: WaitlistEntryDto | null = null;

  constructor(
    private waitlistService: WaitlistService,
    private uiNotificationService: UiNotificationService
  ) {}

  ngOnInit() {
    this.loadEntries();
  }

  loadEntries() {
    this.isLoading = true;
    this.waitlistService.getAllEntries().subscribe({
      next: (entries) => {
        this.entries = entries.sort((a, b) => 
          new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime()
        );
        this.filterEntries();
        this.isLoading = false;
      },
      error: (error) => {
        this.uiNotificationService.showError('Failed to load waitlist entries');
        this.isLoading = false;
        console.error('Error loading entries:', error);
      }
    });
  }

  filterEntries() {
    this.filteredEntries = this.entries.filter(entry => {
      const matchesStatus = !this.selectedStatus || entry.status === this.selectedStatus;
      const matchesSearch = !this.searchTerm || 
        entry.email.toLowerCase().includes(this.searchTerm.toLowerCase()) ||
        (entry.name && entry.name.toLowerCase().includes(this.searchTerm.toLowerCase())) ||
        (entry.company && entry.company.toLowerCase().includes(this.searchTerm.toLowerCase()));
      
      return matchesStatus && matchesSearch;
    });
  }

  inviteUser(entry: WaitlistEntryDto) {
    this.isInviting = true;
    this.waitlistService.inviteUser(entry.id).subscribe({
      next: (updatedEntry) => {
        const index = this.entries.findIndex(e => e.id === entry.id);
        if (index !== -1) {
          this.entries[index] = updatedEntry;
        }
        this.filterEntries();
        this.isInviting = false;
        this.uiNotificationService.showSuccess(`Invited ${entry.email}`);
      },
      error: (error) => {
        this.isInviting = false;
        this.uiNotificationService.showError('Failed to invite user');
        console.error('Error inviting user:', error);
      }
    });
  }

  inviteNextUsers() {
    this.isInviting = true;
    this.waitlistService.inviteNextUsers(this.inviteCount).subscribe({
      next: (invitedEntries) => {
        // Update the entries with invited status
        invitedEntries.forEach(invitedEntry => {
          const index = this.entries.findIndex(e => e.id === invitedEntry.id);
          if (index !== -1) {
            this.entries[index] = invitedEntry;
          }
        });
        this.filterEntries();
        this.isInviting = false;
        this.uiNotificationService.showSuccess(`Invited ${invitedEntries.length} users`);
      },
      error: (error) => {
        this.isInviting = false;
        this.uiNotificationService.showError('Failed to invite users');
        console.error('Error inviting users:', error);
      }
    });
  }

  showDetails(entry: WaitlistEntryDto) {
    this.selectedEntry = entry;
  }

  closeModal() {
    this.selectedEntry = null;
  }

  getTotalPending(): number {
    return this.entries.filter(e => e.status === 'PENDING').length;
  }

  getTotalInvited(): number {
    return this.entries.filter(e => e.status === 'INVITED').length;
  }

  getTotalRegistered(): number {
    return this.entries.filter(e => e.status === 'REGISTERED').length;
  }

  getStatusText(status: string): string {
    switch (status) {
      case 'PENDING': return 'Under Review';
      case 'INVITED': return 'Approved';
      case 'REGISTERED': return 'Active';
      default: return status;
    }
  }

  formatDate(dateString: string): string {
    return new Date(dateString).toLocaleDateString('en-US', {
      year: 'numeric',
      month: 'short',
      day: 'numeric'
    });
  }
}