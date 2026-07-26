package com.example.demo.training_unittest.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

public class MathServiceTest {

    private MathService mathService;
    @BeforeEach
    void initData() {
        this.mathService = new MathService();
    }

    @Test
    void multiply_success() {
        int result = this.mathService.multiply(2, 3);
        assertThat(result).isEqualTo(6);
    }

    @Test
    void divide_zero_throwException() {
        assertThatThrownBy(
                () -> this.mathService.divide(10, 0)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("b cannot be zero");
    }

    @Test
    void validateAge_ageLessThan18_throwException() {
        assertThatThrownBy(
                () -> this.mathService.validateAge(15)
        )
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Age must be >= 18");
    }

    @Test
    void validateAge_validAge_success() {
        assertDoesNotThrow(
                () -> this.mathService.validateAge(20)
        );
    }
}
