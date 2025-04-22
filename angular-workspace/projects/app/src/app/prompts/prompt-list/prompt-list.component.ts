import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { Prompt } from '../../models/prompt.model';
import { PromptService } from '../../services/prompt.service';
import { AuthService } from '@auth0/auth0-angular';
import { Observable, switchMap } from 'rxjs';

@Component({
  selector: 'app-prompt-list',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './prompt-list.component.html',
  styleUrls: ['./prompt-list.component.scss']
})
export class PromptListComponent implements OnInit {
  prompts: Prompt[] = [];
  loading = true;
  error = false;

  constructor(
    private promptService: PromptService,
    public auth: AuthService
  ) {}

  ngOnInit(): void {
    this.loadPrompts();
  }

  loadPrompts(): void {
    this.loading = true;
    this.auth.user$
      .pipe(
        switchMap(user => {
          if (user?.sub) {
            return this.promptService.getUserPrompts(user.sub);
          } else {
            return this.promptService.getAllPrompts();
          }
        })
      )
      .subscribe({
        next: (prompts) => {
          this.prompts = prompts;
          this.loading = false;
        },
        error: (err) => {
          console.error('Error loading prompts', err);
          this.error = true;
          this.loading = false;
        }
      });
  }

  deletePrompt(promptUuid: string): void {
    if (confirm('Are you sure you want to delete this prompt?')) {
      this.promptService.deletePrompt(promptUuid).subscribe({
        next: () => {
          this.prompts = this.prompts.filter(p => p.promptUuid !== promptUuid);
        },
        error: (err) => {
          console.error('Error deleting prompt', err);
        }
      });
    }
  }
}