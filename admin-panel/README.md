# ChikChak Admin Panel

Minimal internal admin web panel for the ChikChak backend. Vite + React + Material UI,
no state-management library — just hooks. Talks to the Spring Boot backend's
`/api/admin/**` endpoints (requires an `ADMIN` account).

## Features

- **Login** — JWT auth against `POST /api/auth/login`; only accounts with role `ADMIN`
  can enter. Token is stored in `localStorage`, and any `401` logs you out automatically.
- **Pending Approval** — technicians awaiting verification, with Approve / Reject (+ reason).
- **All Technicians** — paginated list with verified filter, search, and a detail view.
- **Users** — paginated list with role/status filters and search; Suspend (+ reason) / Reactivate.
- **Bookings** — paginated list with status filter and search; Force-cancel (+ reason)
  and Override status (full status dropdown).
- **Audit Log** — read-only trail of admin actions (who, what, when, target, details).

Destructive actions (reject, suspend, force-cancel, override) ask for confirmation first.

## Setup

Requires Node 18+ (tested on Node 20).

```bash
cd admin-panel
npm install
cp .env.example .env        # then edit .env if your backend isn't on localhost:8080
npm run dev                 # opens on http://localhost:5173
```

### Configuration

One environment variable:

| Variable | Meaning | Example |
| --- | --- | --- |
| `VITE_API_BASE_URL` | Backend base URL, no trailing slash | `http://localhost:8080` (dev) / `https://your-app.up.railway.app` (prod) |

If unset, it falls back to `http://localhost:8080`. Note: Vite env vars are baked in
at **build time** — changing the value requires a rebuild/redeploy.

### CORS (important)

The backend only accepts browser requests from origins listed in its `ALLOWED_ORIGINS`
environment variable (comma-separated; defaults to `http://localhost:3000`). For the
panel to work you must add its origin there:

- Local dev: add `http://localhost:5173`
- Production: add your deployed URL, e.g. `https://chikchak-admin.vercel.app`

On Railway this is just an env var change on the backend service — no code change needed.

## Build & deploy

```bash
npm run build               # outputs static files to dist/
```

### Vercel

1. Import the repo, set the project **Root Directory** to `admin-panel`.
2. Framework preset: Vite (build command `npm run build`, output `dist`) — auto-detected.
3. Add the `VITE_API_BASE_URL` env var (your Railway URL) in Project Settings → Environment Variables.
4. Deploy. SPA routing is handled by the included `vercel.json`.

### Netlify

1. New site from Git, set **Base directory** to `admin-panel`.
2. Build command `npm run build`, publish directory `admin-panel/dist`.
3. Add `VITE_API_BASE_URL` under Site settings → Environment variables.
4. Deploy. SPA routing is handled by the included `public/_redirects`.

After deploying, remember to add the deployed origin to the backend's `ALLOWED_ORIGINS` (see CORS above).

## Project structure

```
src/
  api/client.js        fetch wrapper: base URL, Bearer token, ApiResponse unwrap, 401 handling
  auth.jsx             AuthProvider: login/logout, token + user in localStorage
  toast.jsx            app-wide success/error snackbar
  format.js            date/money formatting, status colors, enum lists
  useList.js           shared list-loading hook (items/loading/error/reload)
  useDetail.js         fetch-on-open state for detail dialogs
  components/          Layout (sidebar + app bar), ConfirmDialog, DetailDialog, Pager, ...
  pages/               one file per screen
```

Note: list endpoints return plain arrays (no total count), so tables use simple
Previous/Next paging — Next is disabled when a page comes back shorter than the page size.
