import { NgModule } from '@angular/core';

import { FeatherModule } from 'angular-feather';
import { User, LogOut, ChevronDown, ChevronUp, Layout, Download, Trash2, MoreVertical, Upload, Shield } from 'angular-feather/icons';

// Select some icons (use an object, not an array)
const icons = {
  User,
  LogOut,
  ChevronDown,
  ChevronUp,
  Layout,
  Download,
  Trash2,
  MoreVertical,
  Upload,
  Shield
};

@NgModule({
  imports: [
    FeatherModule.pick(icons)
  ],
  exports: [
    FeatherModule
  ]
})
export class IconsModule { }
