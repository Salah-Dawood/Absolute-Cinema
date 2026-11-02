package com.cinemasaar.auth_service.Service;

import com.cinemasaar.auth_service.Exceptions.ApiException;
import com.cinemasaar.auth_service.Model.Requests.LoginRequest;
import com.cinemasaar.auth_service.Model.Requests.RegisterRequest;
import com.cinemasaar.auth_service.Model.Responses.AuthResponse;
import com.cinemasaar.auth_service.Model.Role;
import com.cinemasaar.auth_service.Model.User;
import com.cinemasaar.auth_service.Repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse register(RegisterRequest request) {
        String email = normalize(request.email());

        if (userRepository.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "Email is already registered");
        }

        // Public registration always creates a normal USER.
        // Admins are created by the seeder or promoted by another admin.
        User user = new User(
                request.name().trim(),
                email,
                passwordEncoder.encode(request.password()),
                Role.USER
        );
        User saved = userRepository.save(user);

        return buildResponse(saved);
    }

    public AuthResponse login(LoginRequest request) {
        String email = normalize(request.email());

        // Same message for "no such user" and "wrong password" so attackers
        // can't find out which emails are registered.
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
        }

        return buildResponse(user);
    }

    private AuthResponse buildResponse(User user) {
        return new AuthResponse(
                jwtService.generateToken(user),
                "Bearer",
                jwtService.getExpirationMs(),
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole()
        );
    }

    private String normalize(String email) {
        return email.trim().toLowerCase();
    }
}
