import { useEffect, useState, type FormEvent, type ReactNode } from "react";
import { Link, useLocation, useNavigate, useParams } from "react-router-dom";
import {
  createAdminUser,
  deleteAdminUser,
  getAdminUser,
  searchAdminUsers,
  setAdminUserPassword,
  updateAdminUser,
  type AdminUserCreateRequest,
  type AdminUserDetailDto,
  type AdminUserSearchDto,
  type AdminUserShortDto,
  type UserRole,
} from "../../../api/adminUsers";
import { ApiError } from "../../../api/client";
import type { PageResponse } from "../../../api/sports";
import { useSession } from "../../../app/session/SessionContext";
import {
  emailFieldError,
  focusFirstInvalidField,
  requiredFieldError,
} from "../../../utils/formValidation";
import { AdminDialog } from "../../core/pages/AdminSports";
import { DateFilter, toBackendDate } from "../components/DateFilter";
import "../../core/pages/CorePages.css";

const userRoles: UserRole[] = ["ROLE_USER", "ROLE_ADMIN"];
const pageSizes = [10, 20, 50] as const;
const dateFilterKeys = ["registeredAtStart", "lastLoginStart"] as const;
const filterKeys = [
  "username",
  "email",
  "roles",
  ...dateFilterKeys,
] as const;
const removedDateFilterKeys = ["registeredAtEnd", "lastLoginEnd"] as const;
const sortableProperties = [
  "id",
  "username",
  "email",
  "registeredAt",
  "lastLogin",
] as const;

function describeError(error: unknown): string {
  if (!(error instanceof ApiError)) {
    return error instanceof Error ? error.message : "The operation could not be completed.";
  }
  if (error.status === 400) return `Request rejected: ${error.message}`;
  if (error.status === 401) return `Your session has expired: ${error.message}`;
  if (error.status === 403) return `Access denied: ${error.message}`;
  if (error.status === 404) return `User not found: ${error.message}`;
  if (error.status === 409) return `Conflict: ${error.message}`;
  return error.message;
}

function currentSort(search: string): { property: string; direction: "asc" | "desc" } | null {
  const value = new URLSearchParams(search).get("sort");
  if (!value) return null;
  const [property, direction, ...extra] = value.split(",");
  if (
    extra.length ||
    !sortableProperties.includes(property as (typeof sortableProperties)[number]) ||
    (direction !== "asc" && direction !== "desc")
  ) {
    return null;
  }
  return { property, direction };
}

function requestParams(search: string): URLSearchParams {
  const current = new URLSearchParams(search);
  const params = new URLSearchParams();
  const pageValue = current.get("page") ?? "0";
  const page = /^\d+$/.test(pageValue) ? Number(pageValue) : 0;
  const sizeValue = current.get("size") ?? "10";
  const size = pageSizes.includes(Number(sizeValue) as (typeof pageSizes)[number])
    ? Number(sizeValue)
    : 10;

  params.set("page", String(Number.isSafeInteger(page) ? page : 0));
  params.set("size", String(size));

  for (const key of ["username", "email"] as const) {
    const value = current.get(key)?.trim();
    if (value) params.set(key, value);
  }

  for (const role of current.getAll("roles")) {
    if (userRoles.includes(role as UserRole)) params.append("roles", role);
  }

  for (const key of dateFilterKeys) {
    const value = current.get(key);
    const formatted = value ? toBackendDate(value) : null;
    if (formatted) params.set(key, formatted);
  }

  const sort = currentSort(search);
  if (sort) params.set("sort", `${sort.property},${sort.direction}`);
  return params;
}

function appliedUserFilters(params: URLSearchParams): {
  name: string;
  value: string;
  label: string;
  multiple: boolean;
}[] {
  const filters: { name: string; value: string; label: string; multiple: boolean }[] = [];
  for (const [name, label] of [["username", "Username"], ["email", "Email"]] as const) {
    const value = params.get(name)?.trim();
    if (value) filters.push({ name, value, label: `${label}: ${value}`, multiple: false });
  }
  for (const role of [...new Set(params.getAll("roles"))]) {
    if (userRoles.includes(role as UserRole)) {
      filters.push({ name: "roles", value: role, label: `Role: ${roleLabel(role)}`, multiple: true });
    }
  }
  const dateLabels = {
    registeredAtStart: "Registered from",
    lastLoginStart: "Last login from",
  } as const;
  for (const key of dateFilterKeys) {
    const value = params.get(key);
    if (!value || !toBackendDate(value)) continue;
    const [year, month, day] = value.split("-");
    filters.push({
      name: key,
      value,
      label: `${dateLabels[key]}: ${day}.${month}.${year}`,
      multiple: false,
    });
  }
  return filters;
}

function roleLabel(role: string): string {
  if (role === "ROLE_ADMIN") return "Administrator";
  if (role === "ROLE_USER") return "Regular user";
  return role;
}

function formatDateTime(value: string | null | undefined, emptyValue = "—"): string {
  if (!value) return emptyValue;

  const localDateTime = /^(\d{4})-(\d{2})-(\d{2})T(\d{2}):(\d{2})(?::(\d{2})(?:\.(\d+))?)?$/.exec(value);
  const date = localDateTime
    ? new Date(
        Number(localDateTime[1]),
        Number(localDateTime[2]) - 1,
        Number(localDateTime[3]),
        Number(localDateTime[4]),
        Number(localDateTime[5]),
        Number(localDateTime[6] ?? 0),
        Number((localDateTime[7] ?? "").padEnd(3, "0").slice(0, 3) || 0),
      )
    : new Date(value);
  return Number.isNaN(date.getTime()) ? value : date.toLocaleString();
}

function Feedback({ error, success }: { error?: string; success?: string }) {
  return (
    <>
      {error && <p className="inline-message message-error" role="alert">{error}</p>}
      {success && <p className="inline-message message-success" role="status">{success}</p>}
    </>
  );
}

function CreateUserDialog({
  onClose,
  onCreated,
}: {
  onClose: () => void;
  onCreated: () => void;
}) {
  const [roles, setRoles] = useState<UserRole[]>(["ROLE_USER"]);
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [fieldErrors, setFieldErrors] = useState<{
    username?: string;
    email?: string;
    password?: string;
    confirmPassword?: string;
    roles?: string;
  }>({});
  const [touched, setTouched] = useState<{
    username?: boolean;
    email?: boolean;
    password?: boolean;
    confirmPassword?: boolean;
  }>({});

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (busy) return;
    const form = event.currentTarget;
    const formData = new FormData(form);
    const username = String(formData.get("username") ?? "");
    const email = String(formData.get("email") ?? "");
    const errors = {
      username: requiredFieldError(username, "username"),
      email: emailFieldError(email),
      password: requiredFieldError(password, "password"),
      confirmPassword: requiredFieldError(confirmPassword, "password confirmation")
        || (password === confirmPassword ? "" : "Please make sure both passwords match."),
      roles: roles.length ? "" : "Please select at least one role.",
    };
    setFieldErrors(errors);
    setTouched({
      username: true,
      email: true,
      password: true,
      confirmPassword: true,
    });
    if (Object.values(errors).some(Boolean)) {
      focusFirstInvalidField(form);
      return;
    }

    const request: AdminUserCreateRequest = {
      username: username.trim(),
      email: email.trim(),
      password,
      roles,
    };
    setBusy(true);
    setError("");
    try {
      await createAdminUser(request);
      onCreated();
    } catch (requestError) {
      setError(describeError(requestError));
    } finally {
      setBusy(false);
    }
  }

  function validateCreateField(
    field: "username" | "email" | "password" | "confirmPassword",
    value: string,
  ): string {
    if (field === "username") return requiredFieldError(value, "username");
    if (field === "email") return emailFieldError(value);
    if (field === "password") return requiredFieldError(value, "password");
    return requiredFieldError(value, "password confirmation")
      || (value === password ? "" : "Please make sure both passwords match.");
  }

  function toggleRole(role: UserRole, checked: boolean) {
    const nextRoles = checked
      ? [...new Set([...roles, role])]
      : roles.filter((value) => value !== role);
    setRoles(nextRoles);
    if (fieldErrors.roles) {
      setFieldErrors((current) => ({
        ...current,
        roles: nextRoles.length ? "" : "Please select at least one role.",
      }));
    }
  }

  return (
    <AdminDialog
      title="Create user"
      onClose={() => {
        if (!busy) onClose();
      }}
    >
      <form className="admin-form" noValidate onSubmit={(event) => void submit(event)}>
        <label className="admin-form-field">
          <span>Username</span>
          <input
            id="create-user-username"
            name="username"
            type="text"
            autoComplete="username"
            required
            aria-invalid={Boolean(fieldErrors.username)}
            aria-describedby={fieldErrors.username ? "create-user-username-error" : undefined}
            onBlur={(event) => {
              setTouched((current) => ({ ...current, username: true }));
              setFieldErrors((current) => ({
                ...current,
                username: validateCreateField("username", event.currentTarget.value),
              }));
            }}
            onChange={(event) => {
              if (touched.username) {
                setFieldErrors((current) => ({
                  ...current,
                  username: validateCreateField("username", event.currentTarget.value),
                }));
              }
            }}
          />
          {fieldErrors.username && (
            <span className="field-error" id="create-user-username-error">
              {fieldErrors.username}
            </span>
          )}
        </label>
        <label className="admin-form-field">
          <span>Email</span>
          <input
            id="create-user-email"
            name="email"
            type="email"
            autoComplete="email"
            required
            aria-invalid={Boolean(fieldErrors.email)}
            aria-describedby={[
              "create-user-email-example",
              ...(fieldErrors.email ? ["create-user-email-error"] : []),
            ].join(" ")}
            onBlur={(event) => {
              setTouched((current) => ({ ...current, email: true }));
              setFieldErrors((current) => ({
                ...current,
                email: validateCreateField("email", event.currentTarget.value),
              }));
            }}
            onChange={(event) => {
              if (touched.email) {
                setFieldErrors((current) => ({
                  ...current,
                  email: validateCreateField("email", event.currentTarget.value),
                }));
              }
            }}
          />
          <span className="field-hint" id="create-user-email-example">
            Example: name@example.com
          </span>
          {fieldErrors.email && (
            <span className="field-error" id="create-user-email-error">
              {fieldErrors.email}
            </span>
          )}
        </label>
        <label className="admin-form-field">
          <span>Password</span>
          <input
            id="create-user-password"
            name="password"
            type="password"
            autoComplete="new-password"
            value={password}
            onChange={(event) => {
              const value = event.target.value;
              setPassword(value);
              if (touched.password) {
                setFieldErrors((current) => ({
                  ...current,
                  password: validateCreateField("password", value),
                }));
              }
              if (touched.confirmPassword) {
                setFieldErrors((current) => ({
                  ...current,
                  confirmPassword: requiredFieldError(confirmPassword, "password confirmation")
                    || (value === confirmPassword
                      ? ""
                      : "Please make sure both passwords match."),
                }));
              }
            }}
            required
            aria-invalid={Boolean(fieldErrors.password)}
            aria-describedby={fieldErrors.password ? "create-user-password-error" : undefined}
            onBlur={(event) => {
              setTouched((current) => ({ ...current, password: true }));
              setFieldErrors((current) => ({
                ...current,
                password: validateCreateField("password", event.currentTarget.value),
              }));
            }}
          />
          {fieldErrors.password && (
            <span className="field-error" id="create-user-password-error">
              {fieldErrors.password}
            </span>
          )}
        </label>
        <label className="admin-form-field">
          <span>Confirm password</span>
          <input
            id="create-user-confirm-password"
            name="confirmPassword"
            type="password"
            autoComplete="new-password"
            value={confirmPassword}
            onChange={(event) => {
              const value = event.target.value;
              setConfirmPassword(value);
              if (touched.confirmPassword) {
                setFieldErrors((current) => ({
                  ...current,
                  confirmPassword: validateCreateField("confirmPassword", value),
                }));
              }
            }}
            required
            aria-invalid={Boolean(fieldErrors.confirmPassword)}
            aria-describedby={fieldErrors.confirmPassword ? "create-user-confirm-password-error" : undefined}
            onBlur={(event) => {
              setTouched((current) => ({ ...current, confirmPassword: true }));
              setFieldErrors((current) => ({
                ...current,
                confirmPassword: validateCreateField("confirmPassword", event.currentTarget.value),
              }));
            }}
          />
          {fieldErrors.confirmPassword && (
            <span className="field-error" id="create-user-confirm-password-error">
              {fieldErrors.confirmPassword}
            </span>
          )}
        </label>
        <fieldset
          className="admin-role-options"
          aria-invalid={Boolean(fieldErrors.roles)}
          aria-describedby={fieldErrors.roles ? "create-user-roles-error" : undefined}
          aria-required="true"
          tabIndex={-1}
        >
          <legend>Roles</legend>
          {userRoles.map((role) => (
            <label key={role}>
              <input
                type="checkbox"
                checked={roles.includes(role)}
                onChange={(event) => toggleRole(role, event.target.checked)}
              />
              {roleLabel(role)}
            </label>
          ))}
          {fieldErrors.roles && (
            <span className="field-error" id="create-user-roles-error">
              {fieldErrors.roles}
            </span>
          )}
        </fieldset>
        <Feedback error={error} />
        <div className="admin-form-actions">
          <button className="button button-secondary" type="button" onClick={onClose} disabled={busy}>
            Cancel
          </button>
          <button className="button button-primary" type="submit" disabled={busy}>
            {busy ? "Creating…" : "Create user"}
          </button>
        </div>
      </form>
    </AdminDialog>
  );
}

export function AdminUsersPage() {
  const location = useLocation();
  const navigate = useNavigate();
  const [result, setResult] = useState<PageResponse<AdminUserSearchDto> | null>(null);
  const [error, setError] = useState<unknown>(null);
  const [loading, setLoading] = useState(true);
  const [attempt, setAttempt] = useState(0);
  const [creating, setCreating] = useState(false);
  const [success, setSuccess] = useState("");
  const [filterReset, setFilterReset] = useState(0);
  const params = new URLSearchParams(location.search);
  const query = requestParams(location.search);
  const sort = currentSort(location.search);
  const pageValue = query.get("page") ?? "0";
  const currentPage = Number(pageValue);
  const applied = appliedUserFilters(params);

  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setError(null);
    searchAdminUsers(query, controller.signal).then(
      (page) => {
        setResult(page);
        setLoading(false);
      },
      (requestError: unknown) => {
        if (!controller.signal.aborted) {
          setError(requestError);
          setLoading(false);
        }
      },
    );
    return () => controller.abort();
  }, [location.search, attempt, query.toString()]);

  const notice = (location.state as { notice?: unknown } | null)?.notice;

  function navigateWith(next: URLSearchParams) {
    navigate({ pathname: location.pathname, search: next.toString() ? `?${next}` : "" });
  }

  function applyFilters(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = new FormData(event.currentTarget);
    const next = new URLSearchParams(location.search);
    for (const key of [...filterKeys, ...removedDateFilterKeys]) next.delete(key);
    for (const key of ["username", "email"] as const) {
      const value = String(form.get(key) ?? "").trim();
      if (value) next.set(key, value);
    }
    for (const role of form.getAll("roles").map(String)) {
      if (userRoles.includes(role as UserRole)) next.append("roles", role);
    }
    for (const key of dateFilterKeys) {
      const value = String(form.get(key) ?? "");
      if (toBackendDate(value)) next.set(key, value);
    }
    next.delete("page");
    navigateWith(next);
  }

  function removeFilter(name: string, value: string, multiple: boolean) {
    const next = new URLSearchParams(location.search);
    const remaining = multiple
      ? next.getAll(name).filter((current) => current !== value)
      : [];
    next.delete(name);
    for (const current of remaining) next.append(name, current);
    next.delete("page");
    navigateWith(next);
  }

  function changePage(page: number) {
    const next = new URLSearchParams(location.search);
    next.set("page", String(page));
    navigateWith(next);
  }

  function changeSize(value: string) {
    const next = new URLSearchParams(location.search);
    next.set("size", value);
    next.delete("page");
    navigateWith(next);
  }

  function changeSort(property: string, direction: "asc" | "desc") {
    const next = new URLSearchParams(location.search);
    if (property && sortableProperties.includes(property as (typeof sortableProperties)[number])) {
      next.set("sort", `${property},${direction}`);
    } else {
      next.delete("sort");
    }
    next.delete("page");
    navigateWith(next);
  }

  function clearFilters() {
    const next = new URLSearchParams(location.search);
    for (const key of [...filterKeys, ...removedDateFilterKeys]) next.delete(key);
    next.delete("page");
    setFilterReset((value) => value + 1);
    navigateWith(next);
  }

  const outOfRange =
    !!result &&
    currentPage > 0 &&
    (result.totalPages === 0 || currentPage >= result.totalPages);

  return (
    <div className="page-container">
      <div className="admin-list-toolbar">
        <div className="page-heading">
          <p className="eyebrow">Administration</p>
          <h1>User Management</h1>
          <p className="page-description">
            Browse and search accounts. Search dates use DD.MM.YYYY on the server.
          </p>
        </div>
        <button className="button button-primary" type="button" onClick={() => setCreating(true)}>
          Create user
        </button>
      </div>

      <Feedback success={success || (typeof notice === "string" ? notice : "")} />

      <section className="filter-panel" aria-label="User search and filters">
        <form className="filter-form" key={`${location.search}:${filterReset}`} onSubmit={applyFilters}>
          <div className="filter-groups">
            <fieldset className="filter-group">
              <legend>Account identity</legend>
              <div className="filter-grid">
                <label className="filter-field">
                  <span>Username</span>
                  <input name="username" type="search" defaultValue={params.get("username") ?? ""} />
                </label>
                <label className="filter-field">
                  <span>Email</span>
                  <input name="email" type="search" defaultValue={params.get("email") ?? ""} />
                </label>
              </div>
            </fieldset>
            <fieldset className="filter-group">
              <legend>Roles</legend>
              <div className="filter-grid">
                <label className="filter-field">
                  <span>Roles <small>(matches any selected role)</small></span>
                  <select name="roles" multiple size={2} defaultValue={params.getAll("roles")}>
                    {userRoles.map((role) => (
                      <option key={role} value={role}>{roleLabel(role)}</option>
                    ))}
                  </select>
                </label>
              </div>
            </fieldset>
            <fieldset className="filter-group">
              <legend>Registration date</legend>
              <div className="filter-grid">
                <DateFilter name="registeredAtStart" label="Registered from" defaultValue={params.get("registeredAtStart") ?? ""} />
              </div>
            </fieldset>
            <fieldset className="filter-group">
              <legend>Last login date</legend>
              <div className="filter-grid">
                <DateFilter name="lastLoginStart" label="Last login from" defaultValue={params.get("lastLoginStart") ?? ""} />
              </div>
            </fieldset>
          </div>
          <div className="filter-actions">
            <div className="sort-controls">
              <label>
                <span>Sort by</span>
                <select
                  aria-label="Sort by"
                  value={sort?.property ?? ""}
                  onChange={(event) => changeSort(event.target.value, sort?.direction ?? "asc")}
                >
                  <option value="">Default order</option>
                  <option value="id">User ID</option>
                  <option value="username">Username</option>
                  <option value="email">Email</option>
                  <option value="registeredAt">Registration date</option>
                  <option value="lastLogin">Last login</option>
                </select>
              </label>
              <label>
                <span>Direction</span>
                <select
                  aria-label="Sort direction"
                  value={sort?.direction ?? "asc"}
                  disabled={!sort}
                  onChange={(event) =>
                    changeSort(sort?.property ?? "", event.target.value === "desc" ? "desc" : "asc")
                  }
                >
                  <option value="asc">Ascending</option>
                  <option value="desc">Descending</option>
                </select>
              </label>
            </div>
            <div className="filter-form-actions">
              <button className="button button-primary" type="submit">Apply filters</button>
              <button className="button button-secondary" type="button" onClick={clearFilters}>
                Clear filters
              </button>
            </div>
          </div>
        </form>
        {applied.length > 0 && (
          <div className="applied-filters" role="group" aria-label="Applied filters">
            <span className="applied-filters-heading">Applied filters</span>
            <ul>
              {applied.map((filter) => (
                <li key={`${filter.name}:${filter.value}`}>
                  <span>{filter.label}</span>
                  <button
                    type="button"
                    aria-label={`Remove ${filter.label} filter`}
                    onClick={() => removeFilter(filter.name, filter.value, filter.multiple)}
                  >
                    <span aria-hidden="true">×</span>
                  </button>
                </li>
              ))}
            </ul>
          </div>
        )}
      </section>

      {loading && (
        <div className="state-card" role="status" aria-live="polite">
          Loading users…
        </div>
      )}
      {!loading && error !== null && (
        <div className="state-card state-error" role="alert">
          <p>{describeError(error)}</p>
          <button
            className="button button-secondary"
            type="button"
            onClick={() => setAttempt((value) => value + 1)}
          >
            Try again
          </button>
        </div>
      )}
      {!loading && !error && result && outOfRange && (
        <div className="state-card state-empty">
          <p>This page is outside the available results.</p>
          <button className="button button-secondary" type="button" onClick={() => changePage(0)}>
            Return to the first page
          </button>
        </div>
      )}
      {!loading && !error && result && !outOfRange && result.empty && (
        <div className="state-card state-empty">No users match these filters.</div>
      )}
      {!loading && !error && result && !outOfRange && !result.empty && (
        <section className="results-panel" aria-label="User results">
          <div className="results-heading">
            <p>
              {result.totalElements.toLocaleString()}{" "}
              {result.totalElements === 1 ? "user" : "users"}
            </p>
            <label>
              <span>Rows per page</span>
              <select value={result.size} onChange={(event) => changeSize(event.target.value)}>
                {pageSizes.map((size) => <option key={size} value={size}>{size}</option>)}
              </select>
            </label>
          </div>
          <div className="table-scroll">
            <table className="data-table">
              <thead>
                <tr>
                  <th scope="col">ID</th>
                  <th scope="col">Username</th>
                  <th scope="col">Email</th>
                  <th scope="col">Roles</th>
                  <th scope="col">Registered</th>
                  <th scope="col">Last login</th>
                  <th scope="col">Linked sign-in providers</th>
                  <th scope="col"><span className="sr-only">Actions</span></th>
                </tr>
              </thead>
              <tbody>
                {result.content.map((user) => (
                  <tr key={user.id}>
                    <td>{user.id}</td>
                    <td>
                      <Link
                        className="table-primary-link"
                        to={`/admin/users/${user.id}`}
                        state={{ from: `${location.pathname}${location.search}` }}
                      >
                        {user.username}
                      </Link>
                    </td>
                    <td>{user.email}</td>
                    <td>{user.roles.map(roleLabel).join(", ") || "—"}</td>
                    <td>{formatDateTime(user.registeredAt)}</td>
                    <td>{formatDateTime(user.lastLogin, "Never signed in")}</td>
                    <td>{user.linkedProviders.join(", ") || "No linked sign-in providers"}</td>
                    <td className="table-action">
                      <Link
                        to={`/admin/users/${user.id}`}
                        state={{ from: `${location.pathname}${location.search}` }}
                        aria-label={`View ${user.username}`}
                      >
                        View details <span aria-hidden="true">→</span>
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <nav className="pagination" aria-label="User result pages">
            <button
              className="button button-secondary"
              type="button"
              disabled={result.first}
              onClick={() => changePage(Math.max(0, result.number - 1))}
            >
              Previous
            </button>
            <span>Page {result.number + 1} of {Math.max(result.totalPages, 1)}</span>
            <button
              className="button button-secondary"
              type="button"
              disabled={result.last || result.totalPages === 0}
              onClick={() => changePage(result.number + 1)}
            >
              Next
            </button>
          </nav>
        </section>
      )}

      {creating && (
        <CreateUserDialog
          onClose={() => setCreating(false)}
          onCreated={() => {
            setCreating(false);
            setSuccess("User created. The current list has been refreshed.");
            setAttempt((value) => value + 1);
          }}
        />
      )}
    </div>
  );
}

function UserUpdateDialog({
  user,
  onClose,
  onSaved,
}: {
  user: AdminUserShortDto;
  onClose: () => void;
  onSaved: (updatedUser: AdminUserShortDto) => void;
}) {
  const [username, setUsername] = useState(user.username);
  const [email, setEmail] = useState(user.email);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [fieldErrors, setFieldErrors] = useState<{ username?: string; email?: string }>({});
  const [touched, setTouched] = useState<{ username?: boolean; email?: boolean }>({});

  function validateField(field: "username" | "email", value: string): string {
    return field === "username"
      ? requiredFieldError(value, "username")
      : emailFieldError(value);
  }

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (busy) return;
    const form = event.currentTarget;
    const errors = {
      username: validateField("username", username),
      email: validateField("email", email),
    };
    setFieldErrors(errors);
    setTouched({ username: true, email: true });
    if (Object.values(errors).some(Boolean)) {
      focusFirstInvalidField(form);
      return;
    }
    setBusy(true);
    setError("");
    try {
      const updatedUser = await updateAdminUser(user.id, {
        username: username.trim(),
        email: email.trim(),
      });
      onSaved(updatedUser);
    } catch (requestError) {
      setError(describeError(requestError));
    } finally {
      setBusy(false);
    }
  }

  return (
    <AdminDialog title="Update user" onClose={() => { if (!busy) onClose(); }}>
      <form className="admin-form" noValidate onSubmit={(event) => void submit(event)}>
        <label className="admin-form-field">
          <span>Username</span>
          <input
            id="update-user-username"
            name="username"
            type="text"
            autoComplete="username"
            value={username}
            onChange={(event) => {
              const value = event.target.value;
              setUsername(value);
              if (touched.username) {
                setFieldErrors((current) => ({
                  ...current,
                  username: validateField("username", value),
                }));
              }
            }}
            onBlur={() => {
              setTouched((current) => ({ ...current, username: true }));
              setFieldErrors((current) => ({
                ...current,
                username: validateField("username", username),
              }));
            }}
            required
            aria-invalid={Boolean(fieldErrors.username)}
            aria-describedby={fieldErrors.username ? "update-user-username-error" : undefined}
          />
          {fieldErrors.username && (
            <span className="field-error" id="update-user-username-error">
              {fieldErrors.username}
            </span>
          )}
        </label>
        <label className="admin-form-field">
          <span>Email</span>
          <input
            id="update-user-email"
            name="email"
            type="email"
            autoComplete="email"
            value={email}
            onChange={(event) => {
              const value = event.target.value;
              setEmail(value);
              if (touched.email) {
                setFieldErrors((current) => ({
                  ...current,
                  email: validateField("email", value),
                }));
              }
            }}
            onBlur={() => {
              setTouched((current) => ({ ...current, email: true }));
              setFieldErrors((current) => ({
                ...current,
                email: validateField("email", email),
              }));
            }}
            required
            aria-invalid={Boolean(fieldErrors.email)}
            aria-describedby={[
              "update-user-email-example",
              ...(fieldErrors.email ? ["update-user-email-error"] : []),
            ].join(" ")}
          />
          <span className="field-hint" id="update-user-email-example">
            Example: name@example.com
          </span>
          {fieldErrors.email && (
            <span className="field-error" id="update-user-email-error">
              {fieldErrors.email}
            </span>
          )}
        </label>
        <p className="helper-text">Roles cannot be changed through this endpoint.</p>
        <Feedback error={error} />
        <div className="admin-form-actions">
          <button className="button button-secondary" type="button" onClick={onClose} disabled={busy}>
            Cancel
          </button>
          <button className="button button-primary" type="submit" disabled={busy}>
            {busy ? "Saving…" : "Save changes"}
          </button>
        </div>
      </form>
    </AdminDialog>
  );
}

function UserPasswordDialog({
  user,
  onClose,
  onSaved,
}: {
  user: AdminUserShortDto;
  onClose: () => void;
  onSaved: () => void;
}) {
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [fieldErrors, setFieldErrors] = useState<{
    password?: string;
    confirmPassword?: string;
  }>({});

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (busy) return;
    const form = event.currentTarget;
    const errors = {
      password: requiredFieldError(password, "new password"),
      confirmPassword: requiredFieldError(confirmPassword, "password confirmation")
        || (password === confirmPassword ? "" : "Please make sure both passwords match."),
    };
    setFieldErrors(errors);
    if (Object.values(errors).some(Boolean)) {
      focusFirstInvalidField(form);
      return;
    }
    setBusy(true);
    setError("");
    try {
      await setAdminUserPassword(user.id, { newPassword: password });
      onSaved();
    } catch (requestError) {
      setError(describeError(requestError));
    } finally {
      setPassword("");
      setConfirmPassword("");
      setFieldErrors({});
      setBusy(false);
    }
  }

  return (
    <AdminDialog title={`Set password for ${user.username}`} onClose={() => { if (!busy) onClose(); }}>
      <form className="admin-form" noValidate onSubmit={(event) => void submit(event)}>
        <label className="admin-form-field">
          <span>New password</span>
          <input
            id="admin-user-new-password"
            name="newPassword"
            type="password"
            autoComplete="new-password"
            value={password}
            onChange={(event) => {
              const value = event.target.value;
              setPassword(value);
              if (fieldErrors.password || fieldErrors.confirmPassword) {
                setFieldErrors((current) => ({
                  password: requiredFieldError(value, "new password"),
                  confirmPassword: confirmPassword
                    ? (value === confirmPassword ? "" : "Please make sure both passwords match.")
                    : current.confirmPassword,
                }));
              }
            }}
            onBlur={() => setFieldErrors((current) => ({
              ...current,
              password: requiredFieldError(password, "new password"),
            }))}
            required
            aria-invalid={Boolean(fieldErrors.password)}
            aria-describedby={fieldErrors.password ? "admin-user-new-password-error" : undefined}
          />
          {fieldErrors.password && (
            <span className="field-error" id="admin-user-new-password-error">
              {fieldErrors.password}
            </span>
          )}
        </label>
        <label className="admin-form-field">
          <span>Confirm new password</span>
          <input
            id="admin-user-confirm-password"
            name="confirmPassword"
            type="password"
            autoComplete="new-password"
            value={confirmPassword}
            onChange={(event) => {
              const value = event.target.value;
              setConfirmPassword(value);
              if (fieldErrors.confirmPassword) {
                setFieldErrors((current) => ({
                  ...current,
                  confirmPassword: requiredFieldError(value, "password confirmation")
                    || (password === value ? "" : "Please make sure both passwords match."),
                }));
              }
            }}
            onBlur={() => setFieldErrors((current) => ({
              ...current,
              confirmPassword: requiredFieldError(confirmPassword, "password confirmation")
                || (password === confirmPassword ? "" : "Please make sure both passwords match."),
            }))}
            required
            aria-invalid={Boolean(fieldErrors.confirmPassword)}
            aria-describedby={fieldErrors.confirmPassword ? "admin-user-confirm-password-error" : undefined}
          />
          {fieldErrors.confirmPassword && (
            <span className="field-error" id="admin-user-confirm-password-error">
              {fieldErrors.confirmPassword}
            </span>
          )}
        </label>
        <p className="helper-text">This sets a new password directly; the existing password is never retrieved.</p>
        <Feedback error={error} />
        <div className="admin-form-actions">
          <button className="button button-secondary" type="button" onClick={onClose} disabled={busy}>
            Cancel
          </button>
          <button className="button button-primary" type="submit" disabled={busy}>
            {busy ? "Setting password…" : "Set password"}
          </button>
        </div>
      </form>
    </AdminDialog>
  );
}

function DetailField({ label, children }: { label: string; children: ReactNode }) {
  return (
    <div className="detail-field">
      <dt>{label}</dt>
      <dd>{children}</dd>
    </div>
  );
}

export function AdminUserDetailPage() {
  const { userId: rawUserId } = useParams();
  const userId = rawUserId && /^\d+$/.test(rawUserId) ? Number(rawUserId) : Number.NaN;
  const location = useLocation();
  const navigate = useNavigate();
  const { session, updateUser } = useSession();
  const [user, setUser] = useState<AdminUserDetailDto | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<unknown>(null);
  const [attempt, setAttempt] = useState(0);
  const [editing, setEditing] = useState(false);
  const [settingPassword, setSettingPassword] = useState(false);
  const [confirmingDelete, setConfirmingDelete] = useState(false);
  const [busy, setBusy] = useState(false);
  const [success, setSuccess] = useState("");
  const [mutationError, setMutationError] = useState("");

  const state = location.state as { from?: unknown } | null;
  const returnTo =
    typeof state?.from === "string" && state.from.startsWith("/admin/users")
      ? state.from
      : "/admin/users";

  useEffect(() => {
    const controller = new AbortController();
    if (!Number.isSafeInteger(userId) || userId < 1) {
      setUser(null);
      setError(new Error("The user ID in this address is invalid."));
      setLoading(false);
      return () => controller.abort();
    }
    setLoading(true);
    setError(null);
    getAdminUser(userId, controller.signal).then(
      (details) => {
        setUser(details);
        setLoading(false);
      },
      (requestError: unknown) => {
        if (!controller.signal.aborted) {
          setError(requestError);
          setLoading(false);
        }
      },
    );
    return () => controller.abort();
  }, [userId, attempt]);

  async function removeUser() {
    if (!user || busy) return;
    setBusy(true);
    setMutationError("");
    try {
      await deleteAdminUser(user.id);
      navigate(returnTo, {
        replace: true,
        state: { notice: "User deleted successfully." },
      });
    } catch (requestError) {
      setMutationError(describeError(requestError));
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="page-container detail-page">
      <Link className="back-link" to={returnTo}>← Back to users</Link>
      <div className="page-heading">
        <p className="eyebrow">Administration</p>
        <h1>User details</h1>
      </div>
      {loading && <div className="state-card" role="status" aria-live="polite">Loading user…</div>}
      {!loading && error !== null && (
        <div className="state-card state-error" role="alert">
          <p>{describeError(error)}</p>
          <button
            className="button button-secondary"
            type="button"
            onClick={() => setAttempt((value) => value + 1)}
          >
            Try again
          </button>
        </div>
      )}
      {!loading && !error && user && (
        <>
          <section className="detail-card" aria-label="User account details">
            <h2 className="detail-card-heading">Account information</h2>
            <dl className="detail-grid">
              <DetailField label="User ID">{user.id}</DetailField>
              <DetailField label="Username">{user.username}</DetailField>
              <DetailField label="Email">{user.email}</DetailField>
              <DetailField label="Registration date">{formatDateTime(user.registeredAt)}</DetailField>
              <DetailField label="Roles">{user.roles.map(roleLabel).join(", ") || "—"}</DetailField>
            </dl>
            <p className="helper-text">Timestamps are shown as supplied in server-local time.</p>
          </section>
          <section className="detail-card admin-access-card" aria-label="Access and activity">
            <h2 className="detail-card-heading">Access and activity</h2>
            <dl className="detail-grid">
              <DetailField label="Latest successful login">
                {formatDateTime(user.lastLoginAt, "Never signed in")}
              </DetailField>
              <DetailField label="Linked sign-in providers">
                {user.linkedProviders.length
                  ? user.linkedProviders.join(", ")
                  : "No linked sign-in providers"}
              </DetailField>
            </dl>
          </section>
          <section className="detail-section" aria-label="User administration actions">
            <div className="admin-actions">
              <span className="admin-badge">Administrator actions</span>
              <button
                className="button button-secondary"
                type="button"
                onClick={() => {
                  setMutationError("");
                  setSuccess("");
                  setEditing(true);
                }}
              >
                Update username or email
              </button>
              <button
                className="button button-secondary"
                type="button"
                onClick={() => {
                  setMutationError("");
                  setSuccess("");
                  setSettingPassword(true);
                }}
              >
                Set password
              </button>
              {session.status === "authenticated" && user.id !== session.user.id && (
                <button
                  className="button button-danger"
                  type="button"
                  onClick={() => {
                    setMutationError("");
                    setSuccess("");
                    setConfirmingDelete(true);
                  }}
                  disabled={busy}
                >
                  Delete user
                </button>
              )}
            </div>
            <Feedback error={mutationError} success={success} />
          </section>
        </>
      )}

      {editing && user && (
        <UserUpdateDialog
          user={user}
          onClose={() => setEditing(false)}
          onSaved={(updatedUser) => {
            setEditing(false);
            if (session.status === "authenticated" && updatedUser.id === session.user.id) {
              updateUser({ username: updatedUser.username, email: updatedUser.email });
            }
            setSuccess("User profile updated.");
            setAttempt((value) => value + 1);
          }}
        />
      )}
      {settingPassword && user && (
        <UserPasswordDialog
          user={user}
          onClose={() => setSettingPassword(false)}
          onSaved={() => {
            setSettingPassword(false);
            setSuccess("The user password was set successfully.");
          }}
        />
      )}
      {confirmingDelete && user && (
        <AdminDialog title="Delete user?" onClose={() => { if (!busy) setConfirmingDelete(false); }}>
          <div className="admin-form">
            <p>
              This permanently deletes {user.username}'s account and cannot be undone.
              They will no longer be able to sign in with this account.
            </p>
            <Feedback error={mutationError} />
            <div className="admin-form-actions">
              <button
                className="button button-secondary"
                type="button"
                onClick={() => setConfirmingDelete(false)}
                disabled={busy}
              >
                Cancel
              </button>
              <button
                className="button button-danger"
                type="button"
                onClick={() => void removeUser()}
                disabled={busy}
              >
                {busy ? "Deleting…" : "Delete user"}
              </button>
            </div>
          </div>
        </AdminDialog>
      )}
    </div>
  );
}
