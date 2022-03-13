package io.quarkiverse.githubaction.deployment;

import org.jboss.jandex.DotName;

class EventDefinition {

    static final String ALL = "<<ALL>>";

    private final DotName annotation;

    private final String event;

    private final String action;

    private final DotName payloadType;

    EventDefinition(DotName annotation, String event, String action, DotName payloadType) {
        this.annotation = annotation;
        this.event = event;
        this.action = action;
        this.payloadType = payloadType;
    }

    DotName getAnnotation() {
        return annotation;
    }

    String getEvent() {
        return event;
    }

    String getAction() {
        return action;
    }

    public DotName getPayloadType() {
        return payloadType;
    }
}