package com.farm.fpms.domain;

public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
