# Tiny Leaps

Ad-free games for kids, plus a toys & games marketplace for parents.

```
TinyLeapProject/
├── mobile/                      Flutter app (Android + iOS)
├── backend/                     Spring Boot 3.5 / Java 21 multi-module (Maven)
│   ├── common/                  Response envelope, log format, JWT, servlet filters
│   ├── gateway/                 Spring Cloud Gateway: single entry point + token validation   :8080
│   ├── auth-service/            Guest / Google / Apple auth, refresh rotation, activities     :8083
│   ├── catalogue-service/       Virtual games catalogue (paginated)                           :8084
│   └── games/
│       ├── puzzle-game-service/   tl-puzzle-01  Picture Puzzles (Animals, Alphabets, Cars)    :8091
│       ├── painting-game-service/ tl-paint-01   Paint & Play (Houses, Animals, Cars)          :8092
│       └── shared/art.js          Source of the SVG picture library (copied into each game)
├── branding/                    Logo source (generate_logo.py) + rendered PNGs
├── scripts/                     start-backend.sh, stop-backend.sh, rasterize-svg.mjs
└── logs/                        One log file per micro service (created at runtime)
```

## Run it locally

Prerequisites: Java 21, Maven, Flutter 3.47+, Postgres on `localhost:5432` (`postgres`/`postgres`).

```bash
# 1. Databases (once)
createdb -h localhost -U postgres tinyleaps_auth_db
createdb -h localhost -U postgres tinyleaps_catalogue_db

# 2. Backend: builds and starts all 5 services; Flyway creates tables and seeds the catalogue
./scripts/start-backend.sh          # --no-build to skip mvn
./scripts/stop-backend.sh

# 3. App (Android emulator reaches the host at 10.0.2.2:8080 by default)
cd mobile
flutter run
# physical device / custom host:
flutter run --dart-define=API_BASE_URL=http://192.168.1.20:8080
```

## How it fits together

```
 Flutter app ──► Gateway :8080 ──► auth-service / catalogue-service / game services
                   │
                   ├─ public:  POST /api/v1/auth/{guest,refresh,google,apple},  GET /games/{id}/assets/**
                   ├─ all other routes: Bearer JWT verified at the gateway; it sets trusted
                   │  X-User-UUID / X-Account-Type headers and an X-Gateway-Key for downstream
                   └─ GET /games/{gameId}: gateway also calls auth-service /validate with the
                      WebView's token + UUID before the game is served (records GAME_LAUNCH)
```

Services reject any request without the gateway key (`403`), so they can trust `X-User-UUID`.

### App flow
Splash → first launch? → 3 onboarding slides → sign-in (Apple on iOS, Google, **Skip for now**) → Home.
Returning users: splash checks the stored session, refreshes it or recovers the guest, then opens Home.
Home has a bottom bar with **Games** (default) and **Toys & Games** (marketplace placeholder).

### Auth flow (auth-service)
| Situation | What happens |
|---|---|
| First visit / Skip | `POST /auth/guest {deviceId}`: creates a GUEST user + GUEST_DEVICE, issues tokens |
| Access token expired | `POST /auth/refresh`: refresh token rotated (old one revoked) |
| Revoked refresh token replayed | All of that user's refresh tokens are revoked (theft protection) |
| Refresh invalid, guest | App calls `/auth/guest` again: same device → **guest identity recovered**, else a new guest |
| Refresh invalid, registered | App sends the user to sign in |
| Google / Apple sign-in | ID token verified against Google/Apple JWKS. Existing account → sign in; otherwise the device's guest is **upgraded** (same UUID, history kept) |

Access token: HS256 JWT, 15 min. Refresh token: opaque, 30 days, stored only as SHA-256.
Tables: `users`, `refresh_tokens`, `guest_devices`, `user_activities`.

### Games
- Catalogue: `GET /api/v1/catalogue/games?page=&size=&category=&age=`, rendered as a grid.
- Tapping a game opens a WebView at `{API_BASE_URL}/games/{gameId}`, with the `Authorization` and `X-User-UUID` headers on the request.
- The WebView only allows URLs under that game's own path. Anything else (ads, external links) is blocked.
- The entry page is `no-store`, so every launch is re-validated. Assets are `Cache-Control: max-age=30d` and the pictures are inline SVG, so play is instant and works without further network calls.
- Games call `TinyLeaps.postMessage('{"type":"exit"}')` to close. The app injects `window.TinyLeapsSession` (user UUID only; tokens are never exposed to page JS).

**Adding a game:** create a module under `backend/games/` that serves `/games/{newId}`, add `tinyleaps.gateway.games.{newId}: http://host:port` to the gateway config, and insert a row into `virtual_games`.

## API conventions

Every response:
```json
{ "statusCode": 200, "customerMessage": "Let's play!", "description": "Games catalogue",
  "data": [ ... ], "pagination": { "page": 0, "size": 20, "totalElements": 2, "totalPages": 1, "hasNext": false, "hasPrevious": false } }
```
`pagination` appears only on paginated endpoints. Errors use the same envelope.

Every log line (console + `logs/<service>.log`, rolled daily):
```
time stamp=2026-11-12T16:41:44.921+03:00 | Severity=INFO | UserUUID=006a6dca-... | Micro Service Name=auth-service | ProcessName=GUEST_AUTH | LogMessage=HTTP exchange completed | Request=POST /api/v1/auth/guest body={...} | Response=status=200 body={...,"accessToken":"***"} | TAT=124ms |
```
Tokens, ID tokens and passwords are masked automatically.

## Configuration to finish

| What | Where |
|---|---|
| **Google Sign-In** | Create OAuth clients in Google Cloud (Android client with your SHA-1 + package `com.tinyleaps.app`, plus a Web client). Run the app with `--dart-define=GOOGLE_SERVER_CLIENT_ID=<web client id>` and start auth-service with `TL_GOOGLE_CLIENT_IDS=<web client id>`. Until then the backend answers 503 "Google sign-in is not available right now". |
| **Apple Sign-In** (iOS) | Enable the capability for bundle id `com.tinyleaps.app`; `TL_APPLE_CLIENT_IDS` defaults to it. For Google on iOS also pass `GOOGLE_IOS_CLIENT_ID` and add the reversed client id URL scheme to `Info.plist`. |
| Secrets | `TL_JWT_SECRET` (gateway + auth, ≥32 bytes) and `TL_GATEWAY_KEY` (all services). The defaults are dev-only. |
| HTTPS | Release Android builds only allow cleartext to `10.0.2.2`/`localhost`; production needs HTTPS. |

## Branding

`branding/generate_logo.py` produces the logo (a baby frog mid-leap). To re-render the PNGs:
`node scripts/rasterize-svg.mjs branding/logo.svg branding/logo_1024.png`. Then copy them into
`mobile/assets/branding/` and run `dart run flutter_launcher_icons && dart run flutter_native_splash:create` in `mobile/`.
Font: Fredoka (SIL OFL, bundled in `mobile/assets/fonts/`).
# new-tiny-leaps
