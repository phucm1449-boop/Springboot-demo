package com.example.demo.exceptions;

import com.example.demo.models.ProductImage;
import lombok.Getter;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

@Getter
public enum ErrorCode {
    RUNTIME_EXCEPTION(9999, "Runtime Exception", HttpStatus.INTERNAL_SERVER_ERROR),
    INVALID_MESSAGE_KEY(9998, "Invalid message key", HttpStatus.BAD_REQUEST),
    DATA_EXISTED(1001, "Phone number already exists", HttpStatus.BAD_REQUEST),
    DATA_NOT_FOUND(1002, "Data not found", HttpStatus.NOT_FOUND),
    DATE_MUST_BE_AT_LEAST_TODAY(1003, "Date must be at least today!", HttpStatus.BAD_REQUEST),
    SIZE_IS_LARGER_THAN_MAXIMUM_IMAGES_PER_PRODUCT(1004,
            "Number of images must be <= " + ProductImage.MAXIMUM_IMAGES_PER_PRODUCT, HttpStatus.BAD_REQUEST),
    UNAUTHENTICATED(1005, "Unauthenticated", HttpStatus.UNAUTHORIZED),
    YOU_CAN_NOT_REGISTER_USER(1006, "You cannot register an admin account", HttpStatus.BAD_REQUEST),
    VALIDATE_NAME_OF_PRODUCT(1007, "Title must be between 3 and 200 character", HttpStatus.BAD_REQUEST),
    USER_NOT_FOUND(1008, "User not found with phone number: %s", HttpStatus.BAD_REQUEST),
    CANNOT_CREATE_JWT_TOKEN(1009, "Can't create JWT token", HttpStatus.BAD_REQUEST),
    BAD_CREDENTIALS(1010, "Wrong number or password", HttpStatus.BAD_REQUEST),
    INVALID_PHONE_NUMBER_OR_PASSWORD(1011, "Invalid phone number/password", HttpStatus.BAD_REQUEST),
    INVALID_JWT_TOKEN(1012, "Invalid JWT token", HttpStatus.BAD_REQUEST),
    FORBIDDEN(1013, "You do not have permission", HttpStatus.FORBIDDEN),
    INVALID_PASSWORD(1014, "Password must be at least 8 character", HttpStatus.BAD_REQUEST),
    INVALID_DOB(1015, "Your age must be at least {min}", HttpStatus.BAD_REQUEST)
    ;

    ErrorCode(int code, String message, HttpStatusCode statusCode) {
        this.code = code;
        this.message = message;
        this.statusCode = statusCode;
    }

    private int code;
    private String message;
    private HttpStatusCode statusCode;

}
