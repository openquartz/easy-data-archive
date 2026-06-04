# EasyArchive Exception And Magic Value Governance Design

## Background

`easy-archive-master` already has a basic exception abstraction in `easyarchive-common`, including:

- `EasyArchiveErrorCode`
- `EasyArchiveException`
- `Asserts`
- a small set of shared error-code enums

That baseline is not consistently applied across the repository.

Current problems:

- `easyarchive-starter` service implementations still throw `IllegalArgumentException` and `IllegalStateException` for business failures
- many user-facing failure messages are embedded directly in service logic, so the message string itself is a magic value
- some status and type comparisons still use raw `0`, `1`, or ad hoc strings instead of enum-backed semantics
- some technical thresholds and fixed literals are duplicated instead of being named constants
- current error-code enums are inconsistent in naming and prefix application, and do not yet cover the main business domains

The result is that business rules are harder to audit, error handling is fragmented, and future code generation or AI-assisted edits do not have one project-local rule to follow.

## Goal

Introduce a repository-wide rule set and code structure so that:

- business failures use project-defined exceptions and error codes instead of raw JDK runtime exceptions
- service-layer validation and state-transition errors are bound to stable error codes
- domain status/type values use enums where they carry business meaning
- purely technical thresholds use named constants instead of inline literals
- the repository contains an explicit written convention that future contributors and coding agents can follow

## Scope

In scope:

- `easyarchive-common` exception foundation and shared conventions
- `easyarchive-core` project-owned magic values and business-facing runtime exceptions
- `easyarchive-starter` service-layer business exceptions and high-value magic values
- repository documentation for exception and magic-value governance

Out of scope:

- third-party library internals copied into utility classes unless the project has clearly customized them
- broad architectural refactors unrelated to exception handling or magic-value cleanup
- changing every numeric literal in the codebase regardless of meaning
- controller response contract redesign

## Design Principles

### Business Failure Must Carry An Error Code

If a failure represents business validation, missing domain data, permission denial, unsupported state, or unsupported project-side integration shape, it must map to `EasyArchiveErrorCode`.

Direct use of these exceptions for business flow is disallowed:

- `IllegalArgumentException`
- `IllegalStateException`
- ad hoc `RuntimeException`

They may remain only where the code is guarding internal programmer misuse in low-level utilities and the failure is not part of the service/domain contract.

### Business Meaning Uses Enums

A raw value should become an enum when it represents a closed business state or command vocabulary, such as:

- enable/disable
- binary on/off
- datasource type
- datasource connectivity status
- task execution status
- time unit
- executor command classification

### Technical Policy Uses Constants

A raw value should become a named constant when it represents an implementation threshold or fixed technical policy, such as:

- batch flush interval
- default processed-record count
- default finished flag
- supported reflection getter names
- fixed operation labels used only inside one class

### Prefer Local Clarity Over Over-Abstraction

Constants should stay in the narrowest class that owns them unless they are truly shared.

Enums should only be introduced for closed vocabularies with real semantic meaning.

This design does not convert every repeated string into a global constant, and it does not create enums for simple algorithm knobs.

## Exception Model

### Shared Foundation

Keep `EasyArchiveErrorCode` and `EasyArchiveException` as the repository-wide base abstraction.

Refine them so they support:

- stable error code retrieval
- stable default message retrieval
- placeholder-based message formatting
- subclass extension for domain/service exceptions when needed

### Error-Code Layering

Use layered error-code enums modeled after the `easy-event` style, but consistent across this repository.

Minimum layers:

- `CommonErrorCode` for repository-wide shared technical/business primitives
- `CoreErrorCode` for `easyarchive-core`
- `StarterErrorCode` for `easyarchive-starter`

Start with one `StarterErrorCode`. If it exceeds roughly 20 to 30 entries during implementation, split it by business domain while preserving one naming rule. Split points:

- archive group
- archive group item
- datasource
- archive task
- data permission
- user and permission assignment

### Service Exception Type

Introduce a service-facing custom exception type for management/business failures in `easyarchive-starter`.

Recommended shape:

- a subclass of `EasyArchiveException`
- narrow purpose: management/business-side failures
- constructors aligned with `EasyArchiveErrorCode`

This keeps service code explicit and prevents regression back to JDK runtime exceptions.

`easyarchive-core` will continue to use `EasyArchiveException` directly in this pass unless a dedicated subtype is already justified by an existing domain boundary.

### Message Formatting

Dynamic failure messages must use placeholder-capable error codes instead of inline string concatenation.

Examples of cases that should use placeholders:

- unsupported datasource runtime type
- duplicate business key values
- missing referenced objects identified by ID or code
- numeric validation constraints that need actual values in the message

The implementation should make placeholder formatting a normal construction path rather than a surprising static helper that throws from inside the helper body.

## Magic Value Governance Rules

### Convert To Enum

Convert a magic value to enum when all of the following are true:

1. the value is a closed set
2. callers branch behavior based on the value
3. the value has business or command semantics

Immediate targets include:

- `RandomAlphaNumExecutor` classifier strings such as `type1` through `type4`
- `TimeAddExecutor` time unit strings
- user/deleted/status checks that still use raw `0` and `1` semantics

### Convert To Constant

Convert a magic value to constant when all of the following are true:

1. the value is an implementation threshold, default, or fixed literal
2. the vocabulary is not a true business-state enum
3. naming the value improves readability more than introducing a new type

Immediate targets include:

- `MysqlSink` batch commit size
- default task counters and finished flags in execution flows
- repeated class-local labels used in operation-log presenters where the value is not a shared domain code

### Leave As-Is

Do not force conversion when the literal is already obvious and local, such as:

- a `null` check
- collection empty checks using `0`
- loop indexing or JDK API semantics where the literal is part of normal language structure

The goal is clarity, not literal elimination for its own sake.

## Module Plan

### easyarchive-common

Changes:

- normalize exception utility behavior
- align error-code enum structure and prefix application
- fix inconsistent shared enums such as `DataExecuteErrorCode`
- provide only helper improvements that directly reduce repeated business-exception boilerplate

Expected result:

- one coherent base exception model
- one coherent error-code naming and formatting model

### easyarchive-core

Changes:

- replace project-owned raw business/runtime exceptions with error-code-backed exceptions
- replace obvious magic strings and thresholds with enums/constants

Priority targets:

- `RandomAlphaNumExecutor`
- `TimeAddExecutor`
- `MysqlSink`
- execution paths with named default values that currently use inline literals

### easyarchive-starter

Changes:

- replace service-layer `IllegalArgumentException` and `IllegalStateException` with the custom business exception flow
- migrate validation and state-transition failures to error codes
- reduce duplicated validation literals by extracting constants or helper methods where it improves clarity
- keep controller contract behavior stable unless a targeted test requires adjustment

Priority services:

- `ArchiveConnectionServiceImpl`
- `ArchiveGroupServiceImpl`
- `ArchiveGroupExecutionServiceImpl`
- `ArchiveGroupItemByIdServiceImpl`
- `ArchiveGroupItemByTimeServiceImpl`
- `ArchiveTaskLogServiceImpl`
- `DataPermissionServiceImpl`
- `UserDatasourcePermissionServiceImpl`

## Repository Rule Documentation

Add a project-visible rule document under `docs/conventions/` and also update the repository agent guidance so future AI-assisted edits follow the same standard.

The written rule set must include:

- business exceptions must use `EasyArchiveErrorCode`
- no direct `IllegalArgumentException` or `IllegalStateException` in service-layer business logic
- when to use enums versus constants
- naming and prefix rules for new error-code enums
- placeholder-message rules
- guidance to prefer enum helper methods over raw code comparisons

This rule is intended for human contributors and tools such as Codex or Claude Code.

## Testing Strategy

### Unit And Service Tests

Update or add focused tests so they assert:

- business methods throw project exceptions instead of raw JDK runtime exceptions
- the thrown exception carries the expected error code
- enum-based validation still preserves previous behavior

Prefer asserting exception type plus error code over asserting full message text.

### Repository Scan

Run targeted repository scans after the refactor to confirm the intended cleanup:

- no remaining service-layer `IllegalArgumentException` for business validation
- no remaining service-layer `IllegalStateException` for business state failures
- no remaining targeted magic strings and literals in the identified hotspots

### Build Verification

Run focused Maven tests for touched modules, then widen as far as practical within the session.

Minimum expected verification:

- `easyarchive-common` tests if present
- `easyarchive-core` targeted tests if touched
- `easyarchive-starter` service tests affected by exception behavior

## Risks And Constraints

- Changing exception types may require test updates where current tests explicitly assert JDK exception classes.
- Some user-facing API behavior may currently depend on generic exception handling; implementation must preserve existing response shape unless a deliberate correction is required.
- Not every raw literal in the repository should change in this pass; overreaching would add churn without improving clarity.
- Existing uncommitted user changes must not be overwritten. This refactor should stay isolated to directly relevant files.

## Implementation Outline

1. Normalize the shared exception foundation in `easyarchive-common`.
2. Add or refine module-level error-code enums and the service custom exception.
3. Replace business exceptions in `easyarchive-starter` services.
4. Replace targeted business-facing exceptions and magic values in `easyarchive-core`.
5. Extract named constants for technical thresholds and defaults where they materially improve clarity.
6. Add repository rule documentation and update local agent guidance for future compliance.
7. Add or update tests to assert exception type and error code behavior.
8. Run focused verification and final repository scans.
