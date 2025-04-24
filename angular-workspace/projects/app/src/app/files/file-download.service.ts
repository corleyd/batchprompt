import { Injectable } from '@angular/core';
import { FileService } from './file.service';
import { Observable } from 'rxjs';
import { tap } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class FileDownloadService {

  constructor(private fileService: FileService) { }

  /**
   * Downloads a file using its UUID
   * @param fileUuid The UUID of the file to download
   * @param fileName Optional custom file name for the download
   */
  downloadFile(fileUuid: string, fileName?: string): Observable<string> {
    return this.fileService.getDownloadUrl(fileUuid).pipe(
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
}