package com.projects.azure.event;

public final class EventNotification {
    private final EventType type;
    private final String data;

    public EventNotification(final EventType type, final String data) {
        this.type = type;
        this.data = data;
    }

    public EventType getType() {
        return type;
    }

    public String getData() {
        return data;
    }

    @Override
    public String toString() {
        return "EventNotification{" +
                "type=" + type +
                ", data='" + data + '\'' +
                '}';
    }
}
