import { requestJson } from "./client";
import type {
  CoachDto,
  MessageDto,
  PlayerDto,
  TeamDto,
} from "./sports";

export interface RequestTeamDto {
  name: string;
  championshipCount: number;
  creationYear: number;
}

export interface PatchTeamRequest {
  teamName?: string;
  championshipCount?: number;
  creationYear?: number;
}

export interface RequestPlayerDto {
  firstName: string;
  lastName: string;
  salary: number;
  teamId?: number;
  playerPositions: string[];
  rating: number;
  championshipsWon: number;
}

export interface PatchPlayerRequest {
  firstName?: string;
  lastName?: string;
  salary?: number;
  playerPositions?: string[];
  rating?: number;
  championshipsWon?: number;
}

export interface RequestCoachDto {
  firstName: string;
  lastName: string;
  salary: number;
  teamId?: number;
  yearsOfExperience: number;
  championshipsWon: number;
}

export interface PatchCoachRequest {
  firstName?: string;
  lastName?: string;
  salary?: number;
  yearsOfExperience?: number;
  championshipsWon?: number;
}

export interface TeamTransferDto {
  message: string;
  memberId: number;
  memberFullName: string;
  memberRole: string;
  oldTeamName?: string;
  newTeamId?: number;
  newTeamName?: string;
}

export function createTeam(body: RequestTeamDto): Promise<TeamDto> {
  return requestJson<TeamDto>("/api/admin/teams", {
    method: "POST",
    body: JSON.stringify(body),
  });
}

export function updateTeam(id: number, body: PatchTeamRequest): Promise<TeamDto> {
  return requestJson<TeamDto>(`/api/admin/teams/${encodeURIComponent(id)}`, {
    method: "PATCH",
    body: JSON.stringify(body),
  });
}

export function deleteTeam(id: number): Promise<MessageDto> {
  return requestJson<MessageDto>(`/api/admin/teams/${encodeURIComponent(id)}`, {
    method: "DELETE",
  });
}

export function createPlayer(body: RequestPlayerDto): Promise<PlayerDto> {
  return requestJson<PlayerDto>("/api/admin/players", {
    method: "POST",
    body: JSON.stringify(body),
  });
}

export function updatePlayer(id: number, body: PatchPlayerRequest): Promise<PlayerDto> {
  return requestJson<PlayerDto>(`/api/admin/players/${encodeURIComponent(id)}`, {
    method: "PATCH",
    body: JSON.stringify(body),
  });
}

export function deletePlayer(id: number): Promise<MessageDto> {
  return requestJson<MessageDto>(`/api/admin/players/${encodeURIComponent(id)}`, {
    method: "DELETE",
  });
}

export function changePlayerTeam(id: number, newTeamId?: number): Promise<TeamTransferDto> {
  const query = new URLSearchParams();
  if (newTeamId !== undefined) {
    query.set("newTeamId", String(newTeamId));
  }
  const suffix = query.size > 0 ? `?${query.toString()}` : "";
  return requestJson<TeamTransferDto>(
    `/api/admin/players/${encodeURIComponent(id)}/change-team${suffix}`,
    { method: "PATCH" },
  );
}

export function createCoach(body: RequestCoachDto): Promise<CoachDto> {
  return requestJson<CoachDto>("/api/admin/coaches", {
    method: "POST",
    body: JSON.stringify(body),
  });
}

export function updateCoach(id: number, body: PatchCoachRequest): Promise<CoachDto> {
  return requestJson<CoachDto>(`/api/admin/coaches/${encodeURIComponent(id)}`, {
    method: "PATCH",
    body: JSON.stringify(body),
  });
}

export function deleteCoach(id: number): Promise<MessageDto> {
  return requestJson<MessageDto>(`/api/admin/coaches/${encodeURIComponent(id)}`, {
    method: "DELETE",
  });
}

export function changeCoachTeam(id: number, newTeamId?: number): Promise<TeamTransferDto> {
  const query = new URLSearchParams();
  if (newTeamId !== undefined) {
    query.set("newTeamId", String(newTeamId));
  }
  const suffix = query.size > 0 ? `?${query.toString()}` : "";
  return requestJson<TeamTransferDto>(
    `/api/admin/coaches/${encodeURIComponent(id)}/change-team${suffix}`,
    { method: "PATCH" },
  );
}

export function addPlayerToTeam(teamId: number, playerId: number): Promise<TeamTransferDto> {
  return requestJson<TeamTransferDto>(
    `/api/admin/teams/${encodeURIComponent(teamId)}/players/${encodeURIComponent(playerId)}`,
    { method: "POST" },
  );
}

export function removePlayerFromTeam(teamId: number, playerId: number): Promise<TeamTransferDto> {
  return requestJson<TeamTransferDto>(
    `/api/admin/teams/${encodeURIComponent(teamId)}/players/${encodeURIComponent(playerId)}`,
    { method: "DELETE" },
  );
}

export function addCoachToTeam(teamId: number, coachId: number): Promise<TeamTransferDto> {
  return requestJson<TeamTransferDto>(
    `/api/admin/teams/${encodeURIComponent(teamId)}/coaches/${encodeURIComponent(coachId)}`,
    { method: "POST" },
  );
}

export function removeCoachFromTeam(teamId: number, coachId: number): Promise<TeamTransferDto> {
  return requestJson<TeamTransferDto>(
    `/api/admin/teams/${encodeURIComponent(teamId)}/coaches/${encodeURIComponent(coachId)}`,
    { method: "DELETE" },
  );
}

export function removeAllTeamMembers(teamId: number): Promise<MessageDto> {
  return requestJson<MessageDto>(
    `/api/admin/teams/${encodeURIComponent(teamId)}/members`,
    { method: "DELETE" },
  );
}
