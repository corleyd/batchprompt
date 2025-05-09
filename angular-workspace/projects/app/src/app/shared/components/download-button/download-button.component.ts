import { CommonModule } from '@angular/common';
import { Component, EventEmitter, Input, Output } from '@angular/core';
import { FileService } from '../../../files/file.service';

@Component({
  selector: 'app-download-button',
  templateUrl: './download-button.component.html',
  styleUrls: ['./download-button.component.scss'],
  standalone: true,
  imports: [CommonModule]
})
export class DownloadButtonComponent {
  @Input() fileUuid?: string;
  @Input() fileName?: string;
  @Input() disabled = false;
  @Input() buttonClass = 'download-button';
  @Input() buttonText = 'Download';
  
  @Output() download = new EventEmitter<void>();
  @Output() downloadStarted = new EventEmitter<void>();
  @Output() downloadError = new EventEmitter<any>();

  constructor(private fileService: FileService) {}

  onClick(): void {
    if (!this.disabled && this.fileUuid) {
      this.download.emit();
      
      this.fileService.downloadFile(this.fileUuid, this.fileName).subscribe({
        next: () => {
          this.downloadStarted.emit();
        },
        error: (err) => {
          console.error('Error initiating file download:', err);
          this.downloadError.emit(err);
        }
      });
    }
  }
}