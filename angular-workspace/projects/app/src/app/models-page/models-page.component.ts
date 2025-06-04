import { Component } from '@angular/core';
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
export class ModelsPageComponent {

  modelProviders$: Observable<ModelProviderDto[]>;

  constructor(
    private modelService: ModelService
  ) {
    // Initialize the modelProviders$ observable to fetch model providers
    this.modelProviders$ = this.modelService.getProviders();
  }
  
}
