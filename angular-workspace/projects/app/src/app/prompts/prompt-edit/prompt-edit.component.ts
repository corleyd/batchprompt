import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule, FormBuilder, FormGroup, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { AuthService } from '@auth0/auth0-angular';
import { PromptService } from '../../services/prompt.service';
import { Prompt } from '../../models/prompt.model';
import { catchError, of, switchMap, take } from 'rxjs';

@Component({
  selector: 'app-prompt-edit',
  standalone: true,
  imports: [CommonModule, FormsModule, ReactiveFormsModule, RouterModule],
  templateUrl: './prompt-edit.component.html',
  styleUrls: ['./prompt-edit.component.scss']
})
export class PromptEditComponent implements OnInit {
  promptForm: FormGroup;
  isEditMode = false;
  loading = false;
  saving = false;
  error: string | null = null;
  promptId: string | null = null;

  constructor(
    private fb: FormBuilder,
    private promptService: PromptService,
    private route: ActivatedRoute,
    private router: Router,
    private auth: AuthService
  ) {
    this.promptForm = this.fb.group({
      name: ['', [Validators.required, Validators.maxLength(255)]],
      description: ['', [Validators.required, Validators.maxLength(255)]],
      promptText: ['', [Validators.required]],
      outputSchema: ['{}', [Validators.required]]
    });
  }

  ngOnInit(): void {
    this.route.paramMap.subscribe(params => {
      const promptId = params.get('id');
      if (promptId && promptId !== 'new') {
        this.promptId = promptId;
        this.isEditMode = true;
        this.loadPrompt(promptId);
      }
    });
  }

  loadPrompt(promptId: string): void {
    this.loading = true;
    this.promptService.getPromptById(promptId)
      .subscribe({
        next: (prompt) => {
          this.promptForm.patchValue({
            name: prompt.name,
            description: prompt.description,
            promptText: prompt.promptText,
            outputSchema: prompt.outputSchema
          });
          this.loading = false;
        },
        error: (error) => {
          console.error('Error loading prompt', error);
          this.error = 'Failed to load prompt details. Please try again.';
          this.loading = false;
        }
      });
  }

  onSubmit(): void {
    if (this.promptForm.invalid) {
      this.markFormGroupTouched(this.promptForm);
      return;
    }

    this.saving = true;
    const promptData: Prompt = this.promptForm.value;
    
    this.auth.user$
      .pipe(
        take(1),
        switchMap(user => {
          if (!user?.sub) {
            throw new Error('User not authenticated');
          }
          
          promptData.userId = user.sub;
          
          if (this.isEditMode && this.promptId) {
            return this.promptService.updatePrompt(this.promptId, promptData);
          } else {
            return this.promptService.createPrompt(promptData);
          }
        }),
        catchError(error => {
          console.error('Error saving prompt', error);
          this.error = 'Failed to save prompt. Please try again.';
          this.saving = false;
          return of(null);
        })
      )
      .subscribe({
        next: (response) => {
          if (response) {
            this.saving = false;
            this.router.navigate(['/prompts']);
          }
        }
      });
  }

  markFormGroupTouched(formGroup: FormGroup) {
    Object.values(formGroup.controls).forEach(control => {
      control.markAsTouched();
      if ((control as any).controls) {
        this.markFormGroupTouched(control as FormGroup);
      }
    });
  }

  cancel(): void {
    this.router.navigate(['/prompts']);
  }

  // Helper method to validate if JSON is valid
  validateJson(json: string): boolean {
    try {
      JSON.parse(json);
      return true;
    } catch (e) {
      return false;
    }
  }

  // Method to format the JSON schema
  formatJsonSchema(): void {
    try {
      const value = this.promptForm.get('outputSchema')?.value;
      if (value && this.validateJson(value)) {
        const formatted = JSON.stringify(JSON.parse(value), null, 2);
        this.promptForm.get('outputSchema')?.setValue(formatted);
      }
    } catch (e) {
      // Just ignore if can't format
    }
  }
}