package com.haeyaji.be.profile.domain;

public enum NotificationType {

    // INVITE (약속)
    MEETING_INVITE,
    MEETING_INVITE_RESPONSE,
    MEETING_CONFIRMED,
    MEETING_REMINDER,

    // INVITE (공유)
    SHARE_INVITE,
    SHARE_INVITE_RESPONSE,

    // TODO
    TODO_REMINDER,
    TODO_WEATHER_ALERT,
    TODO_SHARED_UPDATED,

    // FRIEND
    FRIEND_REQUEST,
    FRIEND_RESPONSE,
}
