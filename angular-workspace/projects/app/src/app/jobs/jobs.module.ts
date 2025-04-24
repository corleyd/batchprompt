import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { JobsRoutingModule } from './jobs-routing.module';
import { JobSubmitComponent } from './job-submit/job-submit.component';
import { JobListComponent } from './job-list/job-list.component';

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
    JobsRoutingModule
  ],
  exports: [
    JobSubmitComponent,
    JobListComponent
  ]
})
export class JobsModule { }