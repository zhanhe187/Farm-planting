package com.farm.fpms.service;

import org.springframework.stereotype.Service;

@Service
public class PasswordService {

    public boolean matches(String inputPassword, String storedPassword) {
        if (inputPassword == null || inputPassword.isEmpty() || storedPassword == null || storedPassword.isEmpty()) {
            return false;
        }
        return inputPassword.equals(storedPassword);
    }
}
