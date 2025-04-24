import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators } from '@angular/forms';
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
  loading = false;
  submitting = false;
  submitSuccess = false;
  errorMessage = '';
  
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
      modelName: ['', Validators.required]
    });
  }

  ngOnInit(): void {
    this.loading = true;
    
    // Load files, prompts, and models in parallel
    const fileObservable = this.fileService.getUserFiles();
    const promptObservable = this.promptService.getAllPrompts();
    const modelsObservable = this.jobService.getSupportedModels();
    
    forkJoin({
      files: fileObservable,
      prompts: promptObservable,
      models: modelsObservable
    }).subscribe({
      next: (results) => {
        // Extract the files from the paginated response
        this.files = results.files.content || [];
        this.prompts = results.prompts;
        this.models = results.models;
        this.loading = false;
        
        // Check if we have a fileUuid from the route parameters
        this.route.paramMap.subscribe(params => {
          const fileUuid = params.get('fileUuid');
          if (fileUuid) {
            this.jobForm.patchValue({ fileUuid });
          }
        });
      },
      error: (error) => {
        console.error('Error loading data:', error);
        this.errorMessage = 'Failed to load required data. Please try again.';
        this.loading = false;
      }
    });
  }
  
  onSubmit(): void {
    if (this.jobForm.invalid) {
      return;
    }
    
    this.submitting = true;
    const { fileUuid, promptUuid, modelName } = this.jobForm.value;
    
    this.jobService.submitJob(fileUuid, promptUuid, modelName).subscribe({
      next: (response) => {
        this.submitting = false;
        this.submitSuccess = true;
        setTimeout(() => {
          this.router.navigate(['/jobs']);
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