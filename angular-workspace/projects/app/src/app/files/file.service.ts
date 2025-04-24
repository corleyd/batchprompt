import { HttpClient, HttpEvent, HttpRequest } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { map, Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class FileService {
  private apiUrl = "http://localhost:8081/api/files"; // Base URL for the file API

  constructor(private http: HttpClient) { }

  // Getter for apiUrl to use in components
  getApiUrl(): string {
    return this.apiUrl;
  }

  // Upload a file
  uploadFile(file: File): Observable<HttpEvent<any>> {
    const formData: FormData = new FormData();
    formData.append('file', file);

    const req = new HttpRequest('POST', `${this.apiUrl}`, formData, {
      reportProgress: true,
      responseType: 'json'
    });

    return this.http.request(req);
  }

  // Get file status by id
  getFileStatus(fileId: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/status/${fileId}`);
  }

  // Get all files
  getAllFiles(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}`);
  }

  // New method to get a download token from the server
  getDownloadUrl(fileUuid: string): Observable<string> {
    return this.http.get(`${this.apiUrl}/${fileUuid}/token`, { responseType: 'text' })
      .pipe(map((token: string) => `${this.apiUrl}/${fileUuid}/download/${token}`));
  }

  // Download a file
  downloadFile(fileId: string): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/${fileId}/content`, {
      responseType: 'blob'
    });
  }
}
