import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { Router } from '@angular/router';
import { FileUploadComponent } from '../file-upload/file-upload.component';

@Component({
  selector: 'app-file-upload-page',
  templateUrl: './file-upload-page.component.html',
  styleUrls: ['./file-upload-page.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    RouterModule,
    FileUploadComponent,
    MatIconModule,
    MatButtonModule
  ]
})
export class FileUploadPageComponent {
  constructor(private router: Router) {}
  
  fileUploaded(): void {
    // Optionally navigate to the files list after upload
    // this.router.navigate(['/files']);
    // Or just keep user on this page for additional uploads
  }
}