package com.clipit.auth_service.controller;

import com.clipit.auth_service.entity.User;
import com.clipit.auth_service.repository.UserRepository;
import com.clipit.auth_service.service.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
public class AuthController {

	@Autowired
	private UserRepository repository;
	@Autowired
	private PasswordEncoder passwordEncoder;
	@Autowired
	private JwtService jwtService;
	@Autowired
	private AuthenticationManager authenticationManager;

	@PostMapping("/register")
	public String addNewUser(@RequestBody User user) {
		user.setPassword(passwordEncoder.encode(user.getPassword()));
		repository.save(user);
		return "User added successfully";
	}

	@PostMapping("/login")
	public String getToken(@RequestBody User user) {
		Authentication authenticate = authenticationManager
				.authenticate(new UsernamePasswordAuthenticationToken(user.getUsername(), user.getPassword()));

		if (authenticate.isAuthenticated()) {
			User fullUser = repository.findByUsername(user.getUsername())
					.orElseThrow(() -> new RuntimeException("User not found"));

			return jwtService.generateToken(fullUser.getUsername(), fullUser.getId());
		} else {
			throw new RuntimeException("Invalid access");
		}
	}
}
