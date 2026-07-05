# Room Schema (approved 2026-07-05 at M0 sign-off)

Nothing below is implemented yet. Tables ship with the milestone that first
needs them (via Room migrations), but the whole shape is agreed now so nothing
gets painted into a corner.

## Design decisions (the "why")

1. **One event table for urges AND lapses.** A lapse is not a separate
   phenomenon — it's an urge episode with a different outcome. Modeling it
   as `urge_event.outcome = LAPSED` (+ an optional 1:1 debrief row) means the
   trigger log, trend lines, and "recovery speed" queries read ONE table.
   This is the shame-free principle expressed in the data model: the same
   table row shape whether you surfed or slipped.

2. **Triggers are a taxonomy table + junction, not free text.** The adaptive
   engine (the differentiator) needs to count "loneliness at 11pm" across
   events. Free text can't aggregate; enum columns can't grow. A `trigger`
   table seeded with a curated set + user-added rows, joined via
   `event_trigger`, gives both. Same taxonomy feeds urges, lapses, and
   if-then plan situations.

3. **Enums stored as TEXT, not INT.** Survives reordering, readable in the
   user's own data export (M6), negligible size cost at this scale.

4. **Timestamps = epoch millis + the event's local hour/day denormalized.**
   Risk analysis is all "what hour of day, what day of week" — computing that
   at query time across timezone changes is bug bait. We store
   `localHourOfDay` and `localDayOfWeek` at write time.

5. **UserProfile/Settings live in DataStore, not Room.** Scalars (mode,
   onboarding flags, moral-incongruence score, theme) don't need SQL. The
   exceptions — user values and risk windows — are lists the SQL layer joins
   against, so they get tables.

6. **No soft deletes, no sync columns.** On-device-only forever is the
   architecture; delete means DELETE (M6's "delete all my data" is a real
   wipe, and we can market it honestly).

## Tables

### `urge_event` — every episode (ships in M2)
| Column | Type | Notes |
|---|---|---|
| `id` | Long PK autoGenerate | |
| `startedAtEpochMs` | Long, indexed | when the episode began |
| `endedAtEpochMs` | Long? | null while in progress |
| `localHourOfDay` | Int | 0–23, denormalized at write |
| `localDayOfWeek` | Int | 1–7 (ISO), denormalized at write |
| `mode` | Text | `SOCIAL` \| `PORN` |
| `entryPoint` | Text | `WIDGET` \| `NOTIFICATION` \| `IN_APP` \| `INTERCEPT` \| `RETRO_LOG` |
| `interceptedPackage` | Text? | M4: which app fired the intercept |
| `outcome` | Text | `SURFED` \| `LAPSED` \| `CONTINUED` (intercept: "continue anyway") \| `SKIPPED_APP` (intercept: "I'll skip it") \| `ABANDONED` (left mid-flow) |
| `furthestStep` | Text? | `BREATH` \| `SURF` \| `PLAN` \| `LOG` — how far up the ladder |
| `intensityBefore` | Int? | 1–5, optional self-report |
| `intensityAfter` | Int? | 1–5 — "the wave passed" evidence, feeds progress |
| `mood` | Text? | `CALM` `STRESSED` `BORED` `LONELY` `SAD` `ANXIOUS` `TIRED` `ANGRY` |
| `shownPlanId` | Long? FK → `if_then_plan` (SET_NULL) | which plan step 3 displayed |
| `note` | Text? | optional free text |

`RETRO_LOG` covers "I lapsed earlier and I'm logging it now" — a lapse never
requires having opened the SOS flow first.

### `lapse_debrief` — 1:1 optional extension (ships in M2)
| Column | Type | Notes |
|---|---|---|
| `id` | Long PK autoGenerate | |
| `urgeEventId` | Long FK → `urge_event` (CASCADE), unique index | |
| `completedAtEpochMs` | Long | |
| `whatPreceded` | Text? | the 2-min debrief: situation before the lapse |
| `nextTimeIdea` | Text? | "what I'd try next time" — seed for if-then plans (M3) |
| `debriefCompleted` | Boolean | they can bail early, kindly |

### `trigger` — shared taxonomy (ships in M2)
| Column | Type | Notes |
|---|---|---|
| `id` | Long PK autoGenerate | |
| `name` | Text | "Alone late at night", "Stress after work"… |
| `category` | Text | `EMOTION` \| `TIME` \| `PLACE` \| `ACTIVITY` \| `SOCIAL` |
| `isCustom` | Boolean | seeded rows = false |
| `isArchived` | Boolean | hide without breaking old events |

### `event_trigger` — junction (ships in M2)
`urgeEventId` FK CASCADE + `triggerId` FK RESTRICT, composite PK, index on `triggerId`.

### `if_then_plan` (ships in M3; referenced by M2 as nullable FK)
| Column | Type | Notes |
|---|---|---|
| `id` | Long PK autoGenerate | |
| `situationText` | Text | the "If …" |
| `actionText` | Text | the "then I will …" |
| `situationTriggerId` | Long? FK → `trigger` (SET_NULL) | lets the SOS flow surface the plan matching the logged trigger — the adaptive hook |
| `mode` | Text? | null = applies to both modes |
| `isActive` | Boolean | free tier: one active plan |
| `createdAtEpochMs` / `updatedAtEpochMs` | Long | |
| `rehearsalCount` | Int | rehearsed plans work better — tracked, shown gently |
| `lastRehearsedAtEpochMs` | Long? | |
| `timesShown` / `timesMarkedUsed` | Int | plan effectiveness without surveillance |

### `user_value` (ships in M1)
| Column | Type | Notes |
|---|---|---|
| `id` | Long PK autoGenerate | |
| `name` | Text | from the values assessment ("Being present with family"…) |
| `rank` | Int | display order |

### `risk_window` (ships in M3, reused by M4)
| Column | Type | Notes |
|---|---|---|
| `id` | Long PK autoGenerate | |
| `label` | Text | "Late night", "Sunday evening" |
| `daysOfWeekMask` | Int | bitmask Mon=1<<0 … Sun=1<<6 |
| `startMinuteOfDay` / `endMinuteOfDay` | Int | may wrap past midnight (start > end) |
| `isEnabled` | Boolean | |

### `monitored_app` (ships in M4)
| Column | Type | Notes |
|---|---|---|
| `packageName` | Text PK | only package names, never content |
| `label` | Text | cached display name |
| `mode` | Text | which mode this app belongs to |
| `isEnabled` | Boolean | |

## DataStore (Preferences) — not Room
`mode` (SOCIAL/PORN/BOTH), onboarding-complete flag, moral-incongruence score +
framing route (M1), theme override, notification/reminder opt-ins, PIN/biometric
lock setting (M6), premium entitlement cache (M5).

## Room setup notes
- `exportSchema = true` with `schemas/` checked into git from day one → real
  migration tests later.
- Database name: `stillwater.db`. No passphrase/SQLCipher for MVP: Android
  app-sandbox + full-disk encryption already protect it, and the M6
  PIN lock is app-level. Revisit only if user research demands it.
