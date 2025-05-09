import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';

@Component({
  selector: 'app-submit-job-dialog',
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
    <h2 mat-dialog-title>Submit Job on Behalf of User</h2>
    <div mat-dialog-content>
      <p>File: <strong>{{ data.fileName }}</strong></p>
      <p>User: <strong>{{ data.userName || data.userId }}</strong></p>
      
      <div class="info-text">
        <p>You are about to submit a job on behalf of this user.</p>
        <p>This will navigate you to the job submission form where you can select a prompt and model.</p>
      </div>
    </div>
    <div mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Cancel</button>
      <button mat-raised-button color="primary" (click)="onConfirm()">Proceed</button>
    </div>
  `,
  styles: [`
    .info-text {
      margin-top: 16px;
      padding: 12px;
      background-color: #f5f5f5;
      border-radius: 4px;
    }
    .info-text p {
      margin: 0 0 8px 0;
    }
    .info-text p:last-child {
      margin-bottom: 0;
    }
  `]
})
export class SubmitJobDialogComponent {
  constructor(
    public dialogRef: MatDialogRef<SubmitJobDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: {
      fileUuid: string;
      fileName: string;
      userId: string;
      userName?: string;
    }
  ) {}
  
  onConfirm(): void {
    this.dialogRef.close(true);
  }
}