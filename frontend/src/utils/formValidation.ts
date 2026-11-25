export function requiredFieldError(value: string, field: string): string {
  return value.trim() ? "" : `Please enter your ${field}.`;
}

export function emailFieldError(value: string): string {
  const email = value.trim();
  if (!email) {
    return "Please enter your email address.";
  }

  const input = document.createElement("input");
  input.type = "email";
  input.value = email;
  return input.validity.typeMismatch
    ? "Please enter a valid email address, like name@example.com."
    : "";
}

export function focusFirstInvalidField(form: HTMLFormElement): void {
  window.requestAnimationFrame(() => {
    form.querySelector<HTMLElement>('[aria-invalid="true"]')?.focus();
  });
}
