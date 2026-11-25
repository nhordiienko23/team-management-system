import type { ReactNode } from "react";
import { ThemeSwitch } from "./ThemeSwitch";
import styles from "./AuthShell.module.css";

interface AuthShellProps {
  title: string;
  subtitle: string;
  children: ReactNode;
}

export function AuthShell({ title, subtitle, children }: AuthShellProps) {
  return (
    <main className={styles.page}>
      <ThemeSwitch />
      <section className={styles.card} aria-labelledby="auth-title">
        <p className={styles.brand}>TEAM MANAGEMENT</p>
        <h1 id="auth-title">{title}</h1>
        <p className={styles.subtitle}>{subtitle}</p>
        {children}
      </section>
    </main>
  );
}
