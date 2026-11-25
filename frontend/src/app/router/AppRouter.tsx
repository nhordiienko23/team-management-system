import { Navigate, Outlet, Route, Routes } from "react-router-dom";
import { useSession } from "../session/SessionContext";
import { AuthenticatedLayout } from "../../components/AuthenticatedLayout";
import { SignInPage } from "../../features/auth/pages/SignInPage";
import { SignUpPage } from "../../features/auth/pages/SignUpPage";
import { AdminUserDetailPage, AdminUsersPage } from "../../features/admin-users/pages/AdminUsers";
import {
  CoachDetailPage,
  CoachesPage,
  HomePage,
  PlayerDetailPage,
  PlayersPage,
  ProfilePage,
  TeamDetailPage,
  TeamsPage,
} from "../../features/core/pages/CorePages";

function ProtectedRoutes() {
  const { session, refresh } = useSession();

  if (session.status === "loading") {
    return <main className="status-page" aria-live="polite">Loading your session…</main>;
  }

  if (session.status === "anonymous") {
    return <Navigate to="/sign-in" replace />;
  }

  if (session.status === "error") {
    return (
      <main className="status-page" role="alert">
        <section className="status-card">
          <p>{session.message}</p>
          <button className="button button-primary" onClick={() => void refresh()}>
            Try again
          </button>
        </section>
      </main>
    );
  }

  return (
    <AuthenticatedLayout>
      <Outlet />
    </AuthenticatedLayout>
  );
}

function AdminOnlyRoutes() {
  const { session } = useSession();

  if (session.status !== "authenticated" ||
      !session.user.authorities.includes("ROLE_ADMIN")) {
    return <Navigate to="/" replace />;
  }

  return <Outlet />;
}

export function AppRouter() {
  return (
    <Routes>
      <Route path="/sign-in" element={<SignInPage />} />
      <Route path="/sign-up" element={<SignUpPage />} />
      <Route element={<ProtectedRoutes />}>
        <Route index element={<HomePage />} />
        <Route path="teams" element={<TeamsPage />} />
        <Route path="teams/:id" element={<TeamDetailPage />} />
        <Route path="players" element={<PlayersPage />} />
        <Route path="players/:id" element={<PlayerDetailPage />} />
        <Route path="coaches" element={<CoachesPage />} />
        <Route path="coaches/:id" element={<CoachDetailPage />} />
        <Route path="profile" element={<ProfilePage />} />
        <Route element={<AdminOnlyRoutes />}>
          <Route path="admin/users" element={<AdminUsersPage />} />
          <Route path="admin/users/:userId" element={<AdminUserDetailPage />} />
        </Route>
      </Route>
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
