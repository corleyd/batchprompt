import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { JobSubmitComponent } from './job-submit/job-submit.component';

const routes: Routes = [
  { path: 'submit', component: JobSubmitComponent },
  { path: 'submit/:fileUuid', component: JobSubmitComponent }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class JobsRoutingModule { }