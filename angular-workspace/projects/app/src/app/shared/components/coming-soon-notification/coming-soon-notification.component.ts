import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { IconsModule } from '../../../icons/icons.module';

@Component({
  selector: 'app-coming-soon-notification',
  templateUrl: './coming-soon-notification.component.html',
  styleUrls: ['./coming-soon-notification.component.scss'],
  standalone: true,
  imports: [
    CommonModule,
    IconsModule
  ]
})
export class ComingSoonNotificationComponent {
  @Input() title: string = 'More Formats Coming Soon!';
  @Input() icon: string = 'clock';
}
