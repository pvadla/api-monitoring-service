package com.api.monitor.service;

import com.api.monitor.entity.User;
import com.api.monitor.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        // Get info from Google
        String email = oAuth2User.getAttribute("email");
        String name = oAuth2User.getAttribute("name");
        String picture = oAuth2User.getAttribute("picture");

        // Save or update user in DB
        User user = userRepository.findByEmail(email).orElse(new User());

        user.setEmail(email);
        user.setName(name);
        user.setPicture(picture);
        if (user.getStatusSlug() == null || user.getStatusSlug().isBlank()) {
            String slug = email != null && email.contains("@")
                    ? email.substring(0, email.indexOf("@")).toLowerCase().replaceAll("[^a-z0-9]", "-").replaceAll("-+", "-")
                    : "user";
            if (slug.isBlank()) slug = "user";
            user.setStatusSlug(slug);
        }
        userRepository.save(user);

        return oAuth2User;
    }
}