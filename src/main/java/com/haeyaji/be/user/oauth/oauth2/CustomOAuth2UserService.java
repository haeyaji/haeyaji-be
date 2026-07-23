package com.haeyaji.be.user.oauth.oauth2;

import com.haeyaji.be.user.domain.SocialType;
import com.haeyaji.be.user.domain.User;
import com.haeyaji.be.user.oauth.OAuthAttributes;
import com.haeyaji.be.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest);

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        SocialType socialType = SocialType.valueOf(registrationId.toUpperCase());

        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();

        OAuthAttributes attrs = OAuthAttributes.of(socialType, userNameAttributeName, oAuth2User.getAttributes());

        Optional<User> optionalUser = userRepository.
                findBySocialTypeAndSocialTypeId(attrs.socialType(), attrs.socialTypeId());

        boolean newUser = optionalUser.isEmpty();

        User user = optionalUser
                .map(existingUser -> existingUser.update(attrs.email()))
                .orElseGet(() -> userRepository.save(attrs.toEntity()));

        return new CustomOAuth2User(user, newUser, List.of(new SimpleGrantedAuthority(user.getRole().name())), attrs.attributes(), attrs.nameAttributeKey());
    }
}
