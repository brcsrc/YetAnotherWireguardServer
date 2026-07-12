const ThemeIcon = ({ isDark }: { isDark: boolean }) => (
  <svg
    viewBox="0 0 16 16"
    fill="none"
    xmlns="http://www.w3.org/2000/svg"
    width="16"
    height="16"
    style={{
      transform: isDark ? "none" : "rotate(180deg)",
      transition: "transform 0.3s ease",
    }}
  >
    <circle cx="8" cy="8" r="7" stroke="currentColor" strokeWidth="1" fill="none" />
    <path d="M8 15C11.866 15 15 11.866 15 8C15 4.13401 11.866 1 8 1V15Z" fill="currentColor" />
  </svg>
);

export default ThemeIcon;
