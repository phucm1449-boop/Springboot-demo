package com.example.demo.exceptions;

import com.example.demo.responses.ApiResponse;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.validation.ConstraintViolation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {
    // Sai kiểu dữ liệu (page=o)
//    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
//    public ResponseEntity<?> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
//        String param = ex.getName();
//        String message = String.format("Parameter '%s' must be a number", param);
//
//        return ResponseEntity.badRequest().body(message);
//    }
//
    // Sai validation trong DTO (page = -1)
//    @ExceptionHandler(RuntimeException.class)
//    public ResponseEntity<?> handleIllegalArgumentException(RuntimeException ex) {
//        return ResponseEntity.badRequest().body(ex.getMessage());
//    }
//
//    @ExceptionHandler(RuntimeException.class)
//    public ResponseEntity<ApiResponse> handleIllegalArgumentException(RuntimeException ex) {
//        ApiResponse apiResponse = new ApiResponse();
//        apiResponse.setCode(1001);
//        apiResponse.setMessage(ex.getMessage());
//        return ResponseEntity.badRequest().body(apiResponse);
//    }
//
//    @ExceptionHandler(HttpMessageNotReadableException.class)
//    public ResponseEntity<?> handleJsonParseError(HttpMessageNotReadableException ex) {
//
//        String message = "Dữ liệu nhập không hợp lệ";
//
//        Throwable cause = ex.getCause();
//
//        if (cause instanceof InvalidFormatException ife) {
//            String fieldName = ife.getPath().get(0).getFieldName();
//            Object value = ife.getValue();
//
//            message = String.format(
//                    "Trường '%s' phải là số hợp lệ, nhưng bạn nhập '%s'",
//                    fieldName,
//                    value
//            );
//        }
//
//        return ResponseEntity.badRequest().body(Map.of(
//                "error", message
//        ));
//    }
//
//    // Sai @Valid body (nếu có)
//    @ExceptionHandler(MethodArgumentNotValidException.class)
//    public ResponseEntity<?> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
//        String error = ex.getBindingResult()
//                .getFieldErrors()
//                .stream()
//                .findFirst()
//                .map(e -> e.getField() + ": " + e.getDefaultMessage())
//                .orElse("Invalid request");
//
//        return ResponseEntity.badRequest().body(error);
//    }

    @ExceptionHandler(AppException.class)
    public ResponseEntity<ApiResponse> handleAppException(AppException ex) {
        ErrorCode errorCode = ex.getErrorCode();
        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setCode(errorCode.getCode());
        apiResponse.setMessage(errorCode.getMessage());
        return ResponseEntity.status(errorCode.getStatusCode()).body(apiResponse);
    }

    // NẾU XẢY RA NHỮNG EXCEPTION KHÔNG THUỘC AppException.
    // VD: Sai kiểu dữ liệu (page=o), Sai validation trong DTO (page = -1)
    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ApiResponse> handleRunTimeException(RuntimeException ex) {
        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setCode(ErrorCode.RUNTIME_EXCEPTION.getCode());
        apiResponse.setMessage(ex.getMessage());
        return ResponseEntity.badRequest().body(apiResponse);
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ApiResponse> handleAuthorizationDeniedException(AuthorizationDeniedException ex) {
        ErrorCode errorCode = ErrorCode.FORBIDDEN;
        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setCode(errorCode.getCode());
        apiResponse.setMessage(errorCode.getMessage());
        return ResponseEntity.status(errorCode.getStatusCode()).body(apiResponse);
    }

    //handler @Valid in Spring
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex) {
        String enumKey = ex.getFieldError().getDefaultMessage();
        ErrorCode errorCode;
        Map<String, Object> attributes = null;
        try {
            errorCode = ErrorCode.valueOf(enumKey);

            var constraintViolations = ex.getBindingResult()
                    .getAllErrors().get(0).unwrap(ConstraintViolation.class);

            attributes = constraintViolations.getConstraintDescriptor().getAttributes();
            log.info(attributes.toString());
        } catch (Exception e) {
            errorCode = ErrorCode.INVALID_MESSAGE_KEY;
        }

        ApiResponse apiResponse = new ApiResponse();
        apiResponse.setCode(errorCode.getCode());
        apiResponse.setMessage(
                Objects.nonNull(attributes) ?
                        mapAttribute(errorCode.getMessage(), attributes)
                        : errorCode.getMessage());
        return ResponseEntity.badRequest().body(apiResponse);
    }

    private static final String MIN_ATTRIBUTE = "min";

    private String mapAttribute(String message, Map<String, Object> attributes) {
        String minValue = String.valueOf(attributes.get(MIN_ATTRIBUTE));

        return message.replace("{min}", minValue);
//        return message.replace("{" + MIN_ATTRIBUTE + "}", minValue);
    }
}
