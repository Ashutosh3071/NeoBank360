package com.neobank.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.neobank.entity.User;
import com.neobank.enums.Role;

public interface UserRepository extends JpaRepository<User, Long> {

	Optional<User> findByEmail(String email);

	boolean existsByEmail(String email);

	boolean existsByAadhaarNumber(String aadhaarNumber);

	boolean existsByPanNumber(String panNumber);

	long countByRole(Role role);
}
