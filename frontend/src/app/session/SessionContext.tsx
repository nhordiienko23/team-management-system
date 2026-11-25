import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useMemo,
  useRef,
  useState,
  type ReactNode,
} from "react";
import {
  ApiError,
  clearCsrfToken,
  getCsrfToken,
  requestJson,
  type CsrfTokenResponse,
} from "../../api/client";

export interface CurrentUser {
  id: number;
  username: string;
  email: string;
  authorities: string[];
}

type SessionState =
  | { status: "loading" }
  | { status: "anonymous" }
  | { status: "authenticated"; user: CurrentUser }
  | { status: "error"; message: string };

interface SessionContextValue {
  session: SessionState;
  csrfToken: CsrfTokenResponse | null;
  csrfError: string | null;
  refresh: () => Promise<void>;
  refreshCsrfToken: () => Promise<void>;
  clearSession: () => void;
  updateUser: (identity: Pick<CurrentUser, "username" | "email">) => void;
}

const SessionContext = createContext<SessionContextValue | undefined>(undefined);

export function SessionProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<SessionState>({ status: "loading" });
  const [csrfToken, setCsrfToken] = useState<CsrfTokenResponse | null>(null);
  const [csrfError, setCsrfError] = useState<string | null>(null);
  const refreshInFlight = useRef<Promise<void> | null>(null);

  const refresh = useCallback(async () => {
    if (refreshInFlight.current) {
      return refreshInFlight.current;
    }

    setSession({ status: "loading" });
    setCsrfToken(null);
    setCsrfError(null);

    const refreshRequest = (async () => {
      const [csrfResult, identityResult] = await Promise.allSettled([
        getCsrfToken(true),
        requestJson<CurrentUser>("/api/auth/me"),
      ]);

      if (csrfResult.status === "fulfilled") {
        setCsrfToken(csrfResult.value);
      } else {
        setCsrfError(
          csrfResult.reason instanceof Error
            ? csrfResult.reason.message
            : "Unable to load the security token.",
        );
      }

      if (identityResult.status === "fulfilled") {
        setSession({ status: "authenticated", user: identityResult.value });
      } else if (identityResult.reason instanceof ApiError && identityResult.reason.status === 401) {
        setSession({ status: "anonymous" });
      } else {
        setSession({
          status: "error",
          message:
            identityResult.reason instanceof Error
              ? identityResult.reason.message
              : "Unable to load your session.",
        });
      }
    })();

    refreshInFlight.current = refreshRequest;
    try {
      await refreshRequest;
    } finally {
      refreshInFlight.current = null;
    }
  }, []);

  const refreshCsrfToken = useCallback(async () => {
    setCsrfToken(null);
    setCsrfError(null);
    try {
      setCsrfToken(await getCsrfToken(true));
    } catch (error) {
      setCsrfError(
        error instanceof Error ? error.message : "Unable to load the security token.",
      );
    }
  }, []);

  const clearSession = useCallback(() => {
    clearCsrfToken();
    setSession({ status: "anonymous" });
    setCsrfToken(null);
    setCsrfError(null);
  }, []);

  const updateUser = useCallback(
    (identity: Pick<CurrentUser, "username" | "email">) => {
      setSession((current) =>
        current.status === "authenticated"
          ? {
              ...current,
              user: { ...current.user, ...identity },
            }
          : current,
      );
    },
    [],
  );

  useEffect(() => {
    void refresh();
  }, [refresh]);

  useEffect(() => {
    const invalidateToken = () => {
      setCsrfToken(null);
      setCsrfError("Your security token expired. Refresh it before trying again.");
    };
    const refreshSession = () => {
      void refresh();
    };
    const expireSession = () => {
      clearSession();
      if (window.location.pathname !== "/sign-in") {
        window.location.assign("/sign-in");
      }
    };

    window.addEventListener("csrf-token-invalidated", invalidateToken);
    window.addEventListener("session-refresh", refreshSession);
    window.addEventListener("session-expired", expireSession);
    return () => {
      window.removeEventListener("csrf-token-invalidated", invalidateToken);
      window.removeEventListener("session-refresh", refreshSession);
      window.removeEventListener("session-expired", expireSession);
    };
  }, [clearSession, refresh]);

  const value = useMemo(
    () => ({
      session,
      csrfToken,
      csrfError,
      refresh,
      refreshCsrfToken,
      clearSession,
      updateUser,
    }),
    [session, csrfToken, csrfError, refresh, refreshCsrfToken, clearSession, updateUser],
  );
  return <SessionContext.Provider value={value}>{children}</SessionContext.Provider>;
}

export function useSession(): SessionContextValue {
  const context = useContext(SessionContext);
  if (!context) {
    throw new Error("useSession must be used within a SessionProvider");
  }
  return context;
}
