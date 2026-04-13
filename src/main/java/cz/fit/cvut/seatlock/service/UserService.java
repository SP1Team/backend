package cz.fit.cvut.seatlock.service;

import cz.fit.cvut.seatlock.dto.UserDTO;
import cz.fit.cvut.seatlock.repository.OrderRepository;
import cz.fit.cvut.seatlock.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;

    public UserDTO me(String email) {
        var user = userRepository.findUserByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        return new UserDTO(user.getName(), user.getSurname(), user.getEmail(), user.getRole().getAuthority());
    }
}