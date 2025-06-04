import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';


export interface ModelDto {
  modelId: string;
  displayName: string;
  modelProviderDisplayName: string;
  modelProviderId: string;
  modelProviderModelId: string;
  modelProviderProperties: any;
  modelProviderDisplayOrder: number;
  taskQueueName: string;
  simulateStructuredOutput: boolean;
}

export interface ModelProviderDto {
  modelProviderId: string;
  displayName: string;
  displayOrder: number;
  models: ModelDto[];
};

@Injectable({
  providedIn: 'root'
})
export class ModelService {
  private apiUrl: string;

  constructor(private http: HttpClient) {
    this.apiUrl = `${environment.apiBaseUrl}/api/model-management`;
  }

  /**
   * Get all supported AI models
   * @returns Observable of ModelDto array
   */
  getModels(): Observable<ModelDto[]> {
    return this.http.get<ModelDto[]>(`${this.apiUrl}/models`);
  }

  /**
   * Get all model providers
   * @returns Observable of provider entities
   */
  getProviders(): Observable<ModelProviderDto[]> {
    return this.http.get<ModelProviderDto[]>(`${this.apiUrl}/providers`);
  }

  /**
   * Get enabled model providers
   * @returns Observable of enabled provider entities
   */
  getEnabledProviders(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/providers/enabled`);
  }
}
