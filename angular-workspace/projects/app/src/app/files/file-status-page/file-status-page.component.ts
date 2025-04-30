import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { CommonModule } from '@angular/common';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatTableModule } from '@angular/material/table';
import { FileService } from '../file.service';
import { Observable, Subscription, interval } from 'rxjs';
import { switchMap, takeWhile } from 'rxjs/operators';

@Component({
  selector: 'app-file-status-page',
  templateUrl: './file-status-page.component.html',
  styleUrls: ['./file-status-page.component.scss'],
  standalone: true,
  imports: [
    CommonModule, 
    RouterModule,
    MatProgressBarModule,
    MatButtonModule,
    MatIconModule,
    MatTableModule
  ]
})
export class FileStatusPageComponent implements OnInit, OnDestroy {
  fileId: string | null = null;
  file: any = null;
  loading = true;
  error = false;
  errorMessage = '';
  
  fileFields: string[] = [];
  fieldSamples: any = {};
  displayedColumns: string[] = ['fieldName', 'sampleData'];
  
  statusPolling?: Subscription;
  
  constructor(
    private route: ActivatedRoute,
    private router: Router,
    private fileService: FileService
  ) {}

  ngOnInit(): void {
    this.fileId = this.route.snapshot.paramMap.get('id');
    if (this.fileId) {
      this.loadFileDetails();
    } else {
      this.error = true;
      this.errorMessage = 'No file ID provided';
      this.loading = false;
    }
  }
  
  ngOnDestroy(): void {
    if (this.statusPolling) {
      this.statusPolling.unsubscribe();
    }
  }
  
  loadFileDetails(): void {
    if (!this.fileId) return;
    
    this.fileService.getFileDetails(this.fileId).subscribe({
      next: (fileData) => {
        this.file = fileData;
        this.loading = false;
        
        // If file is ready, load the fields
        if (this.file.status === 'READY') {
          this.loadFileFields();
        } 
        // If file is in VALIDATION or PROCESSING state, start polling
        else if (this.file.status === 'VALIDATION' || this.file.status === 'PROCESSING') {
          this.startPolling();
        }
      },
      error: (err) => {
        console.error('Error loading file details:', err);
        this.error = true;
        this.errorMessage = 'Failed to load file details';
        this.loading = false;
      }
    });
  }
  
  loadFileFields(): void {
    if (!this.fileId) return;
    
    // First, get the fields from the file
    this.fileService.getFileFields(this.fileId).subscribe({
      next: (fieldsList) => {
        console.log('Fields API response:', fieldsList);
        
        // Clear existing fields
        this.fileFields = [];
        
        // Handle different response formats
        if (Array.isArray(fieldsList)) {
          // Direct array response
          if (fieldsList.length > 0) {
            if (typeof fieldsList[0] === 'string') {
              // Array of strings
              this.fileFields = fieldsList;
            } else if (typeof fieldsList[0] === 'object') {
              // Array of objects with fieldName property
              this.fileFields = fieldsList
                .filter(field => field && field.fieldName)
                .map(field => field.fieldName);
            }
          }
        } else if (fieldsList && fieldsList.content && Array.isArray(fieldsList.content)) {
          // Paginated response with content array
          if (fieldsList.content.length > 0) {
            if (typeof fieldsList.content[0] === 'string') {
              this.fileFields = fieldsList.content;
            } else if (typeof fieldsList.content[0] === 'object') {
              this.fileFields = fieldsList.content
                .filter((field: any) => field && field.fieldName)
                .map((field: any) => field.fieldName);
            }
          }
        }
        
        console.log('Extracted field names:', this.fileFields);
        
        // Now that we have fields, get sample records to show example data
        if (this.fileFields.length > 0) {
          this.loadSampleData();
        } else {
          this.fieldSamples = {};
        }
      },
      error: (err) => {
        console.error('Error loading file fields:', err);
        this.fileFields = [];
        this.fieldSamples = {};
      }
    });
  }
  
  loadSampleData(): void {
    if (!this.fileId || this.fileFields.length === 0) return;
    
    this.fileService.getFileRecords(this.fileId, 1).subscribe({
      next: (recordsResponse) => {
        console.log('API Response for sample data:', recordsResponse);
        
        // Initialize empty samples object
        this.fieldSamples = {};
        
        // Check if we got valid records
        if (recordsResponse && recordsResponse && recordsResponse.length > 0) {
          const sampleRecord = recordsResponse[0];
          console.log('Sample record:', sampleRecord);
          
          // Extract sample values for each field - trying multiple access patterns
          this.fileFields.forEach(field => {
            console.log(`Processing field: ${field}`);
            
            // Try all possible locations for the data
            if (sampleRecord.record && sampleRecord.record[field] !== undefined) {
              console.log(`Found in record.${field}: ${sampleRecord.record[field]}`);
              this.fieldSamples[field] = sampleRecord.record[field];
            } 
            else if (sampleRecord[field] !== undefined) {
              console.log(`Found directly in record[${field}]: ${sampleRecord[field]}`);
              this.fieldSamples[field] = sampleRecord[field];
            }
            else if (sampleRecord.record && typeof sampleRecord.record === 'string') {
              // Handle case where record might be a JSON string
              try {
                const parsedRecord = JSON.parse(sampleRecord.record);
                if (parsedRecord && parsedRecord[field] !== undefined) {
                  console.log(`Found in parsed record: ${parsedRecord[field]}`);
                  this.fieldSamples[field] = parsedRecord[field];
                } else {
                  this.fieldSamples[field] = 'N/A';
                }
              } catch (e) {
                console.log('Failed to parse record as JSON');
                this.fieldSamples[field] = 'N/A';
              }
            } 
            else {
              console.log(`Field ${field} not found in record`);
              this.fieldSamples[field] = 'N/A';
            }
          });
        } else {
          // If no records found, set all fields to N/A
          console.log('No records found in response');
          this.fileFields.forEach(field => {
            this.fieldSamples[field] = 'N/A';
          });
        }

        // Debug final result
        console.log('Final fieldSamples:', this.fieldSamples);
      },
      error: (err) => {
        console.error('Error loading sample data:', err);
        // Initialize empty samples if we couldn't get any
        this.fieldSamples = {};
        this.fileFields.forEach(field => this.fieldSamples[field] = 'N/A');
      }
    });
  }
  
  startPolling(): void {
    if (!this.fileId) return;
    
    this.statusPolling = interval(3000)
      .pipe(
        switchMap(() => this.fileService.getFileStatus(this.fileId!)),
        takeWhile((response) => response.status === 'VALIDATION' || response.status === 'PROCESSING', true)
      )
      .subscribe({
        next: (statusData) => {
          this.file.status = statusData.status;
          
          if (statusData.status === 'READY') {
            // File is ready, load the fields
            this.loadFileFields();
            this.statusPolling?.unsubscribe();
          } else if (statusData.status === 'FAILED') {
            // File processing failed
            this.errorMessage = statusData.errorMessage || 'File processing failed';
            this.statusPolling?.unsubscribe();
          }
        },
        error: (err) => {
          console.error('Error polling file status:', err);
          this.statusPolling?.unsubscribe();
        }
      });
  }
  
  getStatusText(): string {
    if (!this.file) return '';
    
    switch (this.file.status) {
      case 'VALIDATION': return 'File is being validated...';
      case 'PROCESSING': return 'File is being processed...';
      case 'READY': return 'File is ready';
      case 'FAILED': return 'File processing failed';
      default: return `Status: ${this.file.status}`;
    }
  }
  
  createJob(): void {
    if (this.file && this.file.fileUuid) {
      this.router.navigate(['/dashboard/jobs/submit', this.file.fileUuid]);
    }
  }
  
  goToFiles(): void {
    this.router.navigate(['files']);
  }
}
