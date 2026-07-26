# USER_NOT_FOUND Change Proposal

Current code:

```java
USER_NOT_FOUND(1002, "User not found")
```

Requested behavior:

```java
"User not found with phone number: " + phoneNumber
```

## Why the current code cannot do this directly

`ErrorCode.USER_NOT_FOUND` is an enum with a fixed message.

`GlobalExceptionHandler` currently returns:

```java
apiResponse.setMessage(errorCode.getMessage());
```

That means even if the throw site knows `phoneNumber`, the response still uses the enum's static text.

## Proposed approach

1. Keep `ErrorCode.USER_NOT_FOUND` as the error identifier.
2. Add an `AppException(ErrorCode errorCode, String message)` constructor.
3. Change `GlobalExceptionHandler` to return `ex.getMessage()` for `AppException`.
4. Update the throw site:

```java
return userRepo.findByPhoneNumber(phoneNumber)
        .orElseThrow(() -> new AppException(
                ErrorCode.USER_NOT_FOUND,
                "User not found with phone number: " + phoneNumber
        ));
```

## Files that would need to change

- `src/main/java/com/example/demo/exceptions/AppException.java`
- `src/main/java/com/example/demo/exceptions/GlobalExceptionHandler.java`
- `src/main/java/com/example/demo/config/SecurityConfig.java`

## Notes

- This preserves the error code `1002`.
- Only the message becomes dynamic.
- If you want a cleaner reusable pattern later, the enum message could become a template such as:

```java
USER_NOT_FOUND(1002, "User not found with phone number: %s")
```

and the exception layer could format it with parameters.

## If you use `%s` in the enum

Example enum:

```java
USER_NOT_FOUND(1002, "User not found with phone number: %s")
```

In that case, `SecurityConfig` would be updated like this:

```java
return userRepo.findByPhoneNumber(phoneNumber)
        .orElseThrow(() -> new AppException(
                ErrorCode.USER_NOT_FOUND,
                String.format(ErrorCode.USER_NOT_FOUND.getMessage(), phoneNumber)
        ));
```

## What this requires

This `SecurityConfig` change still requires the same support in the exception flow:

- `AppException` needs a constructor that accepts `(ErrorCode errorCode, String message)`
- `GlobalExceptionHandler` needs to return `ex.getMessage()` for `AppException`

## Alternative

If you want to keep formatting logic out of `SecurityConfig`, another option is to add a helper in `ErrorCode` such as:

```java
public String format(Object... args) {
    return String.format(this.message, args);
}
```

Then `SecurityConfig` becomes:

```java
return userRepo.findByPhoneNumber(phoneNumber)
        .orElseThrow(() -> new AppException(
                ErrorCode.USER_NOT_FOUND,
                ErrorCode.USER_NOT_FOUND.format(phoneNumber)
        ));
```
