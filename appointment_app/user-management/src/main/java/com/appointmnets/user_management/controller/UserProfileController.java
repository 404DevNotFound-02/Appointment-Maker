package com.appointmnets.user_management.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import com.appointmnets.user_management.entity.UserProfile;
import com.appointmnets.user_management.service.UserProfileService;

@RestController
@RequestMapping("/users")
public class UserProfileController {

    @Autowired
    private UserProfileService userProfileService;

    @GetMapping("/{id}")
    public UserProfile getUserById(@PathVariable Long id) {
        return userProfileService.getUserById(id);
    }

    @PostMapping
    public UserProfile createUser(@RequestBody UserProfile userProfile) {
        return userProfileService.createOrUpdateUser(userProfile);
    }

    @PutMapping("/{id}")
    public UserProfile updateUser(@PathVariable Long id, @RequestBody UserProfile userProfile) {
        userProfile.setId(id);
        return userProfileService.createOrUpdateUser(userProfile);
    }

    @DeleteMapping("/{id}")
    public void deleteUser(@PathVariable Long id) {
        userProfileService.deleteUser(id);
    }
}
