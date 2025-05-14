import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { JobSubmitComponent } from './job-submit/job-submit.component';
import { JobListComponent } from './job-list/job-list.component';
import { JobDetailComponent } from './job-detail/job-detail.component';

const routes: Routes = [
  { path: '', component: JobListComponent },
  { path: 'submit', component: JobSubmitComponent },
  { path: 'submit/:fileUuid', component: JobSubmitComponent },
  { path: ':jobUuid', component: JobDetailComponent }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class JobsRoutingModule { }