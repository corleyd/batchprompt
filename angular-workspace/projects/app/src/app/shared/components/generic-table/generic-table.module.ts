import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { GenericTableComponent } from './generic-table.component';

@NgModule({
  declarations: [
    GenericTableComponent
  ],
  imports: [
    CommonModule,
    FormsModule
  ],
  exports: [
    GenericTableComponent
  ]
})
export class GenericTableModule { }