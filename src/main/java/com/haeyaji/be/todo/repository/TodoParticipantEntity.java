package com.haeyaji.be.todo.repository;

import com.haeyaji.be.common.jpa.UuidBaseEntity;
import com.haeyaji.be.todo.domain.InviteStatus;
import com.haeyaji.be.todo.domain.ParticipantRole;
import com.haeyaji.be.todo.domain.TodoParticipant;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * 할 일 공유 참여자 테이블(todo_participant) 매핑.
 * 소유자는 todo.member_id로만 판단하고 여기엔 OWNER 행을 두지 않는다 — 공유받은 사람만 관리.
 * 스키마에 updated_at이 없어서(created_at만 있음) MeetingParticipantEntity와 같은 패턴으로 @CreatedDate만 둔다.
 */
@Entity
@Table(name = "todo_participant", uniqueConstraints = @UniqueConstraint(
        name = "uk_todo_member", columnNames = {"todo_id", "member_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TodoParticipantEntity extends UuidBaseEntity {

    @Column(name = "todo_id", nullable = false)
    private UUID todoId;

    @Column(name = "member_id", nullable = false)
    private UUID memberId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ParticipantRole role;

    @Enumerated(EnumType.STRING)
    @Column(name = "invite_status", nullable = false, length = 20)
    private InviteStatus inviteStatus;

    @CreatedDate
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public static TodoParticipantEntity invite(UUID todoId, UUID memberId, ParticipantRole role) {
        TodoParticipantEntity entity = new TodoParticipantEntity();
        entity.todoId = todoId;
        entity.memberId = memberId;
        entity.role = role;
        entity.inviteStatus = InviteStatus.PENDING;
        return entity;
    }

    public void changeRole(ParticipantRole role) {
        this.role = role;
    }

    public void accept() {
        this.inviteStatus = InviteStatus.ACCEPTED;
    }

    public void reject() {
        this.inviteStatus = InviteStatus.REJECTED;
    }

    public TodoParticipant toDomain() {
        return new TodoParticipant(getId(), todoId, memberId, role, inviteStatus, createdAt);
    }
}
