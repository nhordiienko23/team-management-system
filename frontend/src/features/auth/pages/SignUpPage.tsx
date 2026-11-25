import { useState, type FormEvent } from "react";
import { Link } from "react-router-dom";
import { ApiError, postJson } from "../../../api/client";
import { AuthShell } from "../../../components/AuthShell";
import {
  emailFieldError,
  focusFirstInvalidField,
  requiredFieldError,
} from "../../../utils/formValidation";
import { AuthProviderButtons } from "../components/AuthProviderButtons";
import styles from "./AuthPages.module.css";

interface RegistrationResponse {
  id: number;
  username: string;
  email: string;
}

type RegistrationField = "username" | "email" | "password";
type RegistrationErrors = Partial<Record<RegistrationField, string>>;

export function SignUpPage() {
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState("");
  const [registered, setRegistered] = useState(false);
  const [fieldErrors, setFieldErrors] = useState<RegistrationErrors>({});
  const [touched, setTouched] = useState<Partial<Record<RegistrationField, boolean>>>({});

  function validateField(field: RegistrationField, value: string): string {
    switch (field) {
      case "username":
        return requiredFieldError(value, "username");
      case "email":
        return emailFieldError(value);
      case "password":
        return requiredFieldError(value, "password");
    }
  }

  async function handleSubmit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = event.currentTarget;
    const formData = new FormData(form);
    const values = {
      username: String(formData.get("username") ?? ""),
      email: String(formData.get("email") ?? ""),
      password: String(formData.get("password") ?? ""),
    };
    const errors: RegistrationErrors = {
      username: validateField("username", values.username),
      email: validateField("email", values.email),
      password: validateField("password", values.password),
    };
    setFieldErrors(errors);
    setTouched({ username: true, email: true, password: true });

    if (Object.values(errors).some(Boolean)) {
      focusFirstInvalidField(form);
      return;
    }

    setIsSubmitting(true);
    setErrorMessage("");
    const registration = {
      username: values.username.trim(),
      email: values.email.trim(),
      password: values.password,
    };

    try {
      await postJson<RegistrationResponse>("/api/auth/register", registration);
      setRegistered(true);
      form.reset();
      setFieldErrors({});
      setTouched({});
    } catch (error) {
      setErrorMessage(
        error instanceof ApiError
          ? error.message
          : "Registration could not be completed. Please try again.",
      );
    } finally {
      setIsSubmitting(false);
    }
  }

  return (
    <AuthShell
      title="Create your account"
      subtitle="Register with your username, email, and password, or continue with an external provider."
    >
      {registered ? (
        <div className={styles.success} role="status">
          <p>Your account has been created. Sign in to continue.</p>
          <Link className="button button-primary" to="/sign-in">
            Go to sign in
          </Link>
        </div>
      ) : (
        <>
          {errorMessage && <p className={styles.alert} role="alert">{errorMessage}</p>}
          <form className={styles.form} noValidate onSubmit={handleSubmit}>
            <label htmlFor="signup-username">Username</label>
            <input
              id="signup-username"
              name="username"
              type="text"
              autoComplete="username"
              required
              aria-invalid={Boolean(fieldErrors.username)}
              aria-describedby={fieldErrors.username ? "signup-username-error" : undefined}
              onBlur={(event) => {
                setTouched((current) => ({ ...current, username: true }));
                setFieldErrors((current) => ({
                  ...current,
                  username: validateField("username", event.currentTarget.value),
                }));
              }}
              onChange={(event) => {
                if (touched.username) {
                  setFieldErrors((current) => ({
                    ...current,
                    username: validateField("username", event.currentTarget.value),
                  }));
                }
              }}
            />
            {fieldErrors.username && (
              <span className="field-error" id="signup-username-error">
                {fieldErrors.username}
              </span>
            )}

            <label htmlFor="signup-email">Email</label>
            <input
              id="signup-email"
              name="email"
              type="email"
              autoComplete="email"
              required
              aria-invalid={Boolean(fieldErrors.email)}
              aria-describedby={[
                "signup-email-example",
                ...(fieldErrors.email ? ["signup-email-error"] : []),
              ].join(" ")}
              onBlur={(event) => {
                setTouched((current) => ({ ...current, email: true }));
                setFieldErrors((current) => ({
                  ...current,
                  email: validateField("email", event.currentTarget.value),
                }));
              }}
              onChange={(event) => {
                if (touched.email) {
                  setFieldErrors((current) => ({
                    ...current,
                    email: validateField("email", event.currentTarget.value),
                  }));
                }
              }}
            />
            <span className="field-hint" id="signup-email-example">
              Example: name@example.com
            </span>
            {fieldErrors.email && (
              <span className="field-error" id="signup-email-error">
                {fieldErrors.email}
              </span>
            )}

            <label htmlFor="signup-password">Password</label>
            <input
              id="signup-password"
              name="password"
              type="password"
              autoComplete="new-password"
              required
              aria-invalid={Boolean(fieldErrors.password)}
              aria-describedby={fieldErrors.password ? "signup-password-error" : undefined}
              onBlur={(event) => {
                setTouched((current) => ({ ...current, password: true }));
                setFieldErrors((current) => ({
                  ...current,
                  password: validateField("password", event.currentTarget.value),
                }));
              }}
              onChange={(event) => {
                if (touched.password) {
                  setFieldErrors((current) => ({
                    ...current,
                    password: validateField("password", event.currentTarget.value),
                  }));
                }
              }}
            />
            {fieldErrors.password && (
              <span className="field-error" id="signup-password-error">
                {fieldErrors.password}
              </span>
            )}

            <button className="button button-primary" type="submit" disabled={isSubmitting}>
              {isSubmitting ? "Creating account…" : "Create account"}
            </button>
          </form>
        </>
      )}

      <AuthProviderButtons groupLabel="Or continue with" />

      <p className={styles.footer}>
        Already have an account? <Link to="/sign-in">Sign in</Link>
      </p>
    </AuthShell>
  );
}
