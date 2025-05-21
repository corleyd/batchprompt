import { Component, Inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogModule } from '@angular/material/dialog';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';

@Component({
  selector: 'app-credit-dialog',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    MatDialogModule,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule
  ],
  template: `
    <h2 mat-dialog-title>Add Credits</h2>
    <div mat-dialog-content>
      <form [formGroup]="form">
        <p>Add credits to account: <strong>{{ data.accountName }}</strong> ({{ data.userName }})</p>
        
        <mat-form-field appearance="fill" class="full-width">
          <mat-label>Amount</mat-label>
          <input matInput type="number" formControlName="amount" placeholder="Enter credit amount" required>
          <mat-error *ngIf="form.get('amount')?.hasError('required')">Amount is required</mat-error>
          <mat-error *ngIf="form.get('amount')?.hasError('min')">Amount must be positive</mat-error>
        </mat-form-field>
        
        <mat-form-field appearance="fill" class="full-width">
          <mat-label>Reason</mat-label>
          <input matInput formControlName="reason" placeholder="Enter reason for adding credits" required>
          <mat-error *ngIf="form.get('reason')?.hasError('required')">Reason is required</mat-error>
        </mat-form-field>
      </form>
    </div>
    <div mat-dialog-actions align="end">
      <button mat-button (click)="onCancel()">Cancel</button>
      <button mat-raised-button color="primary" [disabled]="!form.valid" (click)="onSubmit()">Add Credits</button>
    </div>
  `,
  styles: [`
    .full-width {
      width: 100%;
      margin-bottom: 15px;
    }
  `]
})
export class CreditDialogComponent {
  form: FormGroup;

  constructor(
    private fb: FormBuilder,
    public dialogRef: MatDialogRef<CreditDialogComponent>,
    @Inject(MAT_DIALOG_DATA) public data: any
  ) {
    this.form = this.fb.group({
      amount: [100, [Validators.required] ],
      reason: ['Admin credit adjustment', Validators.required]
    });
  }

  onCancel(): void {
    this.dialogRef.close();
  }

  onSubmit(): void {
    if (this.form.valid) {
      this.dialogRef.close(this.form.value);
    }
  }
}