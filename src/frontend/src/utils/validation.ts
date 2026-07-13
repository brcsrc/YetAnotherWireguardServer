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
 * Special characters allowed by the backend (Constants.ADMIN_USER_PASSWORD_ALLOWED_SPECIAL_CHARS).
 * Keep this in sync with src/backend/main/java/com/brcsrc/yaws/model/Constants.java
 */
const PASSWORD_ALLOWED_SPECIAL_CHARS = "!\"#$%&'()*+,-./:;<=>?@[\\]^_`{|}~";
const PASSWORD_SPECIAL_CHAR_REGEX = /[!"#$%&'()*+,\-./:;<=>?@[\]^_`{|}~]/g;
// Backend rejects any character that isn't uppercase, lowercase, a digit, or an allowed special char.
const PASSWORD_UNMATCHED_CHAR_REGEX = /[^a-zA-Z0-9!"#$%&'()*+,\-./:;<=>?@[\]^_`{|}~]/;

/**
 * Validates a password according to backend rules:
 * - At least 12 characters
 * - At least 2 lowercase letters
 * - At least 2 uppercase letters
 * - At least 1 number
 * - At least 1 special character (from allowed set)
 * - No characters outside the allowed set (letters, digits, allowed special chars)
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
  if (!password.match(PASSWORD_SPECIAL_CHAR_REGEX)) {
    return `Password must contain at least 1 special character (${PASSWORD_ALLOWED_SPECIAL_CHARS})`;
  }
  if (PASSWORD_UNMATCHED_CHAR_REGEX.test(password)) {
    return `Password contains a character that is not allowed (allowed special characters: ${PASSWORD_ALLOWED_SPECIAL_CHARS})`;
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
  if (password.match(PASSWORD_SPECIAL_CHAR_REGEX)) score++;
  // Cap at 4 for UI consistency
  return Math.min(score, 4);
}
