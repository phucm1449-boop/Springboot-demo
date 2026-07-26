package com.example.demo.responses;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)// if that field is null, it will not display
public class ApiResponse <T> {
    private int code = 1000; // api response successfully
    private String message;
    private T result;// return 1 object

}
