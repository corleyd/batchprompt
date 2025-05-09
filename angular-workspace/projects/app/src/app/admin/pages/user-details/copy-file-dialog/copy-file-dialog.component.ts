import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';

@Component({
  selector: 'app-copy-file-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule
  ],
  template: `
    <h2 mat-dialog-title>Copy File to Another User</h2>
    <div mat-dialog-content>
      <p>File: <strong>{{ data.fileName }}</strong></p>
      <p>Source User: <strong>{{ data.sourceUserId }}</strong></p>
      
      <mat-form-field appearance="outline" class="full-width">
        <mat-label>Target User ID</mat-label>
        <input matInput [(ngModel)]="targetUserId" placeholder="Enter user ID" required>
        <mat-hint>Enter the Auth0 ID of the target user</mat-hint>
      </mat-form-field>
    </div>
    <div mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Cancel</button>
      <button mat-raised-button color="primary" [disabled]="!targetUserId" 
        (click)="onConfirm()">Copy File</button>
    </div>
  `,
  styles: [`
    .full-width {
      width: 100%;
    }
    mat-form-field {
      margin-top: 16px;
    }
  `]
})
export class CopyFileDialogComponent {
  targetUserId: string = '';
  
  constructor(
    public dialogRef: MatDialogRef<CopyFileDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: {
      fileUuid: string;
      fileName: string;
      sourceUserId: string;
    }
  ) {}
  
  onConfirm(): void {
    if (this.targetUserId) {
      this.dialogRef.close({
        targetUserId: this.targetUserId
      });
    }
  }
}