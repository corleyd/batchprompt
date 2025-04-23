import { Injectable } from '@angular/core';
import { HttpClient, HttpEvent, HttpRequest } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({
  providedIn: 'root'
})
export class FileService {
  private apiUrl = 'http://localhost:8081/api/files'; // Adjust base URL as needed

  constructor(private http: HttpClient) { }

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
}
