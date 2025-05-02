import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators, FormArray } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { forkJoin, Observable } from 'rxjs';

import { JobService } from '../../services/job.service';
import { FileService } from '../../files/file.service';
import { PromptService } from '../../services/prompt.service';

@Component({
  selector: 'app-job-submit',
  templateUrl: './job-submit.component.html',
  styleUrls: ['./job-submit.component.scss']
})
export class JobSubmitComponent implements OnInit {
  jobForm: FormGroup;
  files: any[] = [];
  prompts: any[] = [];
  models: string[] = [];
  fileFields: any[] = [];
  loading = false;
  fieldsLoading = false;
  submitting = false;
  submitSuccess = false;
  errorMessage = '';
  includeAllFields = true;
  
  constructor(
    private fb: FormBuilder,
    private route: ActivatedRoute,
    private router: Router,
    private jobService: JobService,
    private fileService: FileService,
    private promptService: PromptService
  ) {
    this.jobForm = this.fb.group({
      fileUuid: ['', Validators.required],
      promptUuid: ['', Validators.required],
      modelName: ['', Validators.required],
      includeAllFields: [true],
      outputFieldUuids: this.fb.array([])
    });

    // React to file selection changes
    this.jobForm.get('fileUuid')?.valueChanges.subscribe(fileUuid => {
      if (fileUuid) {
        this.loadFileFields(fileUuid);
      } else {
        this.fileFields = [];
        this.resetOutputFieldUuids();
      }
    });

    // React to include all fields checkbox changes
    this.jobForm.get('includeAllFields')?.valueChanges.subscribe(includeAll => {
      this.includeAllFields = includeAll;
      if (includeAll) {
        this.resetOutputFieldUuids();
      }
    });
  }

  ngOnInit(): void {
    this.loading = true;
    
    // Load prompts and models right away
    const promptObservable = this.promptService.getAllPrompts();
    const modelsObservable = this.jobService.getSupportedModels();
    
    // First check if we have a fileUuid from route parameters or query parameters
    this.route.paramMap.subscribe(params => {
      const paramFileUuid = params.get('fileUuid');
      
      this.route.queryParamMap.subscribe(queryParams => {
        const queryFileUuid = queryParams.get('fileUuid');
        const fileUuid = paramFileUuid || queryFileUuid;
        
        if (fileUuid) {
          // If a fileUuid is provided, only load that specific file
          forkJoin({
            file: this.fileService.getFileDetails(fileUuid),
            prompts: promptObservable,
            models: modelsObservable
          }).subscribe({
            next: (results) => {
              // Create a single-item array with the retrieved file
              this.files = [results.file];
              this.prompts = results.prompts;
              this.models = results.models;
              this.loading = false;
              
              // Auto-select the file in the form
              this.jobForm.patchValue({ fileUuid });
            },
            error: (error) => {
              console.error('Error loading data:', error);
              this.errorMessage = 'Failed to load file details. Please try again.';
              this.loading = false;
            }
          });
        } else {
          // If no fileUuid is provided, load all user files
          forkJoin({
            files: this.fileService.getUserFiles(),
            prompts: promptObservable,
            models: modelsObservable
          }).subscribe({
            next: (results) => {
              // Extract the files from the paginated response
              this.files = results.files.content || [];
              this.prompts = results.prompts;
              this.models = results.models;
              this.loading = false;
            },
            error: (error) => {
              console.error('Error loading data:', error);
              this.errorMessage = 'Failed to load required data. Please try again.';
              this.loading = false;
            }
          });
        }
      });
    });
  }
  
  // Load fields for the selected file
  loadFileFields(fileUuid: string): void {
    this.fieldsLoading = true;
    this.fileFields = [];
    this.resetOutputFieldUuids();
    
    this.fileService.getFileFields(fileUuid).subscribe({
      next: (fields) => {
        this.fileFields = fields;
        this.fieldsLoading = false;
      },
      error: (error) => {
        console.error('Error loading file fields:', error);
        this.errorMessage = 'Failed to load file fields. Please try again.';
        this.fieldsLoading = false;
      }
    });
  }

  // Get the form array for selected fields
  get outputFieldUuidsArray(): FormArray {
    return this.jobForm.get('outputFieldUuids') as FormArray;
  }

  // Reset the selected fields array
  resetOutputFieldUuids(): void {
    const fieldArray = this.jobForm.get('outputFieldUuids') as FormArray;
    while (fieldArray.length) {
      fieldArray.removeAt(0);
    }
  }

  // Toggle a field selection
  toggleFieldSelection(field: string, event: any): void {
    const fieldArray = this.outputFieldUuidsArray;
    
    if (event.target.checked) {
      fieldArray.push(this.fb.control(field));
    } else {
      const index = fieldArray.controls.findIndex(control => control.value === field);
      if (index >= 0) {
        fieldArray.removeAt(index);
      }
    }
  }

  // Check if a field is selected
  isFieldSelected(field: string): boolean {
    return this.outputFieldUuidsArray.controls.some(control => control.value === field);
  }
  
  onSubmit(): void {
    if (this.jobForm.invalid) {
      return;
    }
    
    this.submitting = true;
    const { fileUuid, promptUuid, modelName, includeAllFields, outputFieldUuids } = this.jobForm.value;
    
    // Only send selected fields if not including all fields
    const fieldsToSubmit = includeAllFields ? undefined : outputFieldUuids;
    
    this.jobService.submitJob(fileUuid, promptUuid, modelName, fieldsToSubmit).subscribe({
      next: (response) => {
        this.submitting = false;
        this.submitSuccess = true;
        setTimeout(() => {
          this.router.navigate(['/dashboard/jobs']);
        }, 2000);
      },
      error: (error) => {
        console.error('Error submitting job:', error);
        this.submitting = false;
        this.errorMessage = 'Failed to submit job. Please try again.';
      }
    });
  }
}