package io.quarkiverse.githubaction.deployment;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.DotName;
import org.jboss.jandex.MethodInfo;

class DispatchingConfiguration {

    /**
     * name, event, EventDispatchingConfiguration
     */
    private final Map<String, Map<String, ActionDispatchingConfiguration>> actionConfigurations = new TreeMap<>();

    /**
     * class name, EventDispatchingMethod
     */
    private final Map<DotName, TreeSet<ActionDispatchingMethod>> methods = new TreeMap<>();

    Map<String, Map<String, ActionDispatchingConfiguration>> getActionConfigurations() {
        return actionConfigurations;
    }

    ActionDispatchingConfiguration getOrCreateActionConfiguration(String action, String event, String payloadType) {
        return actionConfigurations
                .computeIfAbsent(action, a -> new HashMap<>())
                .computeIfAbsent(event, et -> new ActionDispatchingConfiguration(event, payloadType));
    }

    Map<DotName, TreeSet<ActionDispatchingMethod>> getMethods() {
        return methods;
    }

    List<ActionDispatchingConfiguration> getActionDispatchingConfigurations() {
        return actionConfigurations.values().stream()
                .flatMap(innerMap -> innerMap.values().stream())
                .collect(Collectors.toList());
    }

    void addActionDispatchingMethod(ActionDispatchingMethod actionDispatchingMethod) {
        methods.computeIfAbsent(actionDispatchingMethod.getMethod().declaringClass().name(), k -> new TreeSet<>())
                .add(actionDispatchingMethod);
    }

    static class ActionDispatchingConfiguration {

        private final String event;

        private final String payloadType;

        private final TreeMap<String, EventAnnotation> eventAnnotations = new TreeMap<>();

        ActionDispatchingConfiguration(String event, String payloadType) {
            this.event = event;
            this.payloadType = payloadType;
        }

        String getEvent() {
            return event;
        }

        String getPayloadType() {
            return payloadType;
        }

        TreeMap<String, EventAnnotation> getEventAnnotations() {
            return eventAnnotations;
        }

        Set<EventAnnotationLiteral> getEventAnnotationLiterals() {
            Set<EventAnnotationLiteral> literals = new HashSet<>();
            for (EventAnnotation eventAnnotation : eventAnnotations.values()) {
                literals.add(new EventAnnotationLiteral(eventAnnotation.getName(),
                        eventAnnotation.getValues().stream().map(av -> av.name()).collect(Collectors.toList())));
            }
            return literals;
        }

        ActionDispatchingConfiguration addEventAnnotation(String action, AnnotationInstance annotationInstance) {
            eventAnnotations.put(action, new EventAnnotation(annotationInstance.name(), annotationInstance.values()));
            return this;
        }
    }

    static class EventAnnotation implements Comparable<EventAnnotation> {

        private final DotName name;

        private final List<AnnotationValue> values;

        EventAnnotation(DotName name, List<AnnotationValue> values) {
            this.name = name;
            this.values = values;
        }

        DotName getName() {
            return name;
        }

        List<AnnotationValue> getValues() {
            return values;
        }

        @Override
        public int compareTo(EventAnnotation other) {
            int nameCompareTo = name.compareTo(other.name);
            if (nameCompareTo != 0) {
                return nameCompareTo;
            }
            int valuesLengthCompare = Integer.compare(values.size(), other.values.size());
            if (valuesLengthCompare != 0) {
                return valuesLengthCompare;
            }
            for (int i = 0; i < values.size(); i++) {
                // we only support string for now, we can adjust laster
                int valueCompare = values.get(i).asString().compareTo(other.values.get(i).asString());
                if (valueCompare != 0) {
                    return valueCompare;
                }
            }

            return 0;
        }
    }

    static class EventAnnotationLiteral {

        private final DotName name;

        // for now, we only support string attributes
        private final List<String> attributes;

        EventAnnotationLiteral(DotName name, List<String> attributes) {
            this.name = name;
            this.attributes = attributes;
        }

        DotName getName() {
            return name;
        }

        List<String> getAttributes() {
            return attributes;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (obj == null) {
                return false;
            }
            if (getClass() != obj.getClass()) {
                return false;
            }
            EventAnnotationLiteral other = (EventAnnotationLiteral) obj;

            return Objects.equals(name, other.name) &&
                    Objects.equals(attributes, other.attributes);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, attributes);
        }
    }

    static class ActionDispatchingMethod implements Comparable<ActionDispatchingMethod> {

        private final String name;

        private final AnnotationInstance eventSubscriberInstance;

        private final MethodInfo method;

        ActionDispatchingMethod(String name, AnnotationInstance eventSubscriberInstance,
                MethodInfo method) {
            this.name = name;
            this.eventSubscriberInstance = eventSubscriberInstance;
            this.method = method;
        }

        String getName() {
            return name;
        }

        AnnotationInstance getEventSubscriberInstance() {
            return eventSubscriberInstance;
        }

        MethodInfo getMethod() {
            return method;
        }

        @Override
        public int compareTo(ActionDispatchingMethod other) {
            int classNameCompareTo = method.declaringClass().name().compareTo(other.method.declaringClass().name());
            if (classNameCompareTo != 0) {
                return classNameCompareTo;
            }

            int methodNameComparator = method.toString().compareTo(other.method.toString());
            if (methodNameComparator != 0) {
                return methodNameComparator;
            }

            int nameComparator = name.compareTo(other.name);
            if (nameComparator != 0) {
                return nameComparator;
            }

            if (eventSubscriberInstance != null) {
                return eventSubscriberInstance.toString(false).compareTo(other.eventSubscriberInstance.toString(false));
            }

            return 0;
        }
    }
}
