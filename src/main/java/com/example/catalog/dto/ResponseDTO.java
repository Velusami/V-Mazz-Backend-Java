package com.example.catalog.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ResponseDTO {
    private Object data;
    private boolean status;
    private String message;
    private String docNo;
}
