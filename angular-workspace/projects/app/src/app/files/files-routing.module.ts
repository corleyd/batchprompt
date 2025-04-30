import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { FilesComponent } from './files.component';
import { FileStatusPageComponent } from './file-status-page/file-status-page.component';

const routes: Routes = [
  {
    path: '',
    component: FilesComponent
  },
  {
    path: 'upload',
    loadComponent: () => import('./file-upload-page/file-upload-page.component').then(m => m.FileUploadPageComponent)
  },
  {
    path: 'status/:id',
    component: FileStatusPageComponent
  }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class FilesRoutingModule { }
