import {
  useEffect,
  useRef,
  useState,
  type FormEvent,
  type ReactNode,
} from "react";
import {
  addCoachToTeam,
  addPlayerToTeam,
  changeCoachTeam,
  changePlayerTeam,
  createCoach,
  createPlayer,
  createTeam,
  deleteCoach,
  deletePlayer,
  deleteTeam,
  removeAllTeamMembers,
  removeCoachFromTeam,
  removePlayerFromTeam,
  updateCoach,
  updatePlayer,
  updateTeam,
} from "../../../api/adminSports";
import { ApiError } from "../../../api/client";
import {
  getPage,
  type CoachDto,
  type PageResponse,
  type PlayerDto,
  type TeamDto,
} from "../../../api/sports";
import { useSession } from "../../../app/session/SessionContext";

type EntityKind = "team" | "player" | "coach";
type SportsEntity = TeamDto | PlayerDto | CoachDto;
type MemberKind = "player" | "coach";

function useIsAdmin(): boolean {
  const { session } = useSession();
  return session.status === "authenticated" &&
    session.user.authorities.includes("ROLE_ADMIN");
}

function errorMessage(error: unknown): string {
  if (error instanceof ApiError) {
    return error.message;
  }
  return error instanceof Error ? error.message : "The operation could not be completed.";
}

export function AdminDialog({
  title,
  onClose,
  children,
}: {
  title: string;
  onClose: () => void;
  children: ReactNode;
}) {
  const dialogRef = useRef<HTMLDialogElement>(null);

  useEffect(() => {
    const dialog = dialogRef.current;
    if (dialog && !dialog.open) {
      dialog.showModal();
    }
    return () => {
      if (dialog?.open) {
        dialog.close();
      }
    };
  }, []);

  return (
    <dialog
      className="admin-dialog"
      ref={dialogRef}
      aria-labelledby="admin-dialog-title"
      onCancel={(event) => {
        event.preventDefault();
        onClose();
      }}
    >
      <div className="admin-dialog-heading">
        <h2 id="admin-dialog-title">{title}</h2>
        <button className="admin-dialog-close" type="button" onClick={onClose} aria-label="Close dialog">
          ×
        </button>
      </div>
      {children}
    </dialog>
  );
}

function AdminActionButton({
  children,
  onClick,
  danger = false,
  disabled = false,
}: {
  children: ReactNode;
  onClick: () => void;
  danger?: boolean;
  disabled?: boolean;
}) {
  return (
    <button
      className={danger ? "button button-danger" : "button button-secondary"}
      type="button"
      onClick={onClick}
      disabled={disabled}
    >
      {children}
    </button>
  );
}

function Feedback({ error, success }: { error: string; success: string }) {
  return (
    <>
      {error && <p className="inline-message message-error" role="alert">{error}</p>}
      {success && <p className="inline-message message-success" role="status">{success}</p>}
    </>
  );
}

function useTeamOptions(enabled: boolean) {
  const [teams, setTeams] = useState<TeamDto[]>([]);
  const [error, setError] = useState("");
  const [attempt, setAttempt] = useState(0);
  const [loading, setLoading] = useState(enabled);

  useEffect(() => {
    if (!enabled) {
      setLoading(false);
      return;
    }
    const controller = new AbortController();
    setLoading(true);
    async function loadTeams() {
      try {
        const allTeams: TeamDto[] = [];
        let totalPages = 1;
        for (let pageNumber = 0; pageNumber < totalPages; pageNumber += 1) {
          const params = new URLSearchParams({
            page: String(pageNumber),
            size: "100",
            sort: "name,asc",
          });
          const page = await getPage<TeamDto>("/api/teams/search", params, controller.signal);
          allTeams.push(...page.content);
          totalPages = page.totalPages;
        }
        if (controller.signal.aborted) return;
        setTeams(allTeams);
        setError("");
      } catch (requestError) {
        if (!controller.signal.aborted) {
          setError(errorMessage(requestError));
        }
      } finally {
        if (!controller.signal.aborted) setLoading(false);
      }
    }
    void loadTeams();
    return () => controller.abort();
  }, [enabled, attempt]);

  return { teams, error, loading, retry: () => setAttempt((value) => value + 1) };
}

function TeamAssignmentField({ name = "teamId" }: { name?: string }) {
  const { teams, error, retry } = useTeamOptions(true);
  return (
    <>
      <label className="admin-form-field">
        <span>Initial team <small>(optional; leave blank for free agent)</small></span>
        <select name={name} defaultValue="">
          <option value="">Free agent / no team</option>
          {teams.map((team) => <option key={team.id} value={team.id}>{team.name}</option>)}
        </select>
      </label>
      {error && (
        <p className="inline-message message-error" role="alert">
          Teams could not be loaded: {error}{" "}
          <button className="text-button" type="button" onClick={retry}>Try again</button>
        </p>
      )}
    </>
  );
}

function TeamEditor({
  entity,
  onClose,
  onSaved,
}: {
  entity?: TeamDto;
  onClose: () => void;
  onSaved: () => void;
}) {
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (saving) return;
    setSaving(true);
    setError("");
    const data = new FormData(event.currentTarget);
    const values = {
      name: String(data.get("name") ?? "").trim(),
      championshipCount: Number(data.get("championshipCount")),
      creationYear: Number(data.get("creationYear")),
    };
    if (!values.name) {
      setError("Team name is required.");
      setSaving(false);
      return;
    }
    try {
      if (entity) {
        await updateTeam(entity.id, {
          teamName: values.name,
          championshipCount: values.championshipCount,
          creationYear: values.creationYear,
        });
      } else {
        await createTeam(values);
      }
      onSaved();
      onClose();
    } catch (requestError) {
      setError(errorMessage(requestError));
    } finally {
      setSaving(false);
    }
  }

  return (
    <AdminDialog title={entity ? "Edit team" : "Create team"} onClose={onClose}>
      <form className="admin-form" onSubmit={(event) => void submit(event)}>
        <label className="admin-form-field">
          <span>Team name</span>
          <input name="name" type="text" defaultValue={entity?.name ?? ""} required />
        </label>
        <label className="admin-form-field">
          <span>Championship titles</span>
          <input name="championshipCount" type="number" min="0" step="1" defaultValue={entity?.championshipTitleCount ?? 0} required />
        </label>
        <label className="admin-form-field">
          <span>Creation year</span>
          <input name="creationYear" type="number" min="0" step="1" defaultValue={entity?.creationYear ?? new Date().getFullYear()} required />
        </label>
        <Feedback error={error} success="" />
        <div className="admin-form-actions">
          <button className="button button-secondary" type="button" onClick={onClose} disabled={saving}>Cancel</button>
          <button className="button button-primary" type="submit" disabled={saving}>
            {saving ? "Saving…" : entity ? "Save changes" : "Create team"}
          </button>
        </div>
      </form>
    </AdminDialog>
  );
}

const playerPositions = ["PG", "SG", "SF", "PF", "C"] as const;
function isPlayerPosition(value: string): value is (typeof playerPositions)[number] {
  return playerPositions.some((position) => position === value);
}

function PlayerEditor({
  entity,
  onClose,
  onSaved,
}: {
  entity?: PlayerDto;
  onClose: () => void;
  onSaved: () => void;
}) {
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (saving) return;
    setSaving(true);
    setError("");
    const data = new FormData(event.currentTarget);
    const positions = data.getAll("playerPositions").map(String);
    if (!positions.length || !positions.every(isPlayerPosition)) {
      setError("Select at least one valid player position.");
      setSaving(false);
      return;
    }
    const firstName = String(data.get("firstName") ?? "").trim();
    const lastName = String(data.get("lastName") ?? "").trim();
    const salary = Number(data.get("salary"));
    const rating = Number(data.get("rating"));
    const championshipsWon = Number(data.get("championshipsWon"));
    if (!firstName || !lastName) {
      setError("First and last names are required.");
      setSaving(false);
      return;
    }
    try {
      if (entity) {
        await updatePlayer(entity.id, {
          firstName,
          lastName,
          salary,
          playerPositions: positions,
          rating,
          championshipsWon,
        });
      } else {
        const teamId = String(data.get("teamId") ?? "");
        await createPlayer({
          firstName,
          lastName,
          salary,
          ...(teamId ? { teamId: Number(teamId) } : {}),
          playerPositions: positions,
          rating,
          championshipsWon,
        });
      }
      onSaved();
      onClose();
    } catch (requestError) {
      setError(errorMessage(requestError));
    } finally {
      setSaving(false);
    }
  }

  return (
    <AdminDialog title={entity ? "Edit player" : "Create player"} onClose={onClose}>
      <form className="admin-form" onSubmit={(event) => void submit(event)}>
        <label className="admin-form-field">
          <span>First name</span>
          <input name="firstName" type="text" defaultValue={entity?.firstName ?? ""} required />
        </label>
        <label className="admin-form-field">
          <span>Last name</span>
          <input name="lastName" type="text" defaultValue={entity?.lastName ?? ""} required />
        </label>
        <label className="admin-form-field">
          <span>Salary (USD)</span>
          <input name="salary" type="number" min="0.01" step="0.01" defaultValue={entity?.salary ?? ""} required />
        </label>
        <label className="admin-form-field">
          <span>Positions</span>
          <select name="playerPositions" multiple required defaultValue={entity?.positions ?? []}>
            {playerPositions.map((position) => <option key={position} value={position}>{position}</option>)}
          </select>
        </label>
        <label className="admin-form-field">
          <span>Rating (0–100)</span>
          <input name="rating" type="number" min="0" max="100" step="1" defaultValue={entity?.rating ?? 0} required />
        </label>
        <label className="admin-form-field">
          <span>Championships won</span>
          <input name="championshipsWon" type="number" min="0" step="1" defaultValue={entity?.championshipWon ?? 0} required />
        </label>
        {!entity && <TeamAssignmentField />}
        {entity && <p className="helper-text">Use Change team to transfer this player or make them a free agent.</p>}
        <Feedback error={error} success="" />
        <div className="admin-form-actions">
          <button className="button button-secondary" type="button" onClick={onClose} disabled={saving}>Cancel</button>
          <button className="button button-primary" type="submit" disabled={saving}>
            {saving ? "Saving…" : entity ? "Save changes" : "Create player"}
          </button>
        </div>
      </form>
    </AdminDialog>
  );
}

function CoachEditor({
  entity,
  onClose,
  onSaved,
}: {
  entity?: CoachDto;
  onClose: () => void;
  onSaved: () => void;
}) {
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (saving) return;
    setSaving(true);
    setError("");
    const data = new FormData(event.currentTarget);
    const firstName = String(data.get("firstName") ?? "").trim();
    const lastName = String(data.get("lastName") ?? "").trim();
    const salary = Number(data.get("salary"));
    const yearsOfExperience = Number(data.get("yearsOfExperience"));
    const championshipsWon = Number(data.get("championshipsWon"));
    if (!firstName || !lastName) {
      setError("First and last names are required.");
      setSaving(false);
      return;
    }
    try {
      if (entity) {
        await updateCoach(entity.id, {
          firstName,
          lastName,
          salary,
          yearsOfExperience,
          championshipsWon,
        });
      } else {
        const teamId = String(data.get("teamId") ?? "");
        await createCoach({
          firstName,
          lastName,
          salary,
          ...(teamId ? { teamId: Number(teamId) } : {}),
          yearsOfExperience,
          championshipsWon,
        });
      }
      onSaved();
      onClose();
    } catch (requestError) {
      setError(errorMessage(requestError));
    } finally {
      setSaving(false);
    }
  }

  return (
    <AdminDialog title={entity ? "Edit coach" : "Create coach"} onClose={onClose}>
      <form className="admin-form" onSubmit={(event) => void submit(event)}>
        <label className="admin-form-field">
          <span>First name</span>
          <input name="firstName" type="text" defaultValue={entity?.firstName ?? ""} required />
        </label>
        <label className="admin-form-field">
          <span>Last name</span>
          <input name="lastName" type="text" defaultValue={entity?.lastName ?? ""} required />
        </label>
        <label className="admin-form-field">
          <span>Salary (USD)</span>
          <input name="salary" type="number" min="0.01" step="0.01" defaultValue={entity?.salary ?? ""} required />
        </label>
        <label className="admin-form-field">
          <span>Years of experience</span>
          <input name="yearsOfExperience" type="number" min="0" step="1" defaultValue={entity?.yearsExperience ?? 0} required />
        </label>
        <label className="admin-form-field">
          <span>Championships won</span>
          <input name="championshipsWon" type="number" min="0" step="1" defaultValue={entity?.championshipWon ?? 0} required />
        </label>
        {!entity && <TeamAssignmentField />}
        {entity && <p className="helper-text">Use Change team to transfer this coach or make them a free agent.</p>}
        <Feedback error={error} success="" />
        <div className="admin-form-actions">
          <button className="button button-secondary" type="button" onClick={onClose} disabled={saving}>Cancel</button>
          <button className="button button-primary" type="submit" disabled={saving}>
            {saving ? "Saving…" : entity ? "Save changes" : "Create coach"}
          </button>
        </div>
      </form>
    </AdminDialog>
  );
}

function AdminEntityEditor({
  kind,
  entity,
  onClose,
  onSaved,
}: {
  kind: EntityKind;
  entity?: SportsEntity;
  onClose: () => void;
  onSaved: () => void;
}) {
  if (kind === "team") {
    return <TeamEditor entity={entity as TeamDto | undefined} onClose={onClose} onSaved={onSaved} />;
  }
  if (kind === "player") {
    return <PlayerEditor entity={entity as PlayerDto | undefined} onClose={onClose} onSaved={onSaved} />;
  }
  return <CoachEditor entity={entity as CoachDto | undefined} onClose={onClose} onSaved={onSaved} />;
}

export function AdminCreateAction({
  kind,
  onChanged,
}: {
  kind: EntityKind;
  onChanged: () => void;
}) {
  const admin = useIsAdmin();
  const [open, setOpen] = useState(false);
  if (!admin) return null;
  const labels = { team: "Create team", player: "Create player", coach: "Create coach" };

  return (
    <>
      <AdminActionButton onClick={() => setOpen(true)}>{labels[kind]}</AdminActionButton>
      {open && (
        <AdminEntityEditor
          kind={kind}
          onClose={() => setOpen(false)}
          onSaved={() => {
            setOpen(false);
            onChanged();
          }}
        />
      )}
    </>
  );
}

function TeamTransferDialog({
  kind,
  entity,
  onClose,
  onSaved,
}: {
  kind: MemberKind;
  entity: PlayerDto | CoachDto;
  onClose: () => void;
  onSaved: () => void;
}) {
  const currentTeam = kind === "player"
    ? (entity as PlayerDto).team
    : (entity as CoachDto).teamName;
  const { teams, error: teamError, loading: teamsLoading, retry } = useTeamOptions(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [newTeamId, setNewTeamId] = useState("");
  const name = `${entity.firstName} ${entity.lastName}`;
  const selectedTeam = teams.find((team) => String(team.id) === newTeamId);
  const destination = selectedTeam?.name ?? "Free agent";
  const unchanged = destination === (currentTeam ?? "Free agent");

  useEffect(() => {
    if (currentTeam && teams.length) {
      const current = teams.find((team) => team.name === currentTeam);
      if (current) setNewTeamId(String(current.id));
    }
  }, [currentTeam, teams]);

  async function submit(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (saving) return;
    if (!window.confirm(`Move ${name} from ${currentTeam ?? "Free agent"} to ${destination}?`)) {
      return;
    }
    setSaving(true);
    setError("");
    try {
      const targetId = newTeamId ? Number(newTeamId) : undefined;
      if (kind === "player") {
        await changePlayerTeam(entity.id, targetId);
      } else {
        await changeCoachTeam(entity.id, targetId);
      }
      onSaved();
      onClose();
    } catch (requestError) {
      setError(errorMessage(requestError));
    } finally {
      setSaving(false);
    }
  }

  return (
    <AdminDialog title={`Change ${kind} team`} onClose={onClose}>
      <form className="admin-form" onSubmit={(event) => void submit(event)}>
        <p className="helper-text">
          {name} currently belongs to {currentTeam ?? "no team"}. Choose a destination or leave the selection empty to make them a free agent.
        </p>
        <label className="admin-form-field">
          <span>New team</span>
          <select value={newTeamId} onChange={(event) => setNewTeamId(event.target.value)}>
            <option value="">Free agent (no team)</option>
            {teams.map((team) => (
              <option key={team.id} value={team.id}>{team.name}</option>
            ))}
          </select>
        </label>
        {teamError && (
          <p className="inline-message message-error" role="alert">
            Teams could not be loaded: {teamError}{" "}
            <button className="text-button" type="button" onClick={retry}>Try again</button>
          </p>
        )}
        <Feedback error={error} success="" />
        <div className="admin-form-actions">
          <button className="button button-secondary" type="button" onClick={onClose} disabled={saving}>Cancel</button>
          <button className="button button-primary" type="submit" disabled={saving || teamsLoading || Boolean(teamError) || unchanged}>
            {saving ? "Updating…" : "Change team"}
          </button>
        </div>
      </form>
    </AdminDialog>
  );
}

export function AdminEntityActions({
  kind,
  entity,
  onChanged,
  onDeleted,
}: {
  kind: EntityKind;
  entity: SportsEntity;
  onChanged: () => void;
  onDeleted?: () => void;
}) {
  const admin = useIsAdmin();
  const [editing, setEditing] = useState(false);
  const [transferring, setTransferring] = useState(false);
  const [deleting, setDeleting] = useState(false);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  if (!admin) return null;

  async function remove() {
    setBusy(true);
    setError("");
    setSuccess("");
    try {
      if (kind === "team") await deleteTeam(entity.id);
      else if (kind === "player") await deletePlayer(entity.id);
      else await deleteCoach(entity.id);
      if (onDeleted) onDeleted();
      else {
        setDeleting(false);
        setSuccess(`${kind[0].toUpperCase()}${kind.slice(1)} deleted successfully.`);
        onChanged();
      }
    } catch (requestError) {
      setError(errorMessage(requestError));
    } finally {
      setBusy(false);
    }
  }

  return (
    <div className="admin-actions">
      <span className="admin-badge">Administrator actions</span>
      <AdminActionButton onClick={() => setEditing(true)} disabled={busy}>Edit</AdminActionButton>
      {(kind === "player" || kind === "coach") && (
        <AdminActionButton onClick={() => setTransferring(true)} disabled={busy}>Change team</AdminActionButton>
      )}
      <AdminActionButton onClick={() => setDeleting(true)} danger disabled={busy}>
        Delete {kind}
      </AdminActionButton>
      <Feedback error={error} success={success} />
      {editing && (
        <AdminEntityEditor
          kind={kind}
          entity={entity}
          onClose={() => setEditing(false)}
          onSaved={() => {
            setEditing(false);
            onChanged();
          }}
        />
      )}
      {transferring && (kind === "player" || kind === "coach") && (
        <TeamTransferDialog
          kind={kind}
          entity={entity as PlayerDto | CoachDto}
          onClose={() => setTransferring(false)}
          onSaved={() => {
            setTransferring(false);
            onChanged();
          }}
        />
      )}
      {deleting && (
        <AdminDialog title={`Delete ${kind}?`} onClose={() => setDeleting(false)}>
          <div className="admin-form">
            <p>
              {kind === "team"
                ? "Deleting this team detaches all players and coaches; it does not delete their records."
                : `This permanently deletes the ${kind} record and may affect team membership. This cannot be undone.`}
            </p>
            <Feedback error={error} success="" />
            <div className="admin-form-actions">
              <button className="button button-secondary" type="button" onClick={() => setDeleting(false)} disabled={busy}>Cancel</button>
              <button className="button button-danger" type="button" onClick={() => void remove()} disabled={busy}>
                {busy ? "Deleting…" : `Delete ${kind}`}
              </button>
            </div>
          </div>
        </AdminDialog>
      )}
    </div>
  );
}

function AddMemberDialog({
  kind,
  team,
  onClose,
  onAdded,
}: {
  kind: MemberKind;
  team: TeamDto;
  onClose: () => void;
  onAdded: () => void;
}) {
  const [firstName, setFirstName] = useState("");
  const [lastName, setLastName] = useState("");
  const [pageNumber, setPageNumber] = useState(0);
  const [attempt, setAttempt] = useState(0);
  const [results, setResults] = useState<PageResponse<PlayerDto | CoachDto> | null>(null);
  const [selectedId, setSelectedId] = useState("");
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    const controller = new AbortController();
    const params = new URLSearchParams({
      page: String(pageNumber),
      size: "50",
      sort: "firstName,asc",
    });
    if (firstName.trim()) params.set("firstName", firstName.trim());
    if (lastName.trim()) params.set("lastName", lastName.trim());
    const endpoint = kind === "player" ? "/api/players/search" : "/api/coaches/search";
    setLoading(true);
    setError("");
    setResults(null);
    setSelectedId("");
    getPage<PlayerDto | CoachDto>(endpoint, params, controller.signal).then(
      (page) => {
        setResults(page);
        setLoading(false);
      },
      (requestError: unknown) => {
        if (!controller.signal.aborted) {
          setError(errorMessage(requestError));
          setLoading(false);
        }
      },
    );
    return () => controller.abort();
  }, [kind, firstName, lastName, pageNumber, attempt]);

  const selected = results?.content.find((member) => String(member.id) === selectedId);

  async function add(event: FormEvent<HTMLFormElement>) {
    event.preventDefault();
    if (!selected || saving) return;
    const currentTeam = kind === "player"
      ? (selected as PlayerDto).team
      : (selected as CoachDto).teamName;
    const name = `${selected.firstName} ${selected.lastName}`;
    if (currentTeam === team.name) {
      setError(`${name} is already a member of ${team.name}.`);
      return;
    }
    if (currentTeam && currentTeam !== team.name &&
        !window.confirm(`Transfer ${name} from ${currentTeam} to ${team.name}?`)) {
      return;
    }
    setSaving(true);
    setError("");
    try {
      if (kind === "player") await addPlayerToTeam(team.id, selected.id);
      else await addCoachToTeam(team.id, selected.id);
      onAdded();
      onClose();
    } catch (requestError) {
      setError(errorMessage(requestError));
    } finally {
      setSaving(false);
    }
  }

  return (
    <AdminDialog title={`Add ${kind} to ${team.name}`} onClose={onClose}>
      <form className="admin-form" onSubmit={(event) => void add(event)}>
        <p className="helper-text">
          Adding someone who already belongs to another team transfers them. Removing a member later does not delete their record.
        </p>
        <div className="admin-name-search">
          <label className="admin-form-field">
            <span>First name filter</span>
            <input value={firstName} onChange={(event) => {
              setFirstName(event.target.value);
              setPageNumber(0);
            }} />
          </label>
          <label className="admin-form-field">
            <span>Last name filter</span>
            <input value={lastName} onChange={(event) => {
              setLastName(event.target.value);
              setPageNumber(0);
            }} />
          </label>
          <button className="button button-secondary" type="button" onClick={() => setAttempt((value) => value + 1)}>
            Search
          </button>
        </div>
        <label className="admin-form-field">
          <span>Existing {kind}</span>
          <select required value={selectedId} onChange={(event) => setSelectedId(event.target.value)} disabled={loading}>
            <option value="">{loading ? "Loading…" : "Select a member"}</option>
            {results?.content.map((member) => {
              const teamName = kind === "player"
                ? (member as PlayerDto).team
                : (member as CoachDto).teamName;
              return (
                <option key={member.id} value={member.id}>
                  {member.firstName} {member.lastName} — {teamName ?? "Free agent"}
                </option>
              );
            })}
          </select>
        </label>
        {results?.empty && <p className="helper-text">No matching {kind}s found.</p>}
        {results && results.totalPages > 1 && (
          <nav className="member-picker-pagination" aria-label={`${kind} search results pages`}>
            <button
              className="button button-secondary"
              type="button"
              onClick={() => setPageNumber((value) => Math.max(0, value - 1))}
              disabled={results.first || loading}
            >
              Previous
            </button>
            <span>Page {results.number + 1} of {results.totalPages}</span>
            <button
              className="button button-secondary"
              type="button"
              onClick={() => setPageNumber((value) => value + 1)}
              disabled={results.last || loading}
            >
              Next
            </button>
          </nav>
        )}
        <Feedback error={error} success="" />
        <div className="admin-form-actions">
          <button className="button button-secondary" type="button" onClick={onClose} disabled={saving}>Cancel</button>
          <button className="button button-primary" type="submit" disabled={saving || loading || !selected}>
            {saving ? "Adding…" : `Add ${kind}`}
          </button>
        </div>
      </form>
    </AdminDialog>
  );
}

export function AdminTeamMemberRemove({
  teamId,
  member,
  kind,
  onRemoved,
}: {
  teamId: number;
  member: { id: number; fullName: string };
  kind: MemberKind;
  onRemoved: () => void;
}) {
  const admin = useIsAdmin();
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  if (!admin) return null;

  async function remove() {
    if (!window.confirm(`Remove ${member.fullName} from this team? Their record will not be deleted.`)) return;
    setBusy(true);
    setError("");
    try {
      if (kind === "player") await removePlayerFromTeam(teamId, member.id);
      else await removeCoachFromTeam(teamId, member.id);
      onRemoved();
    } catch (requestError) {
      setError(errorMessage(requestError));
    } finally {
      setBusy(false);
    }
  }

  return (
    <span className="roster-admin-action">
      <button className="text-button" type="button" onClick={() => void remove()} disabled={busy}>
        {busy ? "Removing…" : "Remove"}
      </button>
      {error && <span className="roster-error" role="alert">{error}</span>}
    </span>
  );
}

export function AdminTeamRosterActions({
  team,
  onChanged,
}: {
  team: TeamDto;
  onChanged: () => void;
}) {
  const admin = useIsAdmin();
  const [adding, setAdding] = useState<MemberKind | null>(null);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  const [success, setSuccess] = useState("");
  if (!admin) return null;

  async function removeAll() {
    if (!window.confirm(
      `Remove all players and coaches from ${team.name}? Their accounts and records will not be deleted.`,
    )) return;
    setBusy(true);
    setError("");
    setSuccess("");
    try {
      const result = await removeAllTeamMembers(team.id);
      setSuccess(result.message || "All team members were detached.");
      onChanged();
    } catch (requestError) {
      setError(errorMessage(requestError));
    } finally {
      setBusy(false);
    }
  }

  return (
    <section className="admin-roster-panel" aria-label="Team roster administration">
      <div className="admin-roster-heading">
        <div>
          <span className="admin-badge">Administrator actions</span>
          <p>Manage members without deleting their player or coach records.</p>
        </div>
        <div className="admin-actions">
          <AdminActionButton onClick={() => setAdding("player")} disabled={busy}>Add player</AdminActionButton>
          <AdminActionButton onClick={() => setAdding("coach")} disabled={busy}>Add coach</AdminActionButton>
          <AdminActionButton onClick={() => void removeAll()} danger disabled={busy}>
            {busy ? "Removing…" : "Remove all members"}
          </AdminActionButton>
        </div>
      </div>
      <Feedback error={error} success={success} />
      {adding && (
        <AddMemberDialog
          kind={adding}
          team={team}
          onClose={() => setAdding(null)}
          onAdded={() => {
            setAdding(null);
            setSuccess(`${adding === "player" ? "Player" : "Coach"} added to ${team.name}.`);
            onChanged();
          }}
        />
      )}
    </section>
  );
}
