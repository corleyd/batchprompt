import { Component, EventEmitter, Output } from '@angular/core';
import { HttpEventType } from '@angular/common/http';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { FileService } from '../file.service';

@Component({
  selector: 'app-file-upload',
  templateUrl: './file-upload.component.html',
  styleUrls: ['./file-upload.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    MatIconModule,
    MatButtonModule,
    MatProgressSpinnerModule
  ]
})
export class FileUploadComponent {
  @Output() fileUploaded = new EventEmitter<void>();
  
  selectedFiles?: FileList;
  currentFile?: File;
  progress = 0;
  message = '';
  errorMsg = '';
  isDragover = false;

  constructor(private fileService: FileService) {}

  selectFile(event: any): void {
    this.selectedFiles = event.target.files;
    this.progress = 0;
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragover = true;
  }

  onDragLeave(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragover = false;
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    event.stopPropagation();
    this.isDragover = false;
    
    if (event.dataTransfer?.files) {
      this.selectedFiles = event.dataTransfer.files;
      this.progress = 0;
    }
  }

  upload(): void {
    this.errorMsg = '';
    this.progress = 0;
    
    if (this.selectedFiles) {
      const file: File | null = this.selectedFiles.item(0);
      
      if (file) {
        this.currentFile = file;
        
        this.fileService.uploadFile(this.currentFile).subscribe({
          next: (event: any) => {
            if (event.type === HttpEventType.UploadProgress) {
              this.progress = Math.round(100 * event.loaded / event.total);
            } else if (event.type === HttpEventType.Response) {
              this.message = 'File uploaded successfully!';
              this.fileUploaded.emit();
            }
          },
          error: (err: any) => {
            this.progress = 0;
            this.errorMsg = 'Could not upload the file!';
            this.currentFile = undefined;
          }
        });
      }
      
      this.selectedFiles = undefined;
    }
  }
}
