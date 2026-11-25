import {
  createContext,
  useCallback,
  useContext,
  useEffect,
  useRef,
  useState,
  type PropsWithChildren,
} from "react";

export type Theme = "dark" | "light";

const THEME_STORAGE_KEY = "team-management-theme";
const THEME_TRANSITION_DURATION_MS = 300;
const THEME_COLORS: Record<Theme, string> = {
  dark: "#111318",
  light: "#f4f6fb",
};

interface ThemeContextValue {
  theme: Theme;
  setTheme: (theme: Theme) => void;
}

const ThemeContext = createContext<ThemeContextValue | null>(null);

function isTheme(value: string | undefined): value is Theme {
  return value === "dark" || value === "light";
}

function getDocumentTheme(): Theme {
  const documentTheme = document.documentElement.dataset.theme;
  return isTheme(documentTheme) ? documentTheme : "dark";
}

export function ThemeProvider({ children }: PropsWithChildren) {
  const [theme, setCurrentTheme] = useState<Theme>(getDocumentTheme);
  const transitionTimer = useRef<number | null>(null);

  const setTheme = useCallback((nextTheme: Theme) => {
    if (getDocumentTheme() !== nextTheme) {
      document.documentElement.dataset.themeTransitioning = "true";
    }
    setCurrentTheme(nextTheme);
    try {
      window.localStorage.setItem(THEME_STORAGE_KEY, nextTheme);
    } catch {
      // The selected theme remains active for this page even if storage is unavailable.
    }
  }, []);

  useEffect(() => {
    document.documentElement.dataset.theme = theme;
    document.documentElement.style.colorScheme = theme;
    document.documentElement.style.backgroundColor = THEME_COLORS[theme];

    const themeColor = document.querySelector<HTMLMetaElement>('meta[name="theme-color"]');
    if (themeColor) {
      themeColor.content = THEME_COLORS[theme];
    }

    if (document.documentElement.dataset.themeTransitioning === "true") {
      if (transitionTimer.current !== null) {
        window.clearTimeout(transitionTimer.current);
      }
      transitionTimer.current = window.setTimeout(() => {
        delete document.documentElement.dataset.themeTransitioning;
        transitionTimer.current = null;
      }, THEME_TRANSITION_DURATION_MS);
    }

    return () => {
      if (transitionTimer.current !== null) {
        window.clearTimeout(transitionTimer.current);
        transitionTimer.current = null;
      }
    };
  }, [theme]);

  return (
    <ThemeContext.Provider value={{ theme, setTheme }}>
      {children}
    </ThemeContext.Provider>
  );
}

export function useTheme(): ThemeContextValue {
  const context = useContext(ThemeContext);
  if (!context) {
    throw new Error("useTheme must be used within a ThemeProvider.");
  }
  return context;
}
