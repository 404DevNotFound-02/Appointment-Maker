package com.appointmnets.auth_service.repo;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.appointmnets.auth_service.entity.User;

@Repository
public interface UserCredentialRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

}
