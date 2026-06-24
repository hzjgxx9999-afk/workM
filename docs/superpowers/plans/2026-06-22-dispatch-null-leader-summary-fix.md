# Dispatch Null Leader Summary Fix Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Prevent the manager app from crashing when a dispatch detail response contains `leaderSummary: null`.

**Architecture:** Align the Retrofit/Gson DTO with the backend's nullable contract and centralize summary fallback formatting in the existing dispatch presentation object. Exercise the behavior with a JVM regression test that deserializes the real null JSON shape.

**Tech Stack:** Kotlin, Gson, JUnit 4, Android Gradle Plugin

---

### Task 1: Reproduce nullable summary response

**Files:**
- Create: `app/src/test/java/com/qkzc/workerm/DispatchDetailNullSafetyTest.kt`

- [ ] Add a test that deserializes `{\"leaderSummary\":null}` into `DispatchOrder` and calls `DispatchStatusPresentation.summaryText`.
- [ ] Run `./gradlew.bat :app:testDebugUnitTest --tests com.qkzc.workerm.DispatchDetailNullSafetyTest` and verify it fails because the null-safe presentation API does not exist.

### Task 2: Implement the minimal null-safe contract

**Files:**
- Modify: `app/src/main/java/com/qkzc/workerm/data/dispatch/DispatchModels.kt`
- Modify: `app/src/main/java/com/qkzc/workerm/ui/dispatch/DispatchDetailActivity.kt`

- [ ] Declare `DispatchOrder.leaderSummary` as `String? = null`.
- [ ] Add `DispatchStatusPresentation.summaryText(String?)`, returning the existing fallback when null or blank.
- [ ] Make `DispatchDetailActivity` render the summary through that function.
- [ ] Re-run the targeted test and verify it passes.

### Task 3: Verify no regression

**Files:**
- Verify all modified production and test files.

- [ ] Run `./gradlew.bat :app:testDebugUnitTest`.
- [ ] Run `./gradlew.bat :app:assembleDebug`.
- [ ] Inspect `git diff` and confirm no unrelated files were modified.
