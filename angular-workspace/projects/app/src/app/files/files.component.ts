import { Component, OnInit } from '@angular/core';
import { FileService } from './file.service';

@Component({
  selector: 'app-files',
  templateUrl: './files.component.html',
  styleUrls: ['./files.component.scss']
})
export class FilesComponent implements OnInit {
  files: any[] = [];
  loading = false;
  
  constructor(private fileService: FileService) { }

  ngOnInit(): void {
    this.loadFiles();
  }

  loadFiles(): void {
    this.loading = true;
    this.fileService.getAllFiles().subscribe({
      next: (data) => {
        this.files = data;
        this.loading = false;
      },
      error: (error) => {
        console.error('Error loading files:', error);
        this.loading = false;
      }
    });
  }

  refreshFiles(): void {
    this.loadFiles();
  }
}
