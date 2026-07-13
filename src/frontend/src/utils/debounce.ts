import { useEffect, useState } from "react";

/**
 * Returns a debounced copy of `value` that only updates after `delayMs`
 * has elapsed without `value` changing. Use this to derive validation/error
 * state from user input so errors don't flash on every keystroke while the
 * user is still typing - validate the debounced value instead of the raw one.
 *
 * @param value the live value to debounce
 * @param delayMs debounce delay in milliseconds (defaults to 500ms)
 * @returns the debounced value
 */
export function useDebouncedValue<T>(value: T, delayMs: number = 500): T {
  const [debouncedValue, setDebouncedValue] = useState(value);

  useEffect(() => {
    const timeoutId = setTimeout(() => {
      setDebouncedValue(value);
    }, delayMs);
    return () => clearTimeout(timeoutId);
  }, [value, delayMs]);

  return debouncedValue;
}
