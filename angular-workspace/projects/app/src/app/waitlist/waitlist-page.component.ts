import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { WaitlistSignupComponent } from './waitlist-signup.component';

@Component({
  selector: 'app-waitlist-page',
  standalone: true,
  imports: [CommonModule, RouterModule, WaitlistSignupComponent],
  templateUrl: './waitlist-page.component.html',
  styleUrls: ['./waitlist-page.component.scss']
})
export class WaitlistPageComponent {
}