import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { JobsRoutingModule } from './jobs-routing.module';
import { JobSubmitComponent } from './job-submit/job-submit.component';
import { JobListComponent } from './job-list/job-list.component';
import { DownloadButtonComponent } from '../shared/components';
import { GenericTableModule } from '../shared/components/generic-table/generic-table.module';

@NgModule({
  declarations: [
    JobSubmitComponent,
    JobListComponent
  ],
  imports: [
    CommonModule,
    RouterModule,
    FormsModule,
    ReactiveFormsModule,
    JobsRoutingModule,
    DownloadButtonComponent,
    GenericTableModule
  ],
  exports: [
    JobSubmitComponent,
    JobListComponent
  ]
})
export class JobsModule { }