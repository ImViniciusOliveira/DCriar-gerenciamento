import { AbstractControl } from '@angular/forms';

export function toggleControlError(
  control: AbstractControl | null,
  errorKey: string,
  enabled: boolean
): void {
  if (!control) {
    return;
  }

  const currentErrors = control.errors ?? {};

  if (enabled) {
    if (!currentErrors[errorKey]) {
      control.setErrors({
        ...currentErrors,
        [errorKey]: true
      });
    }
    return;
  }

  if (!currentErrors[errorKey]) {
    return;
  }

  const { [errorKey]: _, ...remainingErrors } = currentErrors;
  control.setErrors(Object.keys(remainingErrors).length ? remainingErrors : null);
}
