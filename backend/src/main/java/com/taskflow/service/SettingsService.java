package com.taskflow.service;

import com.taskflow.dto.UserSettingsDTO;
import com.taskflow.dto.UserSettingsRequest;
import com.taskflow.exception.ForbiddenException;
import com.taskflow.model.User;
import com.taskflow.model.UserSettings;
import com.taskflow.repository.UserSettingsRepository;
import com.taskflow.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SettingsService {

    @Autowired
    private UserSettingsRepository settingsRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private UserService userService;

    //Gets the settings for the current authenticated user. Creates defaults if missing.
     
    @Transactional
    public UserSettingsDTO getCurrentUserSettings() {
        User current = userService.getCurrentUser();
        UserSettings settings = settingsRepository.findByUserId(current.getId())
                .orElseGet(() -> createDefaultSettings(current));
        return toDTO(settings);
    }

    @Transactional
    public UserSettingsDTO updateSettings(Long userId, UserSettingsRequest request) {
        User current = userService.getCurrentUser();
        if (!current.getId().equals(userId) && current.getRole() != com.taskflow.model.Role.ADMIN) {
            throw new ForbiddenException("Cannot update settings for another user");
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        UserSettings settings = settingsRepository.findByUserId(userId)
                .orElseGet(() -> createDefaultSettings(user));

        settings.setNotificationsEnabled(request.getNotificationsEnabled());
        settings.setDarkMode(request.getDarkMode());
        settings.setTimezone(request.getTimezone());

        return toDTO(settingsRepository.save(settings));
    }

    private UserSettings createDefaultSettings(User user) {
        UserSettings defaults = UserSettings.builder()
                .user(user)
                .notificationsEnabled(true)
                .darkMode(false)
                .timezone("UTC")
                .build();
        return settingsRepository.save(defaults);
    }

    private UserSettingsDTO toDTO(UserSettings settings) {
        return UserSettingsDTO.builder()
                .userId(settings.getUser().getId())
                .notificationsEnabled(settings.getNotificationsEnabled())
                .darkMode(settings.getDarkMode())
                .timezone(settings.getTimezone())
                .build();
    }
}
