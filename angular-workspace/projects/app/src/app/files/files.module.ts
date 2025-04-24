import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ReactiveFormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';

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
    MatIconModule
  ],
  providers: [FileService]
})
export class FilesModule { }
