import { useState, type FormEvent } from "react";
import { Link, useLocation } from "react-router-dom";
import { useSession } from "../../../app/session/SessionContext";
import { AuthShell } from "../../../components/AuthShell";
import { focusFirstInvalidField, requiredFieldError } from "../../../utils/formValidation";
import { AuthProviderButtons } from "../components/AuthProviderButtons";
import styles from "./AuthPages.module.css";

export function SignInPage() {
  const location = useLocation();
  const { csrfToken, csrfError, refreshCsrfToken } = useSession();
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [fieldErrors, setFieldErrors] = useState<{ username?: string; password?: string }>({});
  const loginFailed = new URLSearchParams(location.search).has("error");
  const csrfFailed = new URLSearchParams(location.search).has("csrfError");
  const loggedOut = new URLSearchParams(location.search).has("logout");
  const accountDeleted = new URLSearchParams(location.search).has("deleted");

  function handleSubmit(event: FormEvent<HTMLFormElement>) {
    const form = event.currentTarget;
    const formData = new FormData(form);
    const username = String(formData.get("username") ?? "");
    const password = String(formData.get("password") ?? "");
    const errors = {
      username: requiredFieldError(username, "username or email"),
      password: password.length ? "" : "Please enter your password.",
    };
    setFieldErrors(errors);

    if (Object.values(errors).some(Boolean)) {
      event.preventDefault();
      focusFirstInvalidField(form);
      return;
    }

    if (!csrfToken) {
      event.preventDefault();
      return;
    }

    const usernameInput = form.elements.namedItem("username");
    if (usernameInput instanceof HTMLInputElement) {
      usernameInput.value = username.trim();
    }
    setIsSubmitting(true);
  }

  return (
    <AuthShell title="Welcome back" subtitle="Sign in to continue to your account.">
      {accountDeleted && (
        <p className={styles.success} role="status">
          Your account has been deleted and you have been signed out.
        </p>
      )}
      {loggedOut && (
        <p className={styles.success} role="status">
          You have been signed out.
        </p>
      )}
      {loginFailed && (
        <p className={styles.alert} role="alert">
          Sign-in failed. Check your username or email and password, then try again.
        </p>
      )}
      {csrfFailed && (
        <p className={styles.alert} role="alert">
          The security token was missing or expired. Reload it before signing in again.
        </p>
      )}
      {csrfError && (
        <div className={styles.alert} role="alert">
          <p>{csrfError}</p>
          <button
            className="button button-secondary"
            type="button"
            onClick={() => void refreshCsrfToken()}
          >
            Retry security token
          </button>
        </div>
      )}

      <form
        className={styles.form}
        method="post"
        action="/login"
        noValidate
        onSubmit={handleSubmit}
      >
        {csrfToken && (
          <input
            type="hidden"
            name={csrfToken.parameterName}
            value={csrfToken.token}
          />
        )}
        <label htmlFor="username">Username or email</label>
        <input
          id="username"
          name="username"
          type="text"
          autoComplete="username"
          required
          aria-invalid={Boolean(fieldErrors.username)}
          aria-describedby={fieldErrors.username ? "signin-username-error" : undefined}
          onChange={(event) => {
            const value = event.currentTarget.value;
            setFieldErrors((current) => ({
              ...current,
              username: requiredFieldError(value, "username or email"),
            }));
          }}
        />
        {fieldErrors.username && (
          <span className="field-error" id="signin-username-error">{fieldErrors.username}</span>
        )}

        <label htmlFor="password">Password</label>
        <input
          id="password"
          name="password"
          type="password"
          autoComplete="current-password"
          required
          aria-invalid={Boolean(fieldErrors.password)}
          aria-describedby={fieldErrors.password ? "signin-password-error" : undefined}
          onChange={(event) => {
            const value = event.currentTarget.value;
            setFieldErrors((current) => ({
              ...current,
              password: value.length ? "" : "Please enter your password.",
            }));
          }}
        />
        {fieldErrors.password && (
          <span className="field-error" id="signin-password-error">{fieldErrors.password}</span>
        )}

        <button
          className="button button-primary"
          type="submit"
          disabled={isSubmitting || !csrfToken}
        >
          {isSubmitting ? "Signing in…" : csrfToken ? "Sign in" : "Loading security token…"}
        </button>
      </form>

      <AuthProviderButtons groupLabel="Or sign in with" />

      <p className={styles.footer}>
        Don’t have an account? <Link to="/sign-up">Create an account</Link>
      </p>
    </AuthShell>
  );
}
