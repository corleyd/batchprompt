import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';

import { JobsRoutingModule } from './jobs-routing.module';
import { JobSubmitComponent } from './job-submit/job-submit.component';

@NgModule({
  declarations: [
    JobSubmitComponent
  ],
  imports: [
    CommonModule,
    RouterModule,
    FormsModule,
    ReactiveFormsModule,
    JobsRoutingModule
  ],
  exports: [
    JobSubmitComponent
  ]
})
export class JobsModule { }