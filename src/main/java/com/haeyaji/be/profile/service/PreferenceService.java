package com.haeyaji.be.profile.service;

import com.haeyaji.be.profile.domain.MemberPreference;
import com.haeyaji.be.profile.domain.SurveyCategoryMapper;
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

    /**
     * 설문 저장(upsert). member 1건이므로 있으면 덮어쓰고 없으면 생성.
     *
     * <p>선호 카테고리는 <b>저장 시점에 코드로 정규화</b>한다. 설문은 한글 6분류로 묻고 학습·추천은
     * 10종 코드를 쓰는데, 받은 대로 두면 한 필드에 두 어휘가 섞여 nlp 프롬프트로 그대로 흘러간다
     * (신규 유저는 "야외", 학습이 쌓인 유저는 NATURE_WALK). 읽는 쪽마다 변환하는 대신 입구에서 한 번 맞춘다.
     */
    @Transactional
    public MemberPreference save(UUID memberId, PreferenceRequest request) {
        MemberPreferenceEntity entity = preferenceRepository.findById(memberId)
                .orElseGet(() -> MemberPreferenceEntity.create(memberId));
        entity.update(SurveyCategoryMapper.normalize(request.preferredCategories()),
                request.avoid(), request.vibe(), request.intensity());
        return preferenceRepository.save(entity).toDomain();
    }

    /** 설문 조회. 미저장 유저는 빈 프로필로 응답(개인화는 콜드스타트 허용). */
    public MemberPreference get(UUID memberId) {
        return preferenceRepository.findById(memberId)
                .map(MemberPreferenceEntity::toDomain)
                .orElseGet(() -> new MemberPreference(memberId, List.of(), List.of(), null, null));
    }
}
