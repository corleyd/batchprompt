import { Component, EventEmitter, Output } from '@angular/core';
import { HttpEventType } from '@angular/common/http';
import { FileService } from '../file.service';

@Component({
  selector: 'app-file-upload',
  templateUrl: './file-upload.component.html',
  styleUrls: ['./file-upload.component.scss']
})
export class FileUploadComponent {
  @Output() fileUploaded = new EventEmitter<void>();
  
  selectedFiles?: FileList;
  currentFile?: File;
  progress = 0;
  message = '';
  errorMsg = '';

  constructor(private fileService: FileService) {}

  selectFile(event: any): void {
    this.selectedFiles = event.target.files;
    this.progress = 0;
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
