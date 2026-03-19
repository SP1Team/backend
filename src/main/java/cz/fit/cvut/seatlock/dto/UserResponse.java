package cz.fit.cvut.seatlock.dto;

import lombok.Data;
import java.util.UUID;

@Data
public class UserResponse {
    private UUID id;
    private String email;
    private String name;
    private String surname;
}