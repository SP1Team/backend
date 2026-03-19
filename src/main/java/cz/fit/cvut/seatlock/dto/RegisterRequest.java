package cz.fit.cvut.seatlock.dto;

import lombok.Data;

@Data
public class RegisterRequest {
    private String email;
    private String password;
    private String name;
    private String surname;
    private String countryCode;
    private String phoneNumber;
}