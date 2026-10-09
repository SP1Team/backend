package cz.fit.cvut.seatlock.service;

import cz.fit.cvut.seatlock.domain.User;
import cz.fit.cvut.seatlock.domain.enums.Role;
import cz.fit.cvut.seatlock.dto.LoginRequest;
import cz.fit.cvut.seatlock.dto.RegisterRequest;
import cz.fit.cvut.seatlock.exception.InvalidCredentialsException;
import cz.fit.cvut.seatlock.exception.UserAlreadyExistsException;
import cz.fit.cvut.seatlock.repository.UserRepository;
import cz.fit.cvut.seatlock.security.JWTUtils;
import cz.fit.cvut.seatlock.security.SeatlockUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.jooq.exception.IntegrityConstraintViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {
    private final UserRepository userRepository;
    private final SeatlockUserDetailsService seatlockUserDetailsService;
    private final JWTUtils jwtUtils;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public void register(RegisterRequest registerRequest){
        try{
            User newUser = new User(registerRequest.email(),
                    passwordEncoder.encode(registerRequest.password()),
                    registerRequest.name(),
                    registerRequest.surname(),
                    registerRequest.countryCode(),
                    registerRequest.phoneNumber()
            );
            newUser.setRole(Role.ROLE_CUSTOMER);
            userRepository.save(newUser);
        }catch (IntegrityConstraintViolationException ex){
            throw new UserAlreadyExistsException("User with this email or phone number already exists");
        }
    }


    public String login(LoginRequest loginRequest) {
        var userDetails = seatlockUserDetailsService.loadUserByUsername(loginRequest.email());
        if(!passwordEncoder.matches(loginRequest.password(), userDetails.getPassword())){
            throw new InvalidCredentialsException("Invalid credentials");
        }
        return jwtUtils.generateToken(userDetails);
    }
}
