import { Component, OnInit } from '@angular/core';
import { ModelProviderDto, ModelService } from '../services/model.service';
import { Observable } from 'rxjs';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-models-page',
  standalone: true,
  imports: [ CommonModule],
  templateUrl: './models-page.component.html',
  styleUrls: ['./models-page.component.scss']
})
export class ModelsPageComponent implements OnInit {

  modelProviders: ModelProviderDto[] = [];
  modelCreditUsage: { [key: string]: number } = {};
  isLoadingModels = false;
  isLoadingCreditUsage = false;
  modelLoadError = false;
  creditUsageLoadError = false;

  constructor(
    private modelService: ModelService
  ) {}

  ngOnInit() {
    this.loadModels();
    this.loadCreditUsage();
  }

  private loadModels() {
    this.isLoadingModels = true;
    this.modelLoadError = false;
    
    this.modelService.getProviders().subscribe({
      next: (providers) => {
        this.modelProviders = providers;
        this.isLoadingModels = false;
      },
      error: (error) => {
        console.error('Error loading model providers:', error);
        this.modelLoadError = true;
        this.isLoadingModels = false;
      }
    });
  }

  private loadCreditUsage() {
    this.isLoadingCreditUsage = true;
    this.creditUsageLoadError = false;
    
    this.modelService.getTypicalModelCreditUsage().subscribe({
      next: (creditUsage) => {
        this.modelCreditUsage = creditUsage;
        this.isLoadingCreditUsage = false;
      },
      error: (error) => {
        console.error('Error loading credit usage:', error);
        this.creditUsageLoadError = true;
        this.isLoadingCreditUsage = false;
      }
    });
  }

  retryLoadModels() {
    this.loadModels();
  }

  retryLoadCreditUsage() {
    this.loadCreditUsage();
  }
  
}
