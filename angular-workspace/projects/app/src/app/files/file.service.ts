import { HttpClient, HttpEvent, HttpRequest } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { map, Observable, tap } from 'rxjs';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class FileService {
  private apiUrl: string; // Base URL for the file API

  constructor(private http: HttpClient) {
    this.apiUrl = `${environment.apiBaseUrl}/api/files`;
  }

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

  // Get file details by id (including all metadata and fields)
  getFileDetails(fileId: string): Observable<any> {
    return this.http.get(`${this.apiUrl}/${fileId}`);
  }

  // Get file fields by id (column names and sample data)
  getFileFields(fileId: string): Observable<any> {
    return this.http.get<any[]>(`${this.apiUrl}/${fileId}/fields`);
  }

  // Get file records for sample data
  getFileRecords(fileId: string, limit: number = 5): Observable<any> {
    return this.http.get<any[]>(`${this.apiUrl}/${fileId}/records`, {
      params: { size: limit.toString() }
    });
  }

  // Get user files with pagination, sorting and filtering
  getUserFiles(userId?: string, page: number = 0, size: number = 10, sortBy: string = 'createdAt', 
              sortDirection: string = 'desc', fileType?: string, status?: string): Observable<any> {
    let url = `${this.apiUrl}/user`;
    
    if (userId) {
      url += `/${userId}`;
    }
    url += `?page=${page}&size=${size}&sortBy=${sortBy}&sortDirection=${sortDirection}`;
    
    // Add optional filters if provided
    if (fileType) {
      url += `&fileType=${fileType}`;
    }
    
    if (status) {
      url += `&status=${status}`;
    }
    
    // Return the full Page object which contains:
    // - content: the array of items for the current page
    // - totalElements: total number of items
    // - totalPages: total number of pages
    // - number: current page number
    // - size: page size
    // - first: whether this is the first page
    // - last: whether this is the last page
    // ... and other pagination metadata
    return this.http.get<any>(url);
  }

  // New method to get a download token from the server
  getDownloadUrl(fileUuid: string): Observable<string> {
    return this.http.get(`${this.apiUrl}/${fileUuid}/token`, { responseType: 'text' })
      .pipe(map((token: string) => `${this.apiUrl}/${fileUuid}/download/${token}`));
  }

  /**
   * Downloads a file using its UUID
   * @param fileUuid The UUID of the file to download
   * @param fileName Optional custom file name for the download
   */
  downloadFile(fileUuid: string, fileName?: string): Observable<string> {
    return this.getDownloadUrl(fileUuid).pipe(
      tap(downloadUrl => {
        // Create an anchor element to trigger the download
        const a = document.createElement('a');
        a.style.display = 'none';
        a.href = downloadUrl;
        a.download = fileName || 'download';
        a.target = '_self';
        document.body.appendChild(a);
        a.click();

        // Remove the element after download is initiated
        setTimeout(() => {
          document.body.removeChild(a);
        }, 1000);
      })
    );
  }
  
  // Delete a file
  deleteFile(fileId: string): Observable<any> {
    return this.http.delete(`${this.apiUrl}/${fileId}`);
  }
  
  // Admin function to copy a file to another user
  copyFileToUser(sourceFileUuid: string, targetUserId: string): Observable<any> {
    const url = `${this.apiUrl}/admin/copy`;
    const params = { sourceFileUuid, targetUserId };
    return this.http.post(url, null, { params });
  }
}
