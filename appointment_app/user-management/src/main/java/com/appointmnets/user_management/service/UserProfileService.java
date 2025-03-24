package com.appointmnets.user_management.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.appointmnets.user_management.entity.UserProfile;
import com.appointmnets.user_management.repo.UserProfileRepository;

@Service
public class UserProfileService {

    @Autowired
    private UserProfileRepository userProfileRepository;

    public UserProfile getUserById(Long id) {
        return userProfileRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found with ID: " + id));
    }

    public UserProfile createOrUpdateUser(UserProfile userProfile) {
        return userProfileRepository.save(userProfile);
    }

    public void deleteUser(Long id) {
        userProfileRepository.deleteById(id);
    }
}
