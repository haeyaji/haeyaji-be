package com.haeyaji.be.user.oauth.oidc;

import com.haeyaji.be.user.domain.SocialType;
import com.haeyaji.be.user.domain.User;
import com.haeyaji.be.user.oauth.OAuthAttributes;
import com.haeyaji.be.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CustomOidcUserService extends OidcUserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest); // id_token 검증 + (설정된 경우) userinfo 병합까지 끝난 상태

        /**
         * OAuth2ClientPropertiesRegistrationAdapter가
         * application.yml에 있는 security:oauth2:client:registration 안의
         * kakao/naver가 key, 나머지 내용이 value인 map으로 바인딩됨
         * 이 map을 순회하면서 각 쌍마다 ClientRegistration객체를 생성
         */

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        SocialType socialType = SocialType.valueOf(registrationId.toUpperCase());
        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();

        OAuthAttributes attrs = OAuthAttributes.of(socialType, userNameAttributeName, oidcUser.getAttributes());

        Optional<User> optionalUser = userRepository.
                findBySocialTypeAndSocialTypeId(attrs.socialType(), attrs.socialTypeId());

        boolean newUser = optionalUser.isEmpty();

        User user = optionalUser
                .map(existingUser -> existingUser.update(attrs.email()))
                // 정보가 바뀐채로 로그인될경우 update, 컬럼이 많아지면 dto 고려. 실제로 값이 바뀌지 않았을 경우에는 update쿼리 x (dirty check)
                .orElseGet(() -> userRepository.save(attrs.toEntity()));

        return new CustomOidcUser(user, newUser, List.of(new SimpleGrantedAuthority(user.getRole().name())), oidcUser.getIdToken(), oidcUser.getUserInfo(), attrs.nameAttributeKey());
    }
}