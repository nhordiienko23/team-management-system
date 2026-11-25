import { requestJson } from "./client";

export interface PageResponse<T> {
  content: T[];
  totalPages: number;
  totalElements: number;
  number: number;
  size: number;
  first: boolean;
  last: boolean;
  numberOfElements: number;
  empty: boolean;
}

export interface MemberShort {
  id: number;
  fullName: string;
}

export interface TeamDto {
  id: number;
  name: string;
  championshipTitleCount: number;
  creationYear: number;
  coaches: MemberShort[];
  players: MemberShort[];
}

export interface PlayerDto {
  id: number;
  firstName: string;
  lastName: string;
  teamRole: string;
  positions: string[];
  team: string | null;
  rating: number;
  championshipWon: number;
  salary: number;
}

export interface CoachDto {
  id: number;
  firstName: string;
  lastName: string;
  teamRole: string;
  teamName: string | null;
  yearsExperience: number;
  championshipWon: number;
  salary: number;
}

export interface TeamGroup {
  teamName: string;
  title: string;
  members: MemberShort[];
}

export interface ProfileDto {
  id: number;
  username: string;
  email: string;
}

export interface MessageDto {
  message: string;
}

export function getPage<T>(
  path: string,
  params: URLSearchParams,
  signal: AbortSignal,
): Promise<PageResponse<T>> {
  const query = params.toString();
  return requestJson<PageResponse<T>>(`${path}${query ? `?${query}` : ""}`, {
    signal,
  });
}

export function getTeam(id: string, signal?: AbortSignal): Promise<TeamDto> {
  return requestJson<TeamDto>(`/api/teams/${encodeURIComponent(id)}`, { signal });
}

export function getTeamLineup(id: string, signal?: AbortSignal): Promise<TeamGroup> {
  return requestJson<TeamGroup>(`/api/teams/${encodeURIComponent(id)}/team-lineup`, { signal });
}

export function getCoachingStaff(id: string, signal?: AbortSignal): Promise<TeamGroup> {
  return requestJson<TeamGroup>(`/api/teams/${encodeURIComponent(id)}/coaching-staff`, { signal });
}

export function getPlayer(id: string, signal?: AbortSignal): Promise<PlayerDto> {
  return requestJson<PlayerDto>(`/api/players/${encodeURIComponent(id)}`, { signal });
}

export function getTeammates(id: string, signal?: AbortSignal): Promise<TeamGroup> {
  return requestJson<TeamGroup>(`/api/players/${encodeURIComponent(id)}/teammates`, { signal });
}

export function getCoach(id: string, signal?: AbortSignal): Promise<CoachDto> {
  return requestJson<CoachDto>(`/api/coaches/${encodeURIComponent(id)}`, { signal });
}

export function getColleagues(id: string, signal?: AbortSignal): Promise<TeamGroup> {
  return requestJson<TeamGroup>(`/api/coaches/${encodeURIComponent(id)}/colleagues`, { signal });
}

export function getProfile(signal?: AbortSignal): Promise<ProfileDto> {
  return requestJson<ProfileDto>("/api/users/me", { signal });
}
