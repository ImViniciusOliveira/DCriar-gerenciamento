import { MatDialogRef } from '@angular/material/dialog';

export function scrollDialogToElement(
  dialogRef: MatDialogRef<unknown>,
  selector: string,
  delayMs = 100
): void {
  setTimeout(() => {
    const dialogContent = (dialogRef as any)._containerInstance._elementRef.nativeElement.querySelector('mat-dialog-content');
    const element = dialogContent?.querySelector(selector) as HTMLElement | null;

    if (element) {
      element.scrollIntoView({ block: 'nearest', behavior: 'smooth' });
    }
  }, delayMs);
}
