import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { AuthService } from '@auth0/auth0-angular';
import { Title, Meta } from '@angular/platform-browser';
import { ModelService, ModelDto } from '../services/model.service';
import { HttpClientModule } from '@angular/common/http';

@Component({
  selector: 'app-landing-page',
  standalone: true,
  imports: [CommonModule, RouterModule, HttpClientModule],
  templateUrl: './landing-page.component.html',
  styleUrl: './landing-page.component.scss'
})
export class LandingPageComponent implements OnInit {
  supportedModels: ModelDto[] = [];
  isLoadingModels = false;
  modelLoadError = false;

  constructor(
    public auth: AuthService, 
    private router: Router,
    private titleService: Title,
    private metaService: Meta,
    private modelService: ModelService
  ) {}

  ngOnInit() {
    // Set SEO metadata
    this.titleService.setTitle('BatchPrompt - AI Batch Processing Platform');
    this.metaService.addTags([
      { name: 'description', content: 'Process datasets with AI models using custom prompts. BatchPrompt streamlines your data workflow with powerful batch processing capabilities.' },
      { name: 'keywords', content: 'batch processing, AI, prompts, data processing, LLM, large language models, data workflow, structured output' },
      { property: 'og:title', content: 'BatchPrompt - AI Batch Processing Platform' },
      { property: 'og:description', content: 'Process datasets with AI models using custom prompts. BatchPrompt streamlines your data workflow with powerful batch processing capabilities.' },
      { property: 'og:type', content: 'website' },
      { property: 'og:url', content: 'https://batchprompt.ai' },
      { property: 'og:image', content: 'https://batchprompt.ai/assets/images/batchprompt-banner.jpg' },
      { name: 'twitter:card', content: 'summary_large_image' }
    ]);

    // Check if user is authenticated, but no automatic redirect
    this.auth.isAuthenticated$.subscribe(isAuthenticated => {
      // You can use this for conditional UI rendering if needed
    });
    
    // Fetch supported AI models
    this.loadSupportedModels();
  }
  
  /**
   * Fetches the list of supported AI models from the API
   */
  loadSupportedModels() {
    this.isLoadingModels = true;
    this.modelLoadError = false;
    
    this.modelService.getModels().subscribe({
      next: (models) => {
        this.supportedModels = models;
        this.isLoadingModels = false;
      },
      error: (error) => {
        console.error('Error loading models:', error);
        this.modelLoadError = true;
        this.isLoadingModels = false;
      }
    });
  }

  login() {
    this.auth.loginWithRedirect();
  }

  signup() {
    this.auth.loginWithRedirect({
      authorizationParams: { screen_hint: 'signup' }
    });
  }

  navigateToDashboard() {
    this.router.navigate(['/dashboard/home']);
  }

  /**
   * Gets user's initials from their full name
   * @param name Full name of the user
   * @returns First letter of first name and first letter of last name if available
   */
  getUserInitials(name?: string): string {
    if (!name) return '?';
    
    const names = name.trim().split(' ');
    if (names.length === 1) return names[0].charAt(0).toUpperCase();
    
    return (names[0].charAt(0) + names[names.length - 1].charAt(0)).toUpperCase();
  }
  
  /**
   * Helper method to get object keys for template iteration
   * @param obj The object to get keys from
   * @returns Array of object keys
   */
  getObjectKeys(obj: any): string[] {
    return obj ? Object.keys(obj) : [];
  }

  /**
   * Get unique provider display names from the list of models
   * @returns Array of unique provider display names
   */
  getUniqueProviders(): string[] {
    const providers = this.supportedModels.map(model => model.modelProviderDisplayName);
    return [...new Set(providers)].sort();
  }

  /**
   * Get models filtered by provider display name
   * @param providerName The provider display name to filter by
   * @returns Array of models belonging to the specified provider
   */
  getModelsByProvider(providerName: string): ModelDto[] {
    return this.supportedModels.filter(model => 
      model.modelProviderDisplayName === providerName
    ).sort((a, b) => a.displayName.localeCompare(b.displayName));
  }
}
