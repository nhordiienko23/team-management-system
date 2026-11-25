# Team Management frontend

## Requirements

- Node.js 20 or newer
- The Spring Boot backend running at `http://localhost:8080`

## Install and run

From this directory:

```sh
npm install
npm run dev
```

Open `http://localhost:5173`. The development server proxies backend API,
form login, OAuth authorization/callback, logout, Swagger UI, API docs and
error paths without proxying frontend routes.

Run `npm run typecheck` to check TypeScript and `npm run build` to create the
production bundle in `dist/`.

For deployment, serve `dist/` from the same HTTPS public origin as Spring.
Configure the host to send `/api/**`, `/login`, `/login/oauth2/**`,
`/oauth2/authorization/**`, `/logout`, `/swagger-ui.html`, `/swagger-ui/**`,
`/v3/api-docs/**`, and `/error` to Spring before falling back to the SPA entry
point for frontend routes. Keep missing static assets as 404s.

Google, GitHub and `my-custom-auth` provider credentials and callback settings
remain server-side in Spring configuration. No OAuth secrets belong in this
application.

CSRF protection, same-origin token handling, and protected logout were
implemented in Phase 1B. Set `SESSION_COOKIE_SECURE=true` for HTTPS
production. The current cookie is HttpOnly and SameSite=Lax. The trusted
proxy must overwrite forwarded headers, and Spring must only honor them from
that proxy. Production proxy and OAuth infrastructure are not included here;
see the root README for required provider, database and deployment
configuration.
