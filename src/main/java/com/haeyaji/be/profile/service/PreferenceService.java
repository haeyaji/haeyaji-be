package com.haeyaji.be.profile.service;

import com.haeyaji.be.profile.domain.MemberPreference;
import com.haeyaji.be.profile.dto.PreferenceRequest;
import com.haeyaji.be.profile.repository.MemberPreferenceEntity;
import com.haeyaji.be.profile.repository.MemberPreferenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PreferenceService {

    private final MemberPreferenceRepository preferenceRepository;

    /** 설문 저장(upsert). member 1건이므로 있으면 덮어쓰고 없으면 생성. */
    @Transactional
    public MemberPreference save(UUID memberId, PreferenceRequest request) {
        MemberPreferenceEntity entity = preferenceRepository.findById(memberId)
                .orElseGet(() -> MemberPreferenceEntity.create(memberId));
        entity.update(request.preferredCategories(), request.avoid(), request.vibe(), request.intensity());
        return preferenceRepository.save(entity).toDomain();
    }

    /** 설문 조회. 미저장 유저는 빈 프로필로 응답(개인화는 콜드스타트 허용). */
    public MemberPreference get(UUID memberId) {
        return preferenceRepository.findById(memberId)
                .map(MemberPreferenceEntity::toDomain)
                .orElseGet(() -> new MemberPreference(memberId, List.of(), List.of(), null, null));
    }
}
