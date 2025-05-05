import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

import { FilesRoutingModule } from './files-routing.module';
import { FilesComponent } from './files.component';
import { FileStatusComponent } from './file-status/file-status.component';
import { DownloadButtonComponent } from '../shared/components';
import { FileService } from './file.service';
import { IconsModule } from '../icons/icons.module';
import { GenericTableModule } from '../shared/components/generic-table/generic-table.module';

@NgModule({
  declarations: [
    FilesComponent,
    FileStatusComponent
  ],
  imports: [
    CommonModule,
    FilesRoutingModule,
    ReactiveFormsModule,
    FormsModule,
    MatIconModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    DownloadButtonComponent,
    IconsModule,
    GenericTableModule
  ],
  providers: [FileService]
})
export class FilesModule { }
