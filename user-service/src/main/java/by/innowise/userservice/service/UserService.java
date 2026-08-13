package by.innowise.userservice.service;

import by.innowise.userservice.dto.user.UserDetailsResponseDto;
import by.innowise.userservice.dto.user.UserRequestDto;
import by.innowise.userservice.dto.user.UserResponseDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {

    UserResponseDto create(UserRequestDto dto);

    UserResponseDto findById(Long id);

    UserResponseDto findByEmail(String email);

    Page<UserResponseDto> findAll(
            String name,
            String surname,
            Pageable pageable
    );

    UserDetailsResponseDto findDetailsById(Long id);

    UserResponseDto update(
            Long id,
            UserRequestDto dto
    );

    UserResponseDto setActive(
            Long id,
            boolean active
    );

    void delete(Long id);
}
