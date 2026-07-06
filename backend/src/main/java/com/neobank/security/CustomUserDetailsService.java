package com.neobank.security;

import com.neobank.entity.User;
import com.neobank.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomUserDetailsService implements UserDetailsService {

	@Autowired
	private UserRepository userRepository;

	@Override
	public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {

		User user = userRepository.findByEmail(email)
				.orElseThrow(() -> new UsernameNotFoundException("User not found"));

		String role = String.valueOf(user.getRole());

		return new org.springframework.security.core.userdetails.User(user.getEmail(), user.getPasswordHash(),
				List.of(new SimpleGrantedAuthority("ROLE_" + role)));
	}
}