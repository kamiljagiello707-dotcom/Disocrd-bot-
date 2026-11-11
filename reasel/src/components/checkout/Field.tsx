import { forwardRef, type InputHTMLAttributes } from 'react';
import { cn } from '@/lib/format';

interface FieldProps extends InputHTMLAttributes<HTMLInputElement> {
  label: string;
  error?: string;
  hint?: string;
}

/** Pole formularza z etykietą i komunikatem błędu powiązanym przez aria-describedby. */
export const Field = forwardRef<HTMLInputElement, FieldProps>(function Field({ label, error, hint, id, className, ...props }, ref) {
  const inputId = id ?? props.name;
  const describedBy = error ? `${inputId}-error` : hint ? `${inputId}-hint` : undefined;
  return (
    <div className={className}>
      <label htmlFor={inputId} className="label">
        {label}
      </label>
      <input ref={ref} id={inputId} className={cn('input')} aria-invalid={!!error} aria-describedby={describedBy} {...props} />
      {error ? (
        <p id={`${inputId}-error`} className="error-text" role="alert">
          {error}
        </p>
      ) : (
        hint && (
          <p id={`${inputId}-hint`} className="mt-1.5 text-xs text-ink-400">
            {hint}
          </p>
        )
      )}
    </div>
  );
});
