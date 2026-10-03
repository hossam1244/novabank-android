# NovaBank Android

**Business e-banking Android client** — Kotlin · Jetpack Compose · Hilt

The native Android client for [novabank-api](https://github.com/hossam1244/novabank-api):
MVVM + Clean Architecture over Compose, Hilt injection, Retrofit/OkHttp with a
**synchronized token-refresh interceptor**, EncryptedSharedPreferences for tokens,
Room-backed offline dashboard, and **idempotent payment submission**.

> Fictional product, real practices. Portfolio project by
> [Hossam Rakha](https://github.com/hossam1244). Cross-platform twin of
> [novabank-flutter](https://github.com/hossam1244/novabank-flutter) — same API,
> native toolchain.

[![CI](https://github.com/hossam1244/novabank-android/actions/workflows/ci.yml/badge.svg)](https://github.com/hossam1244/novabank-android/actions/workflows/ci.yml)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1-7F52FF)](https://kotlinlang.org)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

## Architecture

```
app/src/main/java/com/novabank/app/
├── core/
│   ├── network/   NovaBankApi (Retrofit) · DTOs (kotlinx.serialization)
│   │              AuthInterceptor — Bearer + one synchronized refresh on 401
│   ├── storage/   TokenStore — EncryptedSharedPreferences (Android Keystore)
│   └── data/      BankingRepository · Room account cache (Dao + Entity + Database)
├── di/            Hilt modules (network, storage, database)
└── ui/
    ├── NovaBankRoot.kt   Session state machine + navigation
    ├── theme/            Material 3 (navy/gold banking palette)
    └── screens/          Login · Dashboard · Payments (+ sheet) · Cards
```

## The decisions that matter

* **Token refresh is lock-guarded.** OkHttp's `AuthInterceptor` refreshes on 401
  under a `ReentrantLock`, and threads that waited re-check for a newer token
  before refreshing again — a rotating refresh token is never used twice, and
  every failed request is replayed exactly once.
* **Tokens live in EncryptedSharedPreferences** — AES-256 keys in the Android
  Keystore, never plain preferences.
* **Payments carry an `Idempotency-Key`** (UUID per submission attempt):
  transport retries replay the original payment server-side; a new submit gets
  a new key. Retries can never double-debit.
* **The dashboard is offline-tolerant**: Room caches accounts; network-first
  with instant cache paint on failure.
* **kotlinx.serialization** end-to-end — no reflection-based parsing.

## Build & test

```bash
./gradlew testDebugUnitTest   # 6 JVM unit tests (MockWebServer-driven)
./gradlew assembleDebug
# Point at your API: ./gradlew assembleDebug -PapiBaseUrl=http://10.0.2.2:8000
```

Unit tests cover the interceptor contract end-to-end against MockWebServer:
bearer attachment, 401 → refresh → replay with the new token, refresh-failure
session teardown, no-refresh-on-login-401, and no retry loops.

## License

[MIT](LICENSE)
