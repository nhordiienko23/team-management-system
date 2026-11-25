import { useState, type ReactNode } from "react";
import { NavLink } from "react-router-dom";
import { useSession } from "../app/session/SessionContext";
import { ThemeSwitch } from "./ThemeSwitch";

const navigation = [
  { to: "/", label: "Home", end: true },
  { to: "/teams", label: "Teams" },
  { to: "/players", label: "Players" },
  { to: "/coaches", label: "Coaches" },
  { to: "/profile", label: "My Profile" },
];

export function AuthenticatedLayout({ children }: { children: ReactNode }) {
  const { session, csrfToken, csrfError, refreshCsrfToken } = useSession();
  const [menuOpen, setMenuOpen] = useState(false);

  if (session.status !== "authenticated") {
    return null;
  }

  const isAdmin = session.user.authorities.includes("ROLE_ADMIN");

  return (
    <div className="app-shell">
      <header className="app-header">
        <NavLink className="app-brand" to="/" aria-label="Team Management home">
          <span className="brand-mark" aria-hidden="true">TM</span>
          <span>Team Management</span>
        </NavLink>
        <button
          className="mobile-menu-toggle"
          type="button"
          aria-expanded={menuOpen}
          aria-controls="primary-navigation"
          onClick={() => setMenuOpen((open) => !open)}
        >
          <span className="sr-only">{menuOpen ? "Close navigation" : "Open navigation"}</span>
          <span aria-hidden="true">{menuOpen ? "×" : "☰"}</span>
        </button>
        <nav
          className={`primary-navigation${menuOpen ? " is-open" : ""}`}
          id="primary-navigation"
          aria-label="Main navigation"
        >
          {navigation.map((item) => (
            <NavLink
              key={item.to}
              to={item.to}
              end={item.end}
              onClick={() => setMenuOpen(false)}
              className={({ isActive }) => `navigation-link${isActive ? " is-active" : ""}`}
            >
              {item.label}
            </NavLink>
          ))}
          {isAdmin && (
            <NavLink
              to="/admin/users"
              onClick={() => setMenuOpen(false)}
              className={({ isActive }) => `navigation-link${isActive ? " is-active" : ""}`}
            >
              User Management
            </NavLink>
          )}
        </nav>
        <div className="user-summary">
          <span className="user-summary-name">{session.user.username}</span>
          <span className="role-badge">
            {session.user.authorities.includes("ROLE_ADMIN") ? "Administrator" : "User"}
          </span>
        </div>
        <ThemeSwitch />
        {csrfToken ? (
          <form action="/logout" method="post">
            <input
              type="hidden"
              name={csrfToken.parameterName}
              value={csrfToken.token}
            />
            <button className="button button-quiet" type="submit">Log out</button>
          </form>
        ) : (
          <button
            className="button button-quiet"
            type="button"
            onClick={() => void refreshCsrfToken()}
            aria-label={csrfError ?? "Load security token to log out"}
          >
            {csrfError ? "Retry token" : "Loading…"}
          </button>
        )}
      </header>
      <main className="app-content">{children}</main>
      <footer className="app-footer">Team Management System</footer>
    </div>
  );
}
