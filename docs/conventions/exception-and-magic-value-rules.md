# Exception And Magic Value Rules

## Business Exceptions

- Service-layer business failures must use `EasyArchiveErrorCode`.
- Do not throw `IllegalArgumentException` or `IllegalStateException` for business validation, permission checks, or state transitions.
- Prefer `EasyArchiveException` or a module-specific subtype such as `StarterManageException`.

## Error-Code Design

- Use module-prefixed enums such as `CommonErrorCode`, `CoreErrorCode`, and `StarterErrorCode`.
- New dynamic messages must use placeholder-based error codes instead of string concatenation.
- Add new error codes near the business domain they represent; do not hide business failures behind generic parameter errors.

## Magic Values

- Use enums for closed-set business vocabularies such as status, switch, type, and unit values.
- Use `private static final` constants for technical thresholds, default counters, and fixed implementation policy values.
- Do not extract trivial local literals into shared constants without a readability benefit.

## Implementation Guidance

- Prefer enum helper methods such as `isEnabled`, `fromCode`, and `isDisabled` over direct raw code comparisons.
- When refactoring existing logic, keep behavioral changes minimal unless the previous behavior is clearly broken.
- Tests should assert exception type and error code before asserting raw message text.
