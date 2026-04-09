import { AbstractControl } from '@angular/forms';

export function setControlError(
  control: AbstractControl | null,
  errorKey: string,
  errorValue: unknown = true
): void {
  if (!control) {
    return;
  }

  control.setErrors({
    ...(control.errors ?? {}),
    [errorKey]: errorValue
  });
}

export function clearControlError(control: AbstractControl | null, errorKey: string): void {
  if (!control?.hasError(errorKey)) {
    return;
  }

  const { [errorKey]: _, ...remainingErrors } = control.errors ?? {};
  control.setErrors(Object.keys(remainingErrors).length ? remainingErrors : null);
}

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
      setControlError(control, errorKey);
    }
    return;
  }

  clearControlError(control, errorKey);
}
