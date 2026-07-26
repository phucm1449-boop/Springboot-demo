package com.example.demo.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.Objects;

public class DobValidator implements ConstraintValidator<DobConstraint, LocalDate> {
    private int min;

    // get thông tin từ annotation (method này sẽ run trước
    @Override
    public void initialize(DobConstraint constraintAnnotation) {
        ConstraintValidator.super.initialize(constraintAnnotation);
        min = constraintAnnotation.min();
    }

    //method sẽ xử lí data này có đúng hay không
    @Override
    public boolean isValid(LocalDate date, ConstraintValidatorContext constraintValidatorContext) {
        if (Objects.isNull(date)) {
            return true;
        }

        long years = ChronoUnit.YEARS.between(date, LocalDate.now());
        return years >= min;
    }
}
