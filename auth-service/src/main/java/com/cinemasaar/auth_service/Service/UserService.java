package com.cinemasaar.auth_service.Service;

import com.cinemasaar.auth_service.Exceptions.ApiException;
import com.cinemasaar.auth_service.Model.Responses.UserResponse;
import com.cinemasaar.auth_service.Model.Role;
import com.cinemasaar.auth_service.Model.User;
import com.cinemasaar.auth_service.Repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public UserResponse getById(String id) {
        return UserResponse.from(findUser(id));
    }

    public List<UserResponse> getAll() {
        return userRepository.findAll().stream()
                .map(UserResponse::from)
                .toList();
    }

    public UserResponse updateRole(String id, Role role) {
        User user = findUser(id);
        user.setRole(role);
        return UserResponse.from(userRepository.save(user));
    }

    private User findUser(String id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "User not found"));
    }
}
