import {
  useCallback,
  useEffect,
  useState,
  type FormEvent,
  type ReactNode,
} from "react";
import { Link, useLocation, useNavigate, useParams } from "react-router-dom";
import { ApiError, requestJson } from "../../../api/client";
import {
  getCoach,
  getCoachingStaff,
  getColleagues,
  getPage,
  getPlayer,
  getProfile,
  getTeam,
  getTeamLineup,
  getTeammates,
  type CoachDto,
  type PageResponse,
  type PlayerDto,
  type ProfileDto,
  type TeamDto,
  type TeamGroup,
} from "../../../api/sports";
import { useSession } from "../../../app/session/SessionContext";
import {
  AdminCreateAction,
  AdminEntityActions,
  AdminTeamMemberRemove,
  AdminTeamRosterActions,
} from "./AdminSports";
import { formatUsd } from "../../../utils/formatUsd";
import {
  emailFieldError,
  focusFirstInvalidField,
  requiredFieldError,
} from "../../../utils/formValidation";
import "./CorePages.css";

type Resource<T> =
  | { status: "loading" }
  | { status: "loaded"; data: T }
  | { status: "error"; error: unknown };

function useResource<T>(key: string | undefined, load: (signal: AbortSignal) => Promise<T>) {
  const [resource, setResource] = useState<Resource<T>>({ status: "loading" });
  const [attempt, setAttempt] = useState(0);

  useEffect(() => {
    if (!key) {
      setResource({ status: "error", error: new Error("This address is not valid.") });
      return;
    }

    const controller = new AbortController();
    setResource({ status: "loading" });
    load(controller.signal).then(
      (data) => setResource({ status: "loaded", data }),
      (error: unknown) => {
        if (!controller.signal.aborted) {
          setResource({ status: "error", error });
        }
      },
    );
    return () => controller.abort();
  }, [key, load, attempt]);

  return {
    resource,
    retry: () => setAttempt((value) => value + 1),
  };
}

function describeError(error: unknown): string {
  if (!(error instanceof ApiError)) {
    return error instanceof Error
      ? error.message
      : "Something went wrong. Please try again.";
  }

  if (error.status === 0) {
    return error.message || "Unable to reach the server. Check your connection.";
  }
  if (error.status === 400) {
    return `The request could not be completed: ${error.message}`;
  }
  if (error.status === 401) {
    return "Your session has expired. Sign in again to continue.";
  }
  if (error.status === 403) {
    return error.error === "CSRF Failed"
      ? `Security token error: ${error.message}`
      : `Access denied: ${error.message}`;
  }
  if (error.status === 404) {
    return `Not found: ${error.message}`;
  }
  if (error.status === 409) {
    return `Conflict: ${error.message}`;
  }
  if (error.status >= 500) {
    return `The server encountered an error: ${error.message}`;
  }
  return error.message;
}

function PageHeading({ eyebrow, title, description }: {
  eyebrow: string;
  title: string;
  description?: string;
}) {
  return (
    <div className="page-heading">
      <p className="eyebrow">{eyebrow}</p>
      <h1>{title}</h1>
      {description && <p className="page-description">{description}</p>}
    </div>
  );
}

function LoadingState({ label = "Loading…" }: { label?: string }) {
  return (
    <div className="state-card" role="status" aria-live="polite">
      <span className="loading-spinner" aria-hidden="true" />
      <span>{label}</span>
    </div>
  );
}

function ErrorState({ error, onRetry }: { error: unknown; onRetry: () => void }) {
  return (
    <div className="state-card state-error" role="alert">
      <p>{describeError(error)}</p>
      <button className="button button-secondary" type="button" onClick={onRetry}>
        Try again
      </button>
    </div>
  );
}

function EmptyState({ children }: { children: ReactNode }) {
  return <div className="state-card state-empty">{children}</div>;
}

export function HomePage() {
  const { session } = useSession();
  if (session.status !== "authenticated") {
    return null;
  }

  const cards = [
    { to: "/teams", title: "Teams", description: "Browse teams, lineups, and coaching staff." },
    { to: "/players", title: "Players", description: "Explore player profiles and teammates." },
    { to: "/coaches", title: "Coaches", description: "Find coaches and their colleagues." },
    { to: "/profile", title: "My Profile", description: "Update your account and password." },
  ];

  return (
    <div className="page-container">
      <section className="welcome-panel">
        <p className="eyebrow">Your workspace</p>
        <h1>Welcome, {session.user.username}</h1>
        <p>Signed in as {session.user.email}. Browse the league or manage your profile.</p>
      </section>
      <section aria-labelledby="explore-heading">
        <div className="section-heading">
          <div>
            <p className="eyebrow">Get started</p>
            <h2 id="explore-heading">Explore Team Management</h2>
          </div>
        </div>
        <div className="home-card-grid">
          {cards.map((card, index) => (
            <Link className="home-link-card" to={card.to} key={card.to}>
              <span className="card-index" aria-hidden="true">0{index + 1}</span>
              <h3>{card.title}</h3>
              <p>{card.description}</p>
              <span className="card-arrow" aria-hidden="true">→</span>
            </Link>
          ))}
        </div>
      </section>
    </div>
  );
}

interface FilterField {
  name: string;
  label: string;
  type?: "text" | "number" | "select" | "positions";
  options?: string[];
}

interface FilterGroup {
  label: string;
  fields: FilterField[];
}

interface RangeFilter {
  min: string;
  max: string;
  label: string;
}

interface Column<T> {
  label: string;
  value: (row: T) => ReactNode;
}

interface ListConfig<T extends { id: number }> {
  title: string;
  description: string;
  adminKind: "team" | "player" | "coach";
  adminEntity: (row: T) => TeamDto | PlayerDto | CoachDto;
  endpoint: string;
  filterGroups: FilterGroup[];
  ranges: RangeFilter[];
  sorts: { value: string; label: string }[];
  columns: Column<T>[];
  rowName: (row: T) => string;
  detailsPath: (row: T) => string;
}

const teamConfig: ListConfig<TeamDto> = {
  title: "Teams",
  description: "Browse teams, their championship records, and the year they were founded.",
  adminKind: "team",
  adminEntity: (team) => team,
  endpoint: "/api/teams/search",
  filterGroups: [
    { label: "Team identity", fields: [
      { name: "teamName", label: "Team name" },
    ] },
    { label: "Team attributes", fields: [
      { name: "minChampionshipTitleCount", label: "Titles from", type: "number" },
      { name: "maxChampionshipTitleCount", label: "Titles to", type: "number" },
      { name: "minCreationYear", label: "Founded from", type: "number" },
      { name: "maxCreationYear", label: "Founded to", type: "number" },
    ] },
  ],
  ranges: [
    { min: "minChampionshipTitleCount", max: "maxChampionshipTitleCount", label: "Championship titles" },
    { min: "minCreationYear", max: "maxCreationYear", label: "Founded year" },
  ],
  sorts: [
    { value: "name", label: "Team name" },
    { value: "championshipTitleCount", label: "Championship titles" },
    { value: "creationYear", label: "Creation year" },
  ],
  columns: [
    { label: "Team", value: (team) => team.name },
    { label: "Championship titles", value: (team) => team.championshipTitleCount },
    { label: "Created", value: (team) => team.creationYear },
    {
      label: "Roster",
      value: (team) => `${team.players.length} players · ${team.coaches.length} coaches`,
    },
  ],
  rowName: (team) => team.name,
  detailsPath: (team) => `/teams/${team.id}`,
};

const playerConfig: ListConfig<PlayerDto> = {
  title: "Players",
  description: "Search players by name, team, position, rating, salary (USD), or championships.",
  adminKind: "player",
  adminEntity: (player) => player,
  endpoint: "/api/players/search",
  filterGroups: [
    { label: "Player identity and team", fields: [
      { name: "firstName", label: "First name" },
      { name: "lastName", label: "Last name" },
      { name: "teamName", label: "Team" },
    ] },
    { label: "Positions", fields: [
      { name: "positions", label: "Positions", type: "positions" },
    ] },
    { label: "Performance and experience", fields: [
      { name: "minRating", label: "Rating from", type: "number" },
      { name: "maxRating", label: "Rating to", type: "number" },
      { name: "minChampionshipWon", label: "Championships from", type: "number" },
      { name: "maxChampionshipWon", label: "Championships to", type: "number" },
    ] },
    { label: "USD salary range", fields: [
      { name: "minSalary", label: "Minimum salary (USD)", type: "number" },
      { name: "maxSalary", label: "Maximum salary (USD)", type: "number" },
    ] },
  ],
  ranges: [
    { min: "minRating", max: "maxRating", label: "Rating" },
    { min: "minChampionshipWon", max: "maxChampionshipWon", label: "Championships" },
    { min: "minSalary", max: "maxSalary", label: "Salary" },
  ],
  sorts: [
    { value: "firstName", label: "First name" },
    { value: "lastName", label: "Last name" },
    { value: "rating", label: "Rating" },
    { value: "championshipsWon", label: "Championships" },
    { value: "salary", label: "Salary (USD)" },
  ],
  columns: [
    { label: "Player", value: (player) => `${player.firstName} ${player.lastName}` },
    { label: "Position", value: (player) => player.positions.join(", ") || "—" },
    { label: "Team", value: (player) => player.team ?? "Free agent" },
    { label: "Rating", value: (player) => player.rating },
    { label: "Championships", value: (player) => player.championshipWon },
    { label: "Salary (USD)", value: (player) => formatUsd(player.salary) },
  ],
  rowName: (player) => `${player.firstName} ${player.lastName}`,
  detailsPath: (player) => `/players/${player.id}`,
};

const coachConfig: ListConfig<CoachDto> = {
  title: "Coaches",
  description: "Search coaches by name, team, experience, salary (USD), or championships.",
  adminKind: "coach",
  adminEntity: (coach) => coach,
  endpoint: "/api/coaches/search",
  filterGroups: [
    { label: "Coach identity and team", fields: [
      { name: "firstName", label: "First name" },
      { name: "lastName", label: "Last name" },
      { name: "teamName", label: "Team" },
    ] },
    { label: "Experience and championships", fields: [
      { name: "minYearsOfExperience", label: "Experience from", type: "number" },
      { name: "maxYearsOfExperience", label: "Experience to", type: "number" },
      { name: "minChampionshipsWon", label: "Championships from", type: "number" },
      { name: "maxChampionshipsWon", label: "Championships to", type: "number" },
    ] },
    { label: "USD salary range", fields: [
      { name: "minSalary", label: "Minimum salary (USD)", type: "number" },
      { name: "maxSalary", label: "Maximum salary (USD)", type: "number" },
    ] },
  ],
  ranges: [
    { min: "minYearsOfExperience", max: "maxYearsOfExperience", label: "Experience" },
    { min: "minChampionshipsWon", max: "maxChampionshipsWon", label: "Championships" },
    { min: "minSalary", max: "maxSalary", label: "Salary" },
  ],
  sorts: [
    { value: "firstName", label: "First name" },
    { value: "lastName", label: "Last name" },
    { value: "yearsOfExperience", label: "Experience" },
    { value: "championshipsWon", label: "Championships" },
    { value: "salary", label: "Salary (USD)" },
  ],
  columns: [
    { label: "Coach", value: (coach) => `${coach.firstName} ${coach.lastName}` },
    { label: "Team", value: (coach) => coach.teamName ?? "Free agent" },
    { label: "Experience", value: (coach) => `${coach.yearsExperience} years` },
    { label: "Championships", value: (coach) => coach.championshipWon },
    { label: "Salary (USD)", value: (coach) => formatUsd(coach.salary) },
  ],
  rowName: (coach) => `${coach.firstName} ${coach.lastName}`,
  detailsPath: (coach) => `/coaches/${coach.id}`,
};

function filterFields<T extends { id: number }>(config: ListConfig<T>): FilterField[] {
  return config.filterGroups.flatMap((group) => group.fields);
}

function rangeErrorId(range: RangeFilter): string {
  return `range-error-${range.min}`;
}

function validateRanges<T extends { id: number }>(
  config: ListConfig<T>,
  formData: FormData,
): Record<string, string> {
  const errors: Record<string, string> = {};
  for (const range of config.ranges) {
    const minValue = String(formData.get(range.min) ?? "").trim();
    const maxValue = String(formData.get(range.max) ?? "").trim();
    if (!minValue || !maxValue) continue;

    const min = Number(minValue);
    const max = Number(maxValue);
    if (Number.isFinite(min) && Number.isFinite(max) && min > max) {
      errors[rangeErrorId(range)] = `Minimum ${range.label.toLowerCase()} must not exceed maximum ${range.label.toLowerCase()}.`;
    }
  }
  return errors;
}

const positionNames: Record<string, string> = {
  PG: "Point Guard",
  SG: "Shooting Guard",
  SF: "Small Forward",
  PF: "Power Forward",
  C: "Center",
};

function activeFilters<T extends { id: number }>(
  config: ListConfig<T>,
  search: string,
): { name: string; value: string; label: string; multiple: boolean }[] {
  const params = new URLSearchParams(search);
  return filterFields(config).flatMap((filter) =>
    [...new Set(params.getAll(filter.name))]
      .filter((value) => value.trim())
      .map((value) => ({
        name: filter.name,
        value,
        label: `${filter.label}: ${filter.name === "positions"
          ? `${positionNames[value] ?? value} (${value})`
          : value}`,
        multiple: filter.type === "positions",
      })),
  );
}

function listRequestParams<T extends { id: number }>(
  config: ListConfig<T>,
  search: string,
): URLSearchParams {
  const urlParams = new URLSearchParams(search);
  const params = new URLSearchParams();
  const page = Number.parseInt(urlParams.get("page") ?? "0", 10);
  const size = Number.parseInt(urlParams.get("size") ?? "10", 10);

  params.set("page", String(Number.isInteger(page) && page >= 0 ? page : 0));
  params.set("size", String([10, 20, 50].includes(size) ? size : 10));

  for (const filter of filterFields(config)) {
    const values = urlParams.getAll(filter.name);
    for (const value of values) {
      if (value.trim()) {
        params.append(filter.name, value);
      }
    }
  }

  const sort = urlParams.get("sort");
  if (sort) {
    const [property, direction, ...extra] = sort.split(",");
    if (
      !extra.length &&
      config.sorts.some((option) => option.value === property) &&
      (direction === "asc" || direction === "desc")
    ) {
      params.set("sort", `${property},${direction}`);
    }
  }
  return params;
}

function searchString(params: URLSearchParams): string {
  const value = params.toString();
  return value ? `?${value}` : "";
}

function EntityListPage<T extends { id: number }>({ config }: { config: ListConfig<T> }) {
  const location = useLocation();
  const navigate = useNavigate();
  const [result, setResult] = useState<PageResponse<T> | null>(null);
  const [error, setError] = useState<unknown | null>(null);
  const [loading, setLoading] = useState(true);
  const [attempt, setAttempt] = useState(0);
  const [filterReset, setFilterReset] = useState(0);
  const [rangeErrors, setRangeErrors] = useState<Record<string, string>>({});
  const requestParams = listRequestParams(config, location.search);

  useEffect(() => {
    setRangeErrors({});
  }, [location.search]);

  useEffect(() => {
    const controller = new AbortController();
    setLoading(true);
    setError(null);
    getPage<T>(config.endpoint, requestParams, controller.signal).then(
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
  }, [config, location.search, attempt]);

  function navigateWith(params: URLSearchParams) {
    navigate({ pathname: location.pathname, search: searchString(params) });
  }

  function applyFilters(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const formData = new FormData(event.currentTarget);
    const errors = validateRanges(config, formData);
    setRangeErrors(errors);
    if (Object.keys(errors).length) return;

    const params = new URLSearchParams(location.search);
    params.delete("page");
    for (const filter of filterFields(config)) {
      params.delete(filter.name);
      const values = formData.getAll(filter.name);
      for (const rawValue of values) {
        const value = String(rawValue).trim();
        if (value) {
          params.append(filter.name, value);
        }
      }
    }
    navigateWith(params);
  }

  function clearFilters() {
    const params = new URLSearchParams(location.search);
    for (const filter of filterFields(config)) params.delete(filter.name);
    params.delete("page");
    setFilterReset((value) => value + 1);
    setRangeErrors({});
    navigateWith(params);
  }

  function removeFilter(name: string, value: string, multiple: boolean) {
    const params = new URLSearchParams(location.search);
    const remaining = multiple
      ? params.getAll(name).filter((current) => current !== value)
      : [];
    params.delete(name);
    for (const current of remaining) params.append(name, current);
    params.delete("page");
    navigateWith(params);
  }

  function changePage(page: number) {
    const params = new URLSearchParams(location.search);
    params.set("page", String(page));
    navigateWith(params);
  }

  function changeSize(size: string) {
    const params = new URLSearchParams(location.search);
    params.set("size", size);
    params.delete("page");
    navigateWith(params);
  }

  function changeSort(property: string, direction: string) {
    const params = new URLSearchParams(location.search);
    if (property) {
      params.set("sort", `${property},${direction}`);
    } else {
      params.delete("sort");
    }
    params.delete("page");
    navigateWith(params);
  }

  const urlParams = new URLSearchParams(location.search);
  const sortValue = urlParams.get("sort")?.split(",") ?? [];
  const validSort = config.sorts.some((sort) => sort.value === sortValue[0])
    ? sortValue[0]
    : "";
  const sortDirection = sortValue[1] === "desc" ? "desc" : "asc";
  const sortingBySalary = validSort === "salary";
  const currentPage = Number.parseInt(urlParams.get("page") ?? "0", 10);
  const outOfRange =
    !!result &&
    currentPage > 0 &&
    (result.totalPages === 0 || result.number >= result.totalPages);
  const applied = activeFilters(config, location.search);

  return (
    <div className="page-container">
      <div className="admin-list-toolbar">
        <PageHeading eyebrow="League directory" title={config.title} description={config.description} />
        <AdminCreateAction
          kind={config.adminKind}
          onChanged={() => setAttempt((value) => value + 1)}
        />
      </div>

      <section className="filter-panel" aria-label={`${config.title} search and filters`}>
        <form
          className="filter-form"
          key={`${location.search}:${filterReset}`}
          onSubmit={applyFilters}
          onChange={(event) => setRangeErrors(validateRanges(config, new FormData(event.currentTarget)))}
        >
          <div className="filter-groups">
            {config.filterGroups.map((group) => (
              <fieldset className="filter-group" key={group.label}>
                <legend>{group.label}</legend>
                <div className="filter-grid">
                  {group.fields.map((filter) => {
                    const range = config.ranges.find((candidate) =>
                      candidate.min === filter.name || candidate.max === filter.name,
                    );
                    const errorId = range ? rangeErrorId(range) : undefined;
                    const error = errorId ? rangeErrors[errorId] : undefined;
                    if (filter.type === "positions") {
                      return (
                        <fieldset
                          className="position-selector"
                          key={filter.name}
                          aria-describedby="position-help"
                        >
                          <legend>{filter.label}</legend>
                          <p id="position-help" className="field-hint">Matches any selected position</p>
                          <div className="position-chip-list">
                            {Object.entries(positionNames).map(([code, fullName]) => (
                              <label className="position-chip" key={code}>
                                <input
                                  type="checkbox"
                                  name={filter.name}
                                  value={code}
                                  defaultChecked={urlParams.getAll(filter.name).includes(code)}
                                />
                                <span className="position-chip-code">{code}</span>
                                <span className="position-chip-name">{fullName}</span>
                              </label>
                            ))}
                          </div>
                        </fieldset>
                      );
                    }
                    return (
                      <label className="filter-field" key={filter.name}>
                        <span>{filter.label}</span>
                        {filter.type === "select" ? (
                          <select
                            name={filter.name}
                            defaultValue={urlParams.get(filter.name) ?? ""}
                          >
                            <option value="">All</option>
                            {filter.options?.map((option) => (
                              <option key={option} value={option}>{option}</option>
                            ))}
                          </select>
                        ) : (
                          <input
                            name={filter.name}
                            type={filter.type ?? "text"}
                            defaultValue={urlParams.get(filter.name) ?? ""}
                            min={filter.type === "number" ? "0" : undefined}
                            step={filter.name.toLowerCase().includes("salary") ? "0.01" : undefined}
                            aria-invalid={error ? true : undefined}
                            aria-describedby={error ? errorId : undefined}
                          />
                        )}
                      </label>
                    );
                  })}
                </div>
                {config.ranges.filter((range) =>
                  group.fields.some((field) => field.name === range.min || field.name === range.max),
                ).map((range) => {
                  const error = rangeErrors[rangeErrorId(range)];
                  return error ? (
                    <p className="range-error" id={rangeErrorId(range)} key={range.min} role="alert">
                      {error}
                    </p>
                  ) : null;
                })}
              </fieldset>
            ))}
          </div>
          <div className="filter-actions">
            <div className="sort-controls">
              <label>
                <span>Sort by</span>
                <select
                  aria-label="Sort by"
                  value={validSort}
                  onChange={(event) => changeSort(event.target.value, sortDirection)}
                >
                  <option value="">Default order</option>
                  {config.sorts.map((sort) => (
                    <option key={sort.value} value={sort.value}>{sort.label}</option>
                  ))}
                </select>
              </label>
              <label>
                <span>{sortingBySalary ? "Salary order (USD)" : "Direction"}</span>
                <select
                  aria-label={sortingBySalary ? "Salary order (USD)" : "Sort direction"}
                  value={sortDirection}
                  disabled={!validSort}
                  onChange={(event) => changeSort(validSort, event.target.value)}
                >
                  <option value="asc">{sortingBySalary ? "Low to high" : "Ascending"}</option>
                  <option value="desc">{sortingBySalary ? "High to low" : "Descending"}</option>
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

      {loading && <LoadingState label={`Loading ${config.title.toLowerCase()}…`} />}
      {!loading && error !== null && (
        <ErrorState error={error} onRetry={() => setAttempt((value) => value + 1)} />
      )}
      {!loading && !error && result && outOfRange && (
        <EmptyState>
          <p>This page is outside the available results.</p>
          <button className="button button-secondary" onClick={() => changePage(0)}>
            Return to the first page
          </button>
        </EmptyState>
      )}
      {!loading && !error && result && !outOfRange && result.empty && (
        <EmptyState>No {config.title.toLowerCase()} match these filters.</EmptyState>
      )}
      {!loading && !error && result && !outOfRange && !result.empty && (
        <section className="results-panel" aria-label={`${config.title} results`}>
          <div className="results-heading">
            <p>
              {result.totalElements.toLocaleString()} {result.totalElements === 1 ? "result" : "results"}
            </p>
            <label>
              <span>Rows per page</span>
              <select
                value={result.size}
                onChange={(event) => changeSize(event.target.value)}
              >
                {[10, 20, 50].map((size) => <option key={size} value={size}>{size}</option>)}
              </select>
            </label>
          </div>
          <div className="table-scroll">
            <table className="data-table">
              <thead>
                <tr>
                  {config.columns.map((column) => <th key={column.label} scope="col">{column.label}</th>)}
                  <th scope="col"><span className="sr-only">Actions</span></th>
                </tr>
              </thead>
              <tbody>
                {result.content.map((row) => (
                  <tr key={row.id}>
                    {config.columns.map((column, index) => (
                      <td key={column.label}>
                        {index === 0 ? (
                          <Link className="table-primary-link" to={config.detailsPath(row)}>
                            {config.rowName(row)}
                          </Link>
                        ) : column.value(row)}
                      </td>
                    ))}
                    <td className="table-action">
                      <div className="table-row-actions">
                        <Link to={config.detailsPath(row)} aria-label={`View ${config.rowName(row)}`}>
                          View details <span aria-hidden="true">→</span>
                        </Link>
                        <AdminEntityActions
                          kind={config.adminKind}
                          entity={config.adminEntity(row)}
                          onChanged={() => setAttempt((value) => value + 1)}
                        />
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
          <nav className="pagination" aria-label={`${config.title} pages`}>
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
    </div>
  );
}

export function TeamsPage() {
  return <EntityListPage config={teamConfig} />;
}

export function PlayersPage() {
  return <EntityListPage config={playerConfig} />;
}

export function CoachesPage() {
  return <EntityListPage config={coachConfig} />;
}

function DetailLayout({
  eyebrow,
  title,
  backTo,
  backLabel,
  children,
}: {
  eyebrow: string;
  title: string;
  backTo: string;
  backLabel: string;
  children: ReactNode;
}) {
  return (
    <div className="page-container detail-page">
      <Link className="back-link" to={backTo}>← {backLabel}</Link>
      <PageHeading eyebrow={eyebrow} title={title} />
      {children}
    </div>
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

function RelatedGroup({
  title,
  resource,
  retry,
  route,
  freeAgentType,
  teamId,
  onAdminChange,
}: {
  title: string;
  resource: Resource<TeamGroup>;
  retry: () => void;
  route: "players" | "coaches";
  freeAgentType?: "player" | "coach";
  teamId?: number;
  onAdminChange?: () => void;
}) {
  return (
    <section className="detail-section">
      <div className="section-heading">
        <div>
          <p className="eyebrow">Related group</p>
          <h2>{title}</h2>
        </div>
      </div>
      {resource.status === "loading" && <LoadingState label={`Loading ${title.toLowerCase()}…`} />}
      {resource.status === "error" && (
        freeAgentType && isFreeAgentError(resource.error, freeAgentType) ? (
          <div className="state-card state-note" role="status">
            {freeAgentType === "player"
              ? "This player is a free agent and has no teammates."
              : "This coach is not currently with a team and has no colleagues."}
          </div>
        ) : (
          <ErrorState error={resource.error} onRetry={retry} />
        )
      )}
      {resource.status === "loaded" && resource.data.members.length === 0 && (
        <EmptyState>No related members are listed.</EmptyState>
      )}
      {resource.status === "loaded" && resource.data.members.length > 0 && (
        <ul className="member-list">
          {resource.data.members.map((member) => (
            <li key={member.id}>
              <Link to={`/${route}/${member.id}`}>{member.fullName}</Link>
              {teamId !== undefined && onAdminChange && (
                <AdminTeamMemberRemove
                  teamId={teamId}
                  member={member}
                  kind={route === "players" ? "player" : "coach"}
                  onRemoved={onAdminChange}
                />
              )}
            </li>
          ))}
        </ul>
      )}
    </section>
  );
}

function isFreeAgentError(error: unknown, type: "player" | "coach"): boolean {
  if (!(error instanceof ApiError) || error.status !== 400) {
    return false;
  }
  const message = error.message.toLowerCase();
  return type === "player"
    ? message.includes("free agent") && message.includes("teammates")
    : message.includes("no colleagues") || message.includes("free agent");
}

function validId(id: string | undefined): string | undefined {
  return id && /^\d+$/.test(id) ? id : undefined;
}

export function TeamDetailPage() {
  const navigate = useNavigate();
  const id = validId(useParams().id);
  const loadTeam = useCallback((signal: AbortSignal) => getTeam(id ?? "", signal), [id]);
  const loadLineup = useCallback((signal: AbortSignal) => getTeamLineup(id ?? "", signal), [id]);
  const loadStaff = useCallback((signal: AbortSignal) => getCoachingStaff(id ?? "", signal), [id]);
  const team = useResource(id, loadTeam);
  const lineup = useResource(id, loadLineup);
  const staff = useResource(id, loadStaff);

  return (
    <DetailLayout eyebrow="Team details" title={team.resource.status === "loaded" ? team.resource.data.name : "Team"} backTo="/teams" backLabel="All teams">
      {team.resource.status === "loading" && <LoadingState label="Loading team…" />}
      {team.resource.status === "error" && <ErrorState error={team.resource.error} onRetry={team.retry} />}
      {team.resource.status === "loaded" && (
        <>
          <section className="detail-card" aria-label="Team information">
            <dl className="detail-grid">
              <DetailField label="Team name">{team.resource.data.name}</DetailField>
              <DetailField label="Championship titles">{team.resource.data.championshipTitleCount}</DetailField>
              <DetailField label="Creation year">{team.resource.data.creationYear}</DetailField>
            </dl>
            <AdminEntityActions
              kind="team"
              entity={team.resource.data}
              onChanged={() => team.retry()}
              onDeleted={() => navigate("/teams", { replace: true })}
            />
          </section>
          <AdminTeamRosterActions
            team={team.resource.data}
            onChanged={() => {
              team.retry();
              lineup.retry();
              staff.retry();
            }}
          />
        </>
      )}
      <div className="detail-grid-panels">
        <RelatedGroup
          title="Lineup"
          resource={lineup.resource}
          retry={lineup.retry}
          route="players"
          teamId={team.resource.status === "loaded" ? team.resource.data.id : undefined}
          onAdminChange={() => {
            lineup.retry();
            staff.retry();
            team.retry();
          }}
        />
        <RelatedGroup
          title="Coaching staff"
          resource={staff.resource}
          retry={staff.retry}
          route="coaches"
          teamId={team.resource.status === "loaded" ? team.resource.data.id : undefined}
          onAdminChange={() => {
            lineup.retry();
            staff.retry();
            team.retry();
          }}
        />
      </div>
    </DetailLayout>
  );
}

export function PlayerDetailPage() {
  const navigate = useNavigate();
  const id = validId(useParams().id);
  const loadPlayer = useCallback((signal: AbortSignal) => getPlayer(id ?? "", signal), [id]);
  const loadTeammates = useCallback((signal: AbortSignal) => getTeammates(id ?? "", signal), [id]);
  const player = useResource(id, loadPlayer);
  const teammates = useResource(id, loadTeammates);
  const title =
    player.resource.status === "loaded"
      ? `${player.resource.data.firstName} ${player.resource.data.lastName}`
      : "Player";

  return (
    <DetailLayout eyebrow="Player details" title={title} backTo="/players" backLabel="All players">
      {player.resource.status === "loading" && <LoadingState label="Loading player…" />}
      {player.resource.status === "error" && <ErrorState error={player.resource.error} onRetry={player.retry} />}
      {player.resource.status === "loaded" && (
        <>
          <section className="detail-card" aria-label="Player information">
            <dl className="detail-grid">
              <DetailField label="Name">{title}</DetailField>
              <DetailField label="Position">{player.resource.data.positions.join(", ") || "—"}</DetailField>
              <DetailField label="Role">{player.resource.data.teamRole}</DetailField>
              <DetailField label="Team">{player.resource.data.team ?? "Free agent"}</DetailField>
              <DetailField label="Rating">{player.resource.data.rating}</DetailField>
              <DetailField label="Championships won">{player.resource.data.championshipWon}</DetailField>
              <DetailField label="Salary (USD)">{formatUsd(player.resource.data.salary)}</DetailField>
            </dl>
            <AdminEntityActions
              kind="player"
              entity={player.resource.data}
              onChanged={() => {
                player.retry();
                teammates.retry();
              }}
              onDeleted={() => navigate("/players", { replace: true })}
            />
          </section>
        </>
      )}
      <RelatedGroup
        title="Teammates"
        resource={teammates.resource}
        retry={teammates.retry}
        route="players"
        freeAgentType="player"
      />
    </DetailLayout>
  );
}

export function CoachDetailPage() {
  const navigate = useNavigate();
  const id = validId(useParams().id);
  const loadCoach = useCallback((signal: AbortSignal) => getCoach(id ?? "", signal), [id]);
  const loadColleagues = useCallback((signal: AbortSignal) => getColleagues(id ?? "", signal), [id]);
  const coach = useResource(id, loadCoach);
  const colleagues = useResource(id, loadColleagues);
  const title =
    coach.resource.status === "loaded"
      ? `${coach.resource.data.firstName} ${coach.resource.data.lastName}`
      : "Coach";

  return (
    <DetailLayout eyebrow="Coach details" title={title} backTo="/coaches" backLabel="All coaches">
      {coach.resource.status === "loading" && <LoadingState label="Loading coach…" />}
      {coach.resource.status === "error" && <ErrorState error={coach.resource.error} onRetry={coach.retry} />}
      {coach.resource.status === "loaded" && (
        <>
          <section className="detail-card" aria-label="Coach information">
            <dl className="detail-grid">
              <DetailField label="Name">{title}</DetailField>
              <DetailField label="Role">{coach.resource.data.teamRole}</DetailField>
              <DetailField label="Team">{coach.resource.data.teamName ?? "Not currently with a team"}</DetailField>
              <DetailField label="Years of experience">{coach.resource.data.yearsExperience}</DetailField>
              <DetailField label="Championships won">{coach.resource.data.championshipWon}</DetailField>
              <DetailField label="Salary (USD)">{formatUsd(coach.resource.data.salary)}</DetailField>
            </dl>
            <AdminEntityActions
              kind="coach"
              entity={coach.resource.data}
              onChanged={() => {
                coach.retry();
                colleagues.retry();
              }}
              onDeleted={() => navigate("/coaches", { replace: true })}
            />
          </section>
        </>
      )}
      <RelatedGroup
        title="Colleagues"
        resource={colleagues.resource}
        retry={colleagues.retry}
        route="coaches"
        freeAgentType="coach"
      />
    </DetailLayout>
  );
}

export function ProfilePage() {
  const { updateUser, clearSession } = useSession();
  const loadProfile = useCallback((signal: AbortSignal) => getProfile(signal), []);
  const profile = useResource("me", loadProfile);
  const [profileError, setProfileError] = useState("");
  const [profileSuccess, setProfileSuccess] = useState("");
  const [passwordError, setPasswordError] = useState("");
  const [passwordSuccess, setPasswordSuccess] = useState("");
  const [deleteError, setDeleteError] = useState("");
  const [isSavingProfile, setIsSavingProfile] = useState(false);
  const [isSavingPassword, setIsSavingPassword] = useState(false);
  const [isDeleting, setIsDeleting] = useState(false);
  const [profileFieldErrors, setProfileFieldErrors] = useState<{
    username?: string;
    email?: string;
  }>({});
  const [newPasswordFieldError, setNewPasswordFieldError] = useState("");
  const [profileTouched, setProfileTouched] = useState<{
    username?: boolean;
    email?: boolean;
  }>({});

  function validateProfileField(field: "username" | "email", value: string): string {
    return field === "username"
      ? requiredFieldError(value, "username")
      : emailFieldError(value);
  }

  async function saveProfile(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = event.currentTarget;
    const formData = new FormData(form);
    const values = {
      username: String(formData.get("username") ?? ""),
      email: String(formData.get("email") ?? ""),
    };
    const errors = {
      username: validateProfileField("username", values.username),
      email: validateProfileField("email", values.email),
    };
    setProfileFieldErrors(errors);
    setProfileTouched({ username: true, email: true });
    if (Object.values(errors).some(Boolean)) {
      focusFirstInvalidField(form);
      return;
    }

    setProfileError("");
    setProfileSuccess("");
    setIsSavingProfile(true);
    const update = {
      username: values.username.trim(),
      email: values.email.trim(),
    };

    try {
      const updated = await requestJson<ProfileDto>("/api/users/me", {
        method: "PATCH",
        body: JSON.stringify(update),
      });
      updateUser({ username: updated.username, email: updated.email });
      setProfileSuccess("Your profile has been updated.");
      setProfileFieldErrors({});
    } catch (error) {
      setProfileError(describeError(error));
    } finally {
      setIsSavingProfile(false);
    }
  }

  async function changePassword(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    const form = event.currentTarget;
    const formData = new FormData(form);
    const newPassword = String(formData.get("newPassword") ?? "");
    const newPasswordError = requiredFieldError(newPassword, "new password");
    setNewPasswordFieldError(newPasswordError);
    if (newPasswordError) {
      focusFirstInvalidField(form);
      return;
    }

    setPasswordError("");
    setPasswordSuccess("");
    setIsSavingPassword(true);
    const passwordUpdate = {
      currentPassword: String(formData.get("currentPassword") ?? ""),
      newPassword,
    };

    try {
      const response = await requestJson<{ message: string }>("/api/users/me/update-password", {
        method: "PATCH",
        body: JSON.stringify(passwordUpdate),
      });
      setPasswordSuccess(response.message);
    } catch (error) {
      setPasswordError(describeError(error));
    } finally {
      form.reset();
      setIsSavingPassword(false);
    }
  }

  async function deleteAccount() {
    if (!window.confirm("Permanently delete your account? This action cannot be undone.")) {
      return;
    }
    setDeleteError("");
    setIsDeleting(true);
    try {
      await requestJson<void>("/api/users/me", { method: "DELETE" });
      clearSession();
      window.location.replace("/sign-in?deleted");
    } catch (error) {
      setDeleteError(describeError(error));
      setIsDeleting(false);
    }
  }

  return (
    <div className="page-container profile-page">
      <PageHeading
        eyebrow="Account settings"
        title="My Profile"
        description="Manage your personal information, password, and account."
      />
      {profile.resource.status === "loading" && <LoadingState label="Loading profile…" />}
      {profile.resource.status === "error" && (
        <ErrorState error={profile.resource.error} onRetry={profile.retry} />
      )}
      {profile.resource.status === "loaded" && (
        <div className="profile-sections">
          <section className="profile-card">
            <div className="section-heading">
              <div><p className="eyebrow">Personal information</p><h2>Profile information</h2></div>
            </div>
            {profileError && <p className="inline-message message-error" role="alert">{profileError}</p>}
            {profileSuccess && <p className="inline-message message-success" role="status">{profileSuccess}</p>}
            <form className="profile-form" noValidate onSubmit={saveProfile}>
              <label>
                <span>Username</span>
                <input
                  id="profile-username"
                  name="username"
                  type="text"
                  defaultValue={profile.resource.data.username}
                  required
                  autoComplete="username"
                  aria-invalid={Boolean(profileFieldErrors.username)}
                  aria-describedby={profileFieldErrors.username ? "profile-username-error" : undefined}
                  onBlur={(event) => {
                    setProfileTouched((current) => ({ ...current, username: true }));
                    setProfileFieldErrors((current) => ({
                      ...current,
                      username: validateProfileField("username", event.currentTarget.value),
                    }));
                  }}
                  onChange={(event) => {
                    if (profileTouched.username) {
                      setProfileFieldErrors((current) => ({
                        ...current,
                        username: validateProfileField("username", event.currentTarget.value),
                      }));
                    }
                  }}
                />
                {profileFieldErrors.username && (
                  <span className="field-error" id="profile-username-error">
                    {profileFieldErrors.username}
                  </span>
                )}
              </label>
              <label>
                <span>Email</span>
                <input
                  id="profile-email"
                  name="email"
                  type="email"
                  defaultValue={profile.resource.data.email}
                  required
                  autoComplete="email"
                  aria-invalid={Boolean(profileFieldErrors.email)}
                  aria-describedby={[
                    "profile-email-example",
                    ...(profileFieldErrors.email ? ["profile-email-error"] : []),
                  ].join(" ")}
                  onBlur={(event) => {
                    setProfileTouched((current) => ({ ...current, email: true }));
                    setProfileFieldErrors((current) => ({
                      ...current,
                      email: validateProfileField("email", event.currentTarget.value),
                    }));
                  }}
                  onChange={(event) => {
                    if (profileTouched.email) {
                      setProfileFieldErrors((current) => ({
                        ...current,
                        email: validateProfileField("email", event.currentTarget.value),
                      }));
                    }
                  }}
                />
                <span className="field-hint" id="profile-email-example">
                  Example: name@example.com
                </span>
                {profileFieldErrors.email && (
                  <span className="field-error" id="profile-email-error">
                    {profileFieldErrors.email}
                  </span>
                )}
              </label>
              <button className="button button-primary" type="submit" disabled={isSavingProfile}>
                {isSavingProfile ? "Saving…" : "Save profile"}
              </button>
            </form>
          </section>

          <section className="profile-card">
            <div className="section-heading">
              <div><p className="eyebrow">Security</p><h2>Change password</h2></div>
            </div>
            <p className="helper-text">
              Existing password accounts must provide their current password. Provider-only accounts can leave it blank.
            </p>
            {passwordError && <p className="inline-message message-error" role="alert">{passwordError}</p>}
            {passwordSuccess && <p className="inline-message message-success" role="status">{passwordSuccess}</p>}
            <form className="profile-form" noValidate onSubmit={changePassword}>
              <label>
                <span>Current password</span>
                <input name="currentPassword" type="password" autoComplete="current-password" />
              </label>
              <label>
                <span>New password</span>
                <input
                  id="profile-new-password"
                  name="newPassword"
                  type="password"
                  autoComplete="new-password"
                  required
                  aria-invalid={Boolean(newPasswordFieldError)}
                  aria-describedby={newPasswordFieldError ? "profile-new-password-error" : undefined}
                  onBlur={(event) => setNewPasswordFieldError(
                    requiredFieldError(event.currentTarget.value, "new password"),
                  )}
                  onChange={(event) => setNewPasswordFieldError(
                    requiredFieldError(event.currentTarget.value, "new password"),
                  )}
                />
                {newPasswordFieldError && (
                  <span className="field-error" id="profile-new-password-error">
                    {newPasswordFieldError}
                  </span>
                )}
              </label>
              <button className="button button-primary" type="submit" disabled={isSavingPassword}>
                {isSavingPassword ? "Updating…" : "Update password"}
              </button>
            </form>
          </section>

          <section className="profile-card danger-card">
            <div className="section-heading">
              <div><p className="eyebrow">Destructive action</p><h2>Delete account</h2></div>
            </div>
            <p>
              Deleting your account is permanent. You will be signed out immediately and your account cannot be restored.
            </p>
            {deleteError && <p className="inline-message message-error" role="alert">{deleteError}</p>}
            <button
              className="button button-danger"
              type="button"
              onClick={() => void deleteAccount()}
              disabled={isDeleting}
            >
              {isDeleting ? "Deleting account…" : "Delete my account"}
            </button>
          </section>
        </div>
      )}
    </div>
  );
}
