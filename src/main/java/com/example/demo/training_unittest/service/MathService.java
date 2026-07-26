package com.example.demo.training_unittest.service;

public class MathService {
    public int multiply(int a, int b) {
        return a * b;
    }

    public int divide(int a, int b) {
        if(b == 0) {
            throw new IllegalArgumentException("b cannot be zero");
        }
        return a / b;
    }

    public void validateAge(int age) {
        if(age < 18) {
            throw new IllegalArgumentException(
                    "Age must be >= 18"
            );
        }
    }

}
