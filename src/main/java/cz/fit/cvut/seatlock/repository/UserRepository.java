package cz.fit.cvut.seatlock.repository;

import cz.fit.cvut.seatlock.domain.User;
import org.jspecify.annotations.NonNull;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findUserByEmail(@NonNull String email);
}