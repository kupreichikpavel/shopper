package by.innowise.userservice.service;

import by.innowise.userservice.dto.user.UserCreateDto;
import by.innowise.userservice.dto.user.UserDetailsResponseDto;
import by.innowise.userservice.dto.user.UserResponseDto;
import by.innowise.userservice.dto.user.UserUpdateDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface UserService {

    UserResponseDto create(UserCreateDto dto);

    UserResponseDto findById(Long id);

    Page<UserResponseDto> findAll(String name, String surname, Pageable pageable);

    UserDetailsResponseDto findDetailsById(Long id);

    UserResponseDto update(Long id, UserUpdateDto dto);

    UserResponseDto setActive(Long id, boolean active);

    void delete(Long id);
}
