/**
 * Display value for the CAPF # field on the printed/electronic CAPF form.
 * The field label already includes "CAPF #", so strip a leading "CAPF-" / "CAPF " prefix.
 */
export function formatCapfFormNumberDisplay(value: string | null | undefined): string {
  const raw = (value ?? '').toString().trim();
  if (!raw) {
    return '';
  }
  return raw.replace(/^CAPF[-\s#]*/i, '');
}
