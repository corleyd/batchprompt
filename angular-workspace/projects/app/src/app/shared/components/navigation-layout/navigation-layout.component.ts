import { Component, Input, HostListener, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AuthService } from '@auth0/auth0-angular';
import { IconsModule } from '../../../icons/icons.module';

export interface NavigationLink {
  path: string;
  label: string;
  exactMatch?: boolean;
  icon?: string;
  cssClass?: string;
}

@Component({
  selector: 'app-navigation-layout',
  standalone: true,
  imports: [CommonModule, RouterModule, IconsModule],
  templateUrl: './navigation-layout.component.html',
  styleUrls: ['./navigation-layout.component.scss']
})
export class NavigationLayoutComponent {
  @Input() links: NavigationLink[] = [];
  @Input() title: string = '';
  @Input() showTitle: boolean = false;
  @Input() containerClass: string = '';
  @Input() collapsible: boolean = true;
  @Output() sidebarToggled = new EventEmitter<boolean>();
  
  isMobileView = false;
  isSidebarCollapsed = false;
  
  constructor(public auth: AuthService) {
    this.checkScreenSize();
  }
  
  @HostListener('window:resize', ['$event'])
  onResize() {
    this.checkScreenSize();
  }
  
  private checkScreenSize() {
    this.isMobileView = window.innerWidth < 768;
    // Only auto-collapse on mobile
    if (this.isMobileView) {
      this.isSidebarCollapsed = true;
    }
  }
  
  toggleSidebar() {
    this.isSidebarCollapsed = !this.isSidebarCollapsed;
    this.sidebarToggled.emit(this.isSidebarCollapsed);
  }
}