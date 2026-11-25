# Account, search, and visual design improvements

## Problem and approach

The application needs consistent USD salaries, useful administrator account details, friendly English validation, easier directory searches, a modern dark-first design, and recognizable working authentication-provider buttons.

Build on the existing React/TypeScript frontend and Spring Boot APIs rather than replacing working features. Preserve existing authentication, authorization, pagination, filtering, and account-editing behavior. The repository has substantial staged, unstaged, and untracked work; integrate with it and do not revert or overwrite unrelated changes.

## Confirmed scope and behavior

- Existing salary numbers already represent US dollars. Format and label them as USD without converting or rewriting stored amounts.
- Administrators need account details and the latest successful login, not a complete login history. Include registration date, roles, and linked sign-in providers.
- Dark mode is the default, with a saved light/dark switch.
- Redesign existing Teams, Players, Coaches, and administrator Users searches. Do not introduce global cross-entity search.
- Selecting several player positions means matching any selected position. No selection means all positions.
- Use polite English inline validation in registration, personal profile editing, and administrator user creation/editing. Include an email example such as `name@example.com`.
- Google, GitHub, and custom authorization-server buttons must launch real sign-in flows from both login and registration pages.

## Current implementation findings

- `frontend\src\features\core\pages\CorePages.tsx` contains sports directories, detail pages, and personal profile forms. Salaries currently use bare `toLocaleString()` values.
- `frontend\src\features\core\pages\AdminSports.tsx` contains player/coach salary inputs; these also need explicit USD labels.
- Player search already sends repeated `positions` parameters through a native multiple select. `PlayerSearchFilter` and `PlayerSpecification` already implement list filtering with any-position matching and distinct results. Improve the selector's discoverability, not the underlying semantics.
- `UserFullDto` already supplies roles, registration time, and last login for administrator search. However, the administrator detail endpoint and frontend detail page use the short DTO with only ID, username, and email. Unfiltered administrator lists also use short DTOs.
- `LoginSuccessListener` already records successful authentication for `CustomUserDetails`; local, OAuth2, and OIDC principals use this wrapper. Verify all real authentication flows update the timestamp.
- External identity records already exist, with provider identifiers and subjects. OIDC provider identifiers may be issuer URIs, so public labels must be mapped deliberately rather than exposing raw identity records.
- Login already links to `/oauth2/authorization/google`, `/oauth2/authorization/github`, and `/oauth2/authorization/my-custom-auth`, but has no provider icons. Registration has no provider buttons.
- Current styling is light-only, with hardcoded colors in global, core-page, and authentication CSS.
- Forms currently rely on browser constraint-validation popups. Backend email annotations do not consistently specify English messages, and validation responses combine errors into a single string.
- The frontend has type-check/build scripts but no automated test runner. Backend controller/service/repository tests already exist.

## Implementation todos

### 1. Establish a shared dark-first visual foundation

Primary files: `frontend\src\styles\global.css`, `frontend\src\features\core\pages\CorePages.css`, `frontend\src\components\AuthShell.module.css`, `frontend\src\features\auth\pages\AuthPages.module.css`, `frontend\src\main.tsx`, `frontend\index.html`.

- Introduce semantic CSS variables for backgrounds, surfaces, borders, primary/secondary text, accents, focus indicators, and feedback states.
- Replace hardcoded theme-dependent colors across directories, details, tables, dialogs, navigation, auth pages, inputs, and error/loading/empty states.
- Add a small shared theme state/control, available in `AuthenticatedLayout` and `AuthShell`. Save explicit selection in local storage and use dark on first visit.
- Apply the initial theme before React renders to avoid a light flash; update browser `color-scheme` and theme metadata. If preference storage is unavailable, keep the switch usable for the current page lifecycle.
- Use consistent spacing, typography, rounded surfaces, subtle elevation, and restrained accent colors. Preserve responsive navigation and honor reduced-motion preferences.
- Ensure readable contrast, visible keyboard focus, and comfortable mobile layouts in both themes.

### 2. Make USD salaries consistent everywhere

Primary files: `CorePages.tsx`, `AdminSports.tsx`, and a small shared formatting utility under `frontend\src`.

- Add one reusable `Intl.NumberFormat` USD formatter; use unambiguous dollar formatting such as `$1,250.00` and explicit USD labels.
- Apply it to player and coach list columns and detail/profile salary fields.
- Label salary ranges, create/edit salary fields, and salary sort choices consistently as USD where useful.
- Keep input values and API request values numeric, preserve cent precision and existing positivity constraints, and do not send formatted currency strings to the server.
- Do not add a currency column, exchange-rate service, or data migration.

### 3. Expose useful administrator account details

Primary backend files: `AdminUserController`, `UserService`, `UserServiceImpl`, user DTOs/mappers, and `ExternalIdentityRepository`.

Primary frontend files: `frontend\src\api\adminUsers.ts`, `frontend\src\features\admin-users\pages\AdminUsers.tsx`.

- Extend the administrator-only user detail contract additively to provide ID, username, email, roles, registration timestamp, nullable last-login timestamp, and linked-provider display identifiers.
- Reuse existing full-account mapping where possible; use a dedicated detail DTO if needed rather than adding provider lookups to each search-result row.
- Read linked providers in the service transaction and map known issuer/provider values to Google, GitHub, or Custom authorization server. Do not expose subjects, tokens, credentials, or raw authentication claims.
- Preserve the short DTO contracts for personal `/me`, creation, and update responses unless a directly required change is identified.
- Keep the user list consistent before and after filtering by using the existing full search endpoint even with no filters, avoiding unrelated changes to the short list endpoint.
- Render account information and access/activity sections in the administrator detail page. Use `Never signed in` when last login is null and distinguish no linked external providers from an unavailable request.
- After editing username/email, refetch detailed data rather than replacing it with the short mutation response.
- Retain administrator access checks and existing self-delete protections; regular users must not gain access to other accounts' metadata.
- Test that successful local, Google/OIDC, GitHub/OAuth2, and custom OIDC authentication update last login and failed login does not. Fix the existing listener only if those tests reveal a gap.
- Keep timestamp interpretation consistent with the backend's current `LocalDateTime` contract. Do not silently interpret timezone-free timestamps as UTC or invent historical offsets; label the existing server-local time basis where needed.

### 4. Replace browser validation popups with polite English feedback

Primary files: `SignInPage.tsx`, `SignUpPage.tsx`, profile forms in `CorePages.tsx`, user dialogs in `AdminUsers.tsx`, shared form validation helpers/components as needed, and backend request DTOs.

- Use application-controlled validation with `noValidate` on affected forms while preserving meaningful input types, autocomplete, and accessible required-field semantics.
- Implement explicit checks for every existing required constraint on those forms so disabling browser validation cannot allow blank required fields through. Keep login's username-or-email field valid for either identifier; do not require email syntax for usernames.
- Validate on submit and, for touched fields, on blur/change. Focus the first invalid field, connect hints/errors with `aria-describedby`, and mark invalid fields with `aria-invalid`.
- Explain email errors kindly and specifically: required address versus malformed address, with guidance such as `Please enter an email address like name@example.com.`
- Show a persistent example near email inputs. Trim email and username input without changing password content.
- Preserve existing password rules; do not invent new complexity requirements.
- Use reusable frontend checks for shared fields, with the backend remaining authoritative. Keep ordinary required, email-format, and length expectations aligned with the actual backend constraints.
- Specify explicit English validation messages in `RegisterRequest`, `UserCreationRequest`, and `UserUpdateRequest`; preserve optional PATCH behavior while rejecting invalid supplied email values.
- Preserve and display server uniqueness, permission, CSRF, and network failures; do not hide them behind a generic success-shaped fallback.
- Retain the current API error shape unless implementation demonstrates a concrete need for field-level server metadata. Routine format errors should already be attached to fields client-side.

### 5. Organize directory filters and improve position selection

Primary files: shared list/filter rendering in `CorePages.tsx`, administrator search in `AdminUsers.tsx`, and `CorePages.css`.

- Group related filters into labeled sections: identity/team, player positions, performance/experience, USD salary range, and administrator roles/account activity.
- Place minimum/maximum fields together with clear From/To labels and unit hints. Separate filter criteria from sorting and pagination controls.
- Add a clear/reset action and a compact summary of applied filters, including removable position selections.
- Replace the native player multiple select with an accessible checkbox group styled as selectable chips. Show short position codes plus understandable full names and explain `Matches any selected position`.
- Keep repeated `positions=PG&positions=SG` query parameters, correct checked state after reload/back/forward, distinct results, and page reset when applying or clearing criteria.
- Preserve other selected filters when removing one applied filter. Clear filter criteria without unexpectedly resetting sort and page-size choices; return to page zero.
- Validate inverted ranges before sending a request, show useful inline errors, and retain loading/error/empty states.
- Ensure controls remain comfortable on narrow screens and fully usable without Ctrl/Cmd-click or a mouse.
- Reuse the existing filtering APIs; change backend position logic only if targeted tests expose a defect.

### 6. Share working authentication-provider buttons

Primary files: `SignInPage.tsx`, `SignUpPage.tsx`, `AuthPages.module.css`, and a small shared authentication-provider button component.

- Extract the provider definitions and button rendering already used by login.
- Add recognizable Google and GitHub icons plus a neutral shield/key/server icon for the custom authorization server, with readable provider labels.
- Use locally bundled inline SVGs or existing assets; avoid external icon fetching and avoid adding a large icon dependency for three buttons.
- Render the same component on login and registration, with appropriate accessible group labels and consistent focus/hover behavior in both themes.
- Preserve the existing same-origin OAuth authorization URLs and backend session/CSRF/account-linking behavior. Registration through a provider uses the existing first-login account-creation flow, not a separate password registration request.
- Retain generic authentication-failure messaging so identity/provider internals are not exposed.
- Confirm actual provider credentials, registered callback URLs, custom server endpoints, and trusted issuer settings are available for live verification. Missing configuration is a release blocker for that provider, not a reason to replace the link with a placeholder.

### 7. Validate behavior and update directly related documentation

- Add/extend backend controller and service tests for administrator details, linked-provider mapping, null last login, nonexistent accounts, denied access for non-admins, and preservation of detailed frontend data after edits through the UI.
- Cover last-login recording for successful authentication across principal variants and no update on failure.
- Extend player controller/repository tests for no positions, one position, multiple positions with any-match semantics, duplicate-free paginated results, combined filters, and existing invalid-position behavior.
- Cover English backend validation messages and invalid supplied PATCH email values.
- Use the smallest relevant Maven selectors first, for example `mvn -q "-Dtest=AdminUserControllerIntegrationTest,UserServiceImplTest,UserServiceIntegrationTest,AuthControllerIntegrationTest,PlayerControllerIntegrationTest,PlayerRepositoryIntegrationTest" test`, adding any new targeted test classes. Integration tests require Docker/PostgreSQL through the existing Testcontainers setup.
- Run `npm run typecheck` and `npm run build` in `frontend`.
- Since no frontend test runner is configured, perform browser acceptance checks for USD consistency, validation without native popups even with a Russian browser locale, email examples/error focus, grouped ranges/reset behavior, multi-position persistence, administrator details, theme persistence/no initial flash, responsive layouts, keyboard operation, and both-theme feedback states. Add tooling only if automated browser coverage is explicitly introduced as a separate agreed requirement.
- Verify each provider's full redirect/callback, account creation or reuse, session establishment, updated last-login value, logout, and failure handling with configured real providers. Synthetic callback tests do not establish that the external provider is correctly configured.
- Update `README.md` and relevant existing frontend documentation for USD semantics, theme behavior, any-position search, administrator detail fields, validation UX, and any provider setup changes. Do not duplicate the existing OAuth setup documentation unnecessarily.
- Review the final diff for unrelated changes and confirm no credentials, generated builds, or temporary artifacts were added.

## Dependencies and execution order

- Theme foundation precedes the final filter, validation, account-detail, and auth-button presentation so they share the same tokens.
- USD formatting, administrator data contracts, validation logic, and provider-button extraction can otherwise be developed independently.
- Filter implementation reuses the USD formatter for currency-related presentation.
- Final validation/documentation depends on all implementation workstreams.

## Acceptance criteria

1. All player/coach salary displays and all salary filter/create/edit surfaces unambiguously use USD; stored amounts remain unchanged.
2. Administrators can open an account and see registration, latest successful login or `Never signed in`, roles, and linked providers, including after editing the account.
3. Invalid email input produces polite English inline guidance with an example, not a browser-localized popup; valid submissions and authoritative server errors still work.
4. Existing searches are logically grouped and responsive. Several player positions can be chosen without modifier keys, matching any selection and persisting through URL navigation.
5. Dark mode is the first-visit default; the saved switch works across authenticated and authentication pages, and both themes remain readable.
6. Google, GitHub, and custom-server icon buttons appear on login and registration and complete the existing authentication flows when provider infrastructure is configured.
7. Existing authorization, CSRF protection, account-linking safeguards, pagination, and unrelated worktree changes are preserved.

## Out of scope and operational considerations

- No full login history, activity tracking, global search, currency conversion, extra currencies, provider unlinking, role-management feature, or authorization-server implementation.
- No schema migration is expected: salary, registration/last-login, roles, and external identities already exist.
- Live provider verification needs valid environment configuration and access to the actual providers/custom server. Do not claim a provider works end-to-end when configuration or infrastructure prevents checking it.
- If existing salary values, identity records, or timestamps contradict the confirmed assumptions during implementation, stop and clarify rather than silently rewriting data.
