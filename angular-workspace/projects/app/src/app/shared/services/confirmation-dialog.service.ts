import { Injectable } from '@angular/core';
import { Subject, Observable } from 'rxjs';
import { ConfirmationDialogData } from '../components/confirmation-dialog/confirmation-dialog.component';

export interface DialogState {
  isVisible: boolean;
  data: ConfirmationDialogData;
}

@Injectable({
  providedIn: 'root'
})
export class ConfirmationDialogService {
  private dialogSubject = new Subject<DialogState>();
  private resultSubject = new Subject<boolean>();

  public dialogState$ = this.dialogSubject.asObservable();

  constructor() { }

  /**
   * Show a confirmation dialog
   * @param data Dialog configuration
   * @returns Promise that resolves to true if confirmed, false if cancelled
   */
  confirm(data: Partial<ConfirmationDialogData>): Promise<boolean> {
    const dialogData: ConfirmationDialogData = {
      title: data.title || 'Confirm Action',
      message: data.message || 'Are you sure you want to proceed?',
      confirmText: data.confirmText || 'Confirm',
      cancelText: data.cancelText || 'Cancel',
      isDangerous: data.isDangerous || false
    };

    this.dialogSubject.next({
      isVisible: true,
      data: dialogData
    });

    return new Promise<boolean>((resolve) => {
      const subscription = this.resultSubject.subscribe((result) => {
        subscription.unsubscribe();
        this.hide();
        resolve(result);
      });
    });
  }

  /**
   * Convenience method for delete confirmations
   * @param itemName Name of the item being deleted
   * @returns Promise that resolves to true if confirmed, false if cancelled
   */
  confirmDelete(itemName: string): Promise<boolean> {
    return this.confirm({
      title: 'Confirm Deletion',
      message: `Are you sure you want to delete ${itemName}? This action cannot be undone.`,
      confirmText: 'Delete',
      cancelText: 'Cancel',
      isDangerous: true
    });
  }

  /**
   * Handle confirmation result
   */
  handleConfirm(): void {
    this.resultSubject.next(true);
  }

  /**
   * Handle cancellation result
   */
  handleCancel(): void {
    this.resultSubject.next(false);
  }

  /**
   * Hide the dialog
   */
  private hide(): void {
    this.dialogSubject.next({
      isVisible: false,
      data: {
        title: '',
        message: '',
        confirmText: 'Confirm',
        cancelText: 'Cancel',
        isDangerous: false
      }
    });
  }
}