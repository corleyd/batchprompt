import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { FilesComponent } from './files.component';

const routes: Routes = [
  {
    path: '',
    component: FilesComponent
  },
  {
    path: 'upload',
    loadComponent: () => import('./file-upload-page/file-upload-page.component').then(m => m.FileUploadPageComponent)
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class FilesRoutingModule { }
