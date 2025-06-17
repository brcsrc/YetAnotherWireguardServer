/**
 * Validates a username according to backend rules:
 * - 4-32 characters
 * - Alphanumeric, dashes, underscores
 * - No spaces
 * @param username
 * @returns error message or null if valid
 */
export function validateUsername(username: string): string | null {
  if (!/^[a-zA-Z0-9_-]{4,32}$/.test(username)) {
    return "4-32 chars, alphanumeric, dashes or underscores, no spaces";
  }
  return null;
}

/**
 * Validates a password according to backend rules:
 * - At least 12 characters
 * - At least 2 lowercase letters
 * - At least 2 uppercase letters
 * - At least 1 number
 * - At least 1 special character (from allowed set)
 * @param password
 * @returns error message or null if valid
 */
export function validatePassword(password: string): string | null {
  if (password.length < 12) {
    return "Password must be at least 12 characters";
  }
  if ((password.match(/[a-z]/g) || []).length < 2) {
    return "Password must contain at least 2 lowercase letters";
  }
  if ((password.match(/[A-Z]/g) || []).length < 2) {
    return "Password must contain at least 2 uppercase letters";
  }
  if (!password.match(/[0-9]/)) {
    return "Password must contain at least 1 number";
  }
  if (!password.match(/[/*!@#$%^&*()\"{}_\\[\]|\\?/<>,.=]/)) {
    return "Password must contain at least 1 special character";
  }
  return null;
}

/**
 * Checks if password and confirmation match.
 * @param password
 * @param password2
 * @returns error message or null if they match
 */
export function validatePassword2(password: string, password2: string): string | null {
  if (password !== password2) {
    return "Passwords do not match";
  }
  return null;
}

/**
 * Returns a password strength score from 0 (too weak) to 4 (very strong).
 * @param password
 * @returns number 0-4
 */
export function getPasswordStrength(password: string): number {
  let score = 0;
  if (password.length >= 12) score++;
  if ((password.match(/[a-z]/g) || []).length >= 2) score++;
  if ((password.match(/[A-Z]/g) || []).length >= 2) score++;
  if (password.match(/[0-9]/)) score++;
  if (password.match(/[/*!@#$%^&*()\"{}_\\[\]|\\?/<>,.=]/)) score++;
  // Cap at 4 for UI consistency
  return Math.min(score, 4);
}