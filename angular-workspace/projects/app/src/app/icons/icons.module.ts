import { NgModule } from '@angular/core';

import { FeatherModule } from 'angular-feather';
import { 
  User, 
  LogOut, 
  ChevronDown, 
  ChevronUp,
  ChevronLeft,
  ChevronRight, 
  Layout, 
  Download, 
  Trash2, 
  MoreVertical, 
  Upload, 
  Shield,
  Home,
  FileText,
  Briefcase,
  MessageSquare,
  Settings,
  BarChart2,
  Users,
  // Additional icons for home component
  Code,
  PlayCircle,
  File,
  Play,
  ArrowRight,
  Zap,
  CheckSquare,
  List,
  CheckCircle,
  Clock,
  XCircle
} from 'angular-feather/icons';

// Select some icons (use an object, not an array)
const icons = {
  User,
  LogOut,
  ChevronDown,
  ChevronUp,
  ChevronLeft,
  ChevronRight,
  Layout,
  Download,
  Trash2,
  MoreVertical,
  Upload,
  Shield,
  Home,
  FileText,
  Briefcase,
  MessageSquare,
  Settings,
  BarChart2,
  Users,
  // Additional icons for home component
  Code,
  PlayCircle,
  File,
  Play,
  ArrowRight,
  Zap,
  CheckSquare,
  List,
  CheckCircle,
  Clock,
  XCircle
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
