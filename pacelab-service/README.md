# Pacelab-service

This is a service for workout analyzing and AI-coaching.

It is also a showcase and learning project for learning better Hexagonal Architecture and an event driven approach.


## Examples

Query workouts:
```
GET /workouts?from=2026-09-01T00:00:00Z&to=2026-10-01T00:00:00Z&enduranceType=RUNNING&limit=50
```

## Strava testing UI

The temporary server-rendered UI at `http://localhost:8080/` connects one Strava account for the current browser session and manually imports supported activities. Configure a Strava application with `localhost` as its Authorization Callback Domain.

Environment variables:

| Variable | Required | Default | Description |
| --- | --- | --- | --- |
| `STRAVA_CLIENT_ID` | Yes | - | Strava application client ID. |
| `STRAVA_CLIENT_SECRET` | Yes | - | Strava application client secret. |
| `STRAVA_REDIRECT_URI` | No | `http://localhost:8080/strava/callback` | OAuth callback registered with Strava. |

Start the application with:

```sh
STRAVA_CLIENT_ID=your-client-id \
STRAVA_CLIENT_SECRET=your-client-secret \
./gradlew bootRun
```

Credentials and imported workouts are intentionally in-memory and disappear on restart.
