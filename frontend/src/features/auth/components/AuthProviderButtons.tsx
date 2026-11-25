import styles from "./AuthProviderButtons.module.css";

interface AuthProviderButtonsProps {
  groupLabel: string;
}

const providers = [
  {
    id: "google",
    label: "Continue with Google",
    icon: (
      <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false">
        <path
          fill="#4285f4"
          d="M22.56 12.25c0-.78-.07-1.53-.2-2.25H12v4.26h5.92a5.1 5.1 0 0 1-2.21 3.31v2.77h3.57c2.08-1.92 3.28-4.74 3.28-8.09Z"
        />
        <path
          fill="#34a853"
          d="M12 23c2.97 0 5.46-.99 7.28-2.66l-3.57-2.77c-.99.66-2.25 1.06-3.71 1.06-2.86 0-5.29-1.93-6.16-4.53H2.15v2.84A11 11 0 0 0 12 23Z"
        />
        <path
          fill="#fbbc05"
          d="M5.84 14.1a6.6 6.6 0 0 1 0-4.2V7.06H2.15a11 11 0 0 0 0 9.88l3.69-2.84Z"
        />
        <path
          fill="#ea4335"
          d="M12 5.37c1.62 0 3.06.56 4.21 1.64l3.15-3.15C17.45 2.09 14.97 1 12 1a11 11 0 0 0-9.85 6.06l3.69 2.84C6.71 7.3 9.14 5.37 12 5.37Z"
        />
      </svg>
    ),
  },
  {
    id: "github",
    label: "Continue with GitHub",
    icon: (
      <svg viewBox="0 0 24 24" aria-hidden="true" focusable="false">
        <path
          fill="currentColor"
          d="M12 .9a11.1 11.1 0 0 0-3.51 21.63c.56.1.76-.24.76-.54v-2.08c-3.1.67-3.76-1.32-3.76-1.32-.5-1.28-1.24-1.62-1.24-1.62-1.01-.69.08-.68.08-.68 1.12.08 1.71 1.15 1.71 1.15 1 .1.76 2.28 3.1 1.61.1-.7.4-1.17.7-1.44-2.48-.28-5.09-1.24-5.09-5.52 0-1.22.44-2.22 1.15-3-.12-.28-.5-1.43.11-2.98 0 0 .94-.3 3.05 1.15a10.6 10.6 0 0 1 5.55 0c2.12-1.44 3.05-1.15 3.05-1.15.61 1.55.23 2.7.11 2.98.72.78 1.15 1.78 1.15 3 0 4.29-2.62 5.23-5.11 5.5.4.35.76 1.02.76 2.06v3.05c0 .3.2.65.77.54A11.1 11.1 0 0 0 12 .9Z"
        />
      </svg>
    ),
  },
  {
    id: "my-custom-auth",
    label: "Continue with Custom Authorization Server",
    icon: (
      <svg
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
        strokeLinecap="round"
        strokeLinejoin="round"
        strokeWidth="1.8"
        aria-hidden="true"
        focusable="false"
      >
        <path d="M12 2.5 20 5.8v5.1c0 5.15-3.27 8.64-8 10.6-4.73-1.96-8-5.45-8-10.6V5.8L12 2.5Z" />
        <path d="M9 12h6m-3-3v6" />
      </svg>
    ),
  },
];

export function AuthProviderButtons({ groupLabel }: AuthProviderButtonsProps) {
  return (
    <>
      <div className={styles.divider} aria-hidden="true">
        <span>{groupLabel}</span>
      </div>
      <nav className={styles.providers} aria-label={groupLabel}>
        {providers.map((provider) => (
          <a
            className={`button button-secondary ${styles.provider}`}
            href={`/oauth2/authorization/${provider.id}`}
            key={provider.id}
          >
            {provider.icon}
            <span>{provider.label}</span>
          </a>
        ))}
      </nav>
    </>
  );
}
