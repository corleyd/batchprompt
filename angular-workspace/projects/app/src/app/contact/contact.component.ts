import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { RouterModule } from '@angular/router';

@Component({
  selector: 'app-contact',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './contact.component.html',
  styleUrls: ['./contact.component.scss']
})
export class ContactComponent {
  feedbackForm: FormGroup;
  isSubmitting = false;
  isSubmitted = false;

  constructor(private fb: FormBuilder) {
    this.feedbackForm = this.fb.group({
      name: ['', [Validators.required, Validators.maxLength(100)]],
      email: ['', [Validators.required, Validators.email]],
      subject: ['', [Validators.required, Validators.maxLength(200)]],
      message: ['', [Validators.required, Validators.maxLength(2000)]]
    });
  }

  onSubmit() {
    if (this.feedbackForm.valid) {
      this.isSubmitting = true;
      
      // Simulate form submission (replace with actual service call)
      setTimeout(() => {
        this.isSubmitting = false;
        this.isSubmitted = true;
        this.feedbackForm.reset();
      }, 1500);
    } else {
      this.markFormGroupTouched(this.feedbackForm);
    }
  }

  markFormGroupTouched(formGroup: FormGroup) {
    Object.values(formGroup.controls).forEach(control => {
      control.markAsTouched();
    });
  }

  resetForm() {
    this.isSubmitted = false;
    this.feedbackForm.reset();
  }
}
