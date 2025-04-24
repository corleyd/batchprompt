import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { FilesRoutingModule } from './files-routing.module';
import { FilesComponent } from './files.component';
import { FileUploadComponent } from './file-upload/file-upload.component';
import { FileStatusComponent } from './file-status/file-status.component';
import { FileService } from './file.service';

@NgModule({
  declarations: [
    FilesComponent,
    FileUploadComponent,
    FileStatusComponent
  ],
  imports: [
    CommonModule,
    FilesRoutingModule,
    ReactiveFormsModule,
    FormsModule,
    MatIconModule,
    MatButtonModule,
    MatProgressSpinnerModule
  ],
  providers: [FileService]
})
export class FilesModule { }
