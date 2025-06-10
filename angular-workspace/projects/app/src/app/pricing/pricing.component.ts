import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '@auth0/auth0-angular';

@Component({
  selector: 'app-pricing',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './pricing.component.html',
  styleUrls: ['./pricing.component.scss']
})
export class PricingComponent {

  constructor(
    public auth: AuthService,
    private router: Router
  ) {}

  navigateToDashboard() {
    this.router.navigate(['/dashboard/home']);
  }

  joinWaitlist() {
    this.router.navigate(['/request-access']);
  }
}
