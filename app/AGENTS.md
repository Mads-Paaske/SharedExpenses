# Testing

Whenever work is done (a feature, bug fix, or change), write relevant unit tests for it.

# Branching

Never commit directly to `main`. Before starting any work, create (or switch to) a feature branch, and do all commits there.

# Project

Android app for tracking shared group expenses (WeShare / MobilePay-groups style).
Requirements: F01–F13 and NF01–NF06 in the design document. **Reliability is the key quality attribute.**

## Stack (from the lectures)
- Kotlin, Jetpack Compose, Material 3, single `MainActivity` (Lecture 7)
- Theme in `ui/theme/Theme.kt`, `Color.kt`, `Type.kt`; `dynamicColor = false` (Lecture 8)
- All user-facing text in `res/values/strings.xml` – never hard-coded in composables
- MVVM (Lecture 8/9): `ViewModel` with `private val _x = MutableStateFlow(..)` + `val x = _x.asStateFlow()`
  (or `stateIn(...)`); screens read with `collectAsStateWithLifecycle()`
- Screens take the ViewModel as a default param: `vm: XViewModel = viewModel(factory = AppViewModelProvider.Factory)`
- `remember` for temporary UI state, `rememberSaveable` for form input (Lecture 8)
- Navigation Compose 2.9.5 (Lecture 9): string routes in `ui/navigation/Routes.kt`,
  nested graphs `AUTH_FLOW` / `APP_FLOW` / `ADD_EXPENSE_FLOW`, `popUpTo(..) { inclusive = true }` after login.
  Screens receive callbacks (`onBack`, `onGroupClick`, ...) – only `AppNavigation.kt` touches the NavController.
- Library versions from the slides: `lifecycle-viewmodel-compose:2.10.0`, `navigation-compose:2.9.5`

## Architecture
```
Compose screens  ->  ViewModels (StateFlow)  ->  Repositories  ->  Room (single source of truth)
                                                     |
                                                     +-> Outbox table -> SyncWorker (WorkManager) -> FakeRemoteApi
```
- `data/local` – Room entities + DAOs. Screens only read from Room, so data is available offline (NF03).
- `data/repository` – every write goes to Room first (UI never waits for network, NF02) and is queued in the
  `outbox` table via `SyncRepository.enqueue(...)`.
- `data/sync/SyncEngine` – sends the outbox; marks rows SYNCED / FAILED. Pure Kotlin → unit-testable.
- `data/sync/SyncWorker` – WorkManager job, `NetworkType.CONNECTED`, exponential backoff, `Result.retry()` (NF01).
- `data/sync/Notifier` – system notifications; NF05 "previously failed request succeeded" is always shown.
- `data/remote/FakeRemoteApi` – mock server (NF06), 2.5 s latency, fails when offline or when the demo
  "simulate network failure" switch is on (`NetworkMonitor.setSimulatedOffline`).
- `domain/DebtCalculator` – F10 algorithm (net balances + greedy settlements). Money is `Long` øre, never `Double`.
- Only SYNCED expenses count in balances ("excluded from shared balances until synced").
- `AppContainer` – manual dependency injection, created in `SharedExpensesApplication`.

## Demo data
Seeded on first launch (`SeedData.kt`): Alex, Sam, Mia, Taylor; groups "Weekend trip" and "Flatmates".
Log in with `alex@example.com` / `password`.

## Definition of done
- Builds: `./gradlew assembleDebug`
- Unit tests pass: `./gradlew testDebugUnitTest` (new logic needs tests; reliability changes need a
  `SyncEngineReliabilityTest`-style test)
- Strings in `strings.xml`, follows the lecture patterns above
- Merged to main only through a pull request reviewed by a teammate who did not generate the code, with CI green
- AI use logged in the AI dossier (Appendix A), including failures

## Known gaps / not done
- No real backend, push notifications or camera – all mocked on purpose (NF06).
- No "mark as paid / record payment" – not in the requirements.
