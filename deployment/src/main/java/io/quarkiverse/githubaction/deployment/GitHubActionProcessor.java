package io.quarkiverse.githubaction.deployment;

import static io.quarkiverse.githubaction.deployment.GitHubActionDotNames.ACTION;
import static io.quarkiverse.githubaction.deployment.GitHubActionDotNames.CONFIG_FILE;
import static io.quarkiverse.githubaction.deployment.GitHubActionDotNames.EVENT;
import static io.quarkiverse.githubaction.deployment.GitHubActionDotNames.INJECTABLE_TYPES;
import static io.quarkus.gizmo.Type.classType;
import static io.quarkus.gizmo.Type.parameterizedType;

import java.io.PrintStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import jakarta.enterprise.event.Event;
import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.jboss.jandex.AnnotationInstance;
import org.jboss.jandex.AnnotationTarget.Kind;
import org.jboss.jandex.AnnotationValue;
import org.jboss.jandex.ClassInfo;
import org.jboss.jandex.DotName;
import org.jboss.jandex.IndexView;
import org.jboss.jandex.MethodInfo;
import org.jboss.jandex.MethodParameterInfo;
import org.jboss.jandex.Type;
import org.jboss.logging.Logger;
import org.kohsuke.github.GHEventPayload;
import org.kohsuke.github.GitHub;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import io.quarkiverse.githubaction.Action;
import io.quarkiverse.githubaction.Action.ActionLiteral;
import io.quarkiverse.githubaction.Context;
import io.quarkiverse.githubaction.deployment.DispatchingConfiguration.ActionDispatchingConfiguration;
import io.quarkiverse.githubaction.deployment.DispatchingConfiguration.ActionDispatchingMethod;
import io.quarkiverse.githubaction.deployment.DispatchingConfiguration.EventAnnotation;
import io.quarkiverse.githubaction.deployment.DispatchingConfiguration.EventAnnotationLiteral;
import io.quarkiverse.githubaction.runtime.ConfigFileReader;
import io.quarkiverse.githubaction.runtime.GitHubEvent;
import io.quarkiverse.githubaction.runtime.GitHubEventHandler;
import io.quarkiverse.githubaction.runtime.Multiplexer;
import io.quarkiverse.githubaction.runtime.PayloadTypeResolver;
import io.quarkiverse.githubapi.deployment.GitHubApiClassWithBridgeMethodsBuildItem;
import io.quarkiverse.githubapp.event.Actions;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmoAdaptor;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.arc.processor.MethodDescriptors;
import io.quarkus.deployment.GeneratedClassGizmoAdaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.LaunchModeBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyBuildItem;
import io.quarkus.gizmo.AnnotatedElement;
import io.quarkus.gizmo.BytecodeCreator;
import io.quarkus.gizmo.ClassCreator;
import io.quarkus.gizmo.ClassOutput;
import io.quarkus.gizmo.FieldCreator;
import io.quarkus.gizmo.FieldDescriptor;
import io.quarkus.gizmo.Gizmo;
import io.quarkus.gizmo.MethodCreator;
import io.quarkus.gizmo.MethodDescriptor;
import io.quarkus.gizmo.ResultHandle;
import io.quarkus.gizmo.SignatureBuilder;
import io.quarkus.runtime.util.HashUtil;

class GitHubActionProcessor {

    private static final Logger LOG = Logger.getLogger(GitHubActionProcessor.class);

    private static final String FEATURE = "github-action";

    private static final String EVENT_EMITTER_FIELD = "eventEmitter";

    private static final MethodDescriptor GITHUB_EVENT_GET_NAME = MethodDescriptor.ofMethod(GitHubEvent.class, "getName",
            String.class);
    private static final MethodDescriptor GITHUB_EVENT_GET_EVENT = MethodDescriptor.ofMethod(GitHubEvent.class, "getEvent",
            String.class);
    private static final MethodDescriptor GITHUB_EVENT_GET_EVENT_ACTION = MethodDescriptor.ofMethod(GitHubEvent.class,
            "getEventAction", String.class);

    private static final MethodDescriptor EVENT_SELECT = MethodDescriptor.ofMethod(Event.class, "select", Event.class,
            Annotation[].class);
    private static final MethodDescriptor EVENT_FIRE = MethodDescriptor.ofMethod(Event.class, "fire",
            void.class, Object.class);

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void silentStartup(BuildProducer<RunTimeConfigurationDefaultBuildItem> configurationDefaults) {
        configurationDefaults.produce(new RunTimeConfigurationDefaultBuildItem("quarkus.log.level", "WARNING"));
        configurationDefaults.produce(new RunTimeConfigurationDefaultBuildItem("quarkus.banner.enabled", "false"));
    }

    @BuildStep
    void registerForReflection(CombinedIndexBuildItem combinedIndex,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses,
            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchies) {
        // Types used for config files
        for (AnnotationInstance configFileAnnotationInstance : combinedIndex.getIndex().getAnnotations(CONFIG_FILE)) {
            MethodParameterInfo methodParameter = configFileAnnotationInstance.target().asMethodParameter();
            short parameterPosition = methodParameter.position();
            Type parameterType = methodParameter.method().parameterTypes().get(parameterPosition);
            reflectiveHierarchies.produce(new ReflectiveHierarchyBuildItem.Builder()
                    .type(parameterType)
                    .index(combinedIndex.getIndex())
                    .source(GitHubActionProcessor.class.getSimpleName() + " > " + methodParameter.method().declaringClass()
                            + "#"
                            + methodParameter.method())
                    .build());
        }
    }

    /**
     * The bridge methods added for binary compatibility in the GitHub API are causing issues with Mockito
     * and more specifically with Byte Buddy (see https://github.com/raphw/byte-buddy/issues/1162).
     * They don't bring much to the plate for new applications that are regularly updated so let's remove them altogether.
     */
    @BuildStep
    void removeCompatibilityBridgeMethodsFromGitHubApi(
            BuildProducer<BytecodeTransformerBuildItem> bytecodeTransformers,
            List<GitHubApiClassWithBridgeMethodsBuildItem> gitHubApiClassesWithBridgeMethods) {
        for (GitHubApiClassWithBridgeMethodsBuildItem gitHubApiClassWithBridgeMethods : gitHubApiClassesWithBridgeMethods) {
            bytecodeTransformers.produce(new BytecodeTransformerBuildItem.Builder()
                    .setClassToTransform(gitHubApiClassWithBridgeMethods.getClassName())
                    .setVisitorFunction((ignored, visitor) -> new RemoveBridgeMethodsClassVisitor(visitor,
                            gitHubApiClassWithBridgeMethods.getClassName(),
                            gitHubApiClassWithBridgeMethods.getMethodsWithBridges()))
                    .build());
        }
    }

    @BuildStep
    void generateClasses(CombinedIndexBuildItem combinedIndex, LaunchModeBuildItem launchMode,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            BuildProducer<GeneratedBeanBuildItem> generatedBeans,
            BuildProducer<GeneratedClassBuildItem> generatedClasses,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses,
            BuildProducer<AnnotationsTransformerBuildItem> annotationsTransformer) {
        Collection<EventDefinition> allEventDefinitions = getAllEventDefinitions(combinedIndex.getIndex());

        // Add @Vetoed to all the user-defined event listening classes
        annotationsTransformer
                .produce(new AnnotationsTransformerBuildItem(new VetoUserDefinedEventListeningClassesAnnotationsTransformer(
                        allEventDefinitions.stream().map(d -> d.getAnnotation()).collect(Collectors.toSet()))));

        // Add the qualifiers as beans
        String[] subscriberAnnotations = allEventDefinitions.stream().map(d -> d.getAnnotation().toString())
                .toArray(String[]::new);
        additionalBeans.produce(new AdditionalBeanBuildItem(subscriberAnnotations));
        additionalBeans.produce(new AdditionalBeanBuildItem(Action.class));

        DispatchingConfiguration dispatchingConfiguration = getDispatchingConfiguration(
                combinedIndex.getIndex(), allEventDefinitions);

        ClassOutput classOutput = new GeneratedClassGizmoAdaptor(generatedClasses, true);
        generateAnnotationLiterals(classOutput, dispatchingConfiguration);

        ClassOutput beanClassOutput = new GeneratedBeanGizmoAdaptor(generatedBeans);
        generatePayloadTypeResolver(beanClassOutput, reflectiveClasses, allEventDefinitions);
        generateActionMain(beanClassOutput, combinedIndex, launchMode, dispatchingConfiguration, reflectiveClasses);
        generateMultiplexers(beanClassOutput, dispatchingConfiguration, reflectiveClasses);
    }

    private static Collection<EventDefinition> getAllEventDefinitions(IndexView index) {
        Collection<EventDefinition> mainEventDefinitions = new ArrayList<>();
        Collection<EventDefinition> allEventDefinitions = new ArrayList<>();

        for (AnnotationInstance eventInstance : index.getAnnotations(EVENT)) {
            if (eventInstance.target().kind() == Kind.CLASS) {
                mainEventDefinitions.add(new EventDefinition(eventInstance.target().asClass().name(),
                        eventInstance.value("name").asString(),
                        null,
                        eventInstance.value("payload").asClass().name()));
            }
        }

        allEventDefinitions.addAll(mainEventDefinitions);

        for (EventDefinition mainEventDefinition : mainEventDefinitions) {
            for (AnnotationInstance eventInstance : index.getAnnotations(mainEventDefinition.getAnnotation())) {
                if (eventInstance.target().kind() == Kind.CLASS) {
                    AnnotationValue actionValue = eventInstance.value();

                    allEventDefinitions.add(new EventDefinition(eventInstance.target().asClass().name(),
                            mainEventDefinition.getEvent(),
                            actionValue != null ? actionValue.asString() : null,
                            mainEventDefinition.getPayloadType()));
                }
            }
        }

        return allEventDefinitions;
    }

    private static DispatchingConfiguration getDispatchingConfiguration(
            IndexView index, Collection<EventDefinition> allEventDefinitions) {
        DispatchingConfiguration configuration = new DispatchingConfiguration();

        Collection<AnnotationInstance> actionInstances = index.getAnnotations(ACTION)
                .stream()
                .filter(ai -> ai.target().kind() == Kind.METHOD)
                .collect(Collectors.toList());
        for (AnnotationInstance actionInstance : actionInstances) {
            String name = actionInstance.valueWithDefault(index).asString();

            MethodInfo methodInfo = actionInstance.target().asMethod();
            boolean hasEventListeners = false;
            for (EventDefinition eventDefinition : allEventDefinitions) {
                Collection<AnnotationInstance> eventSubscriberInstances = methodInfo
                        .annotations(eventDefinition.getAnnotation());

                for (AnnotationInstance eventSubscriberInstance : eventSubscriberInstances) {
                    String action = eventDefinition.getAction() != null ? eventDefinition.getAction()
                            : (eventSubscriberInstance.value() != null ? eventSubscriberInstance.value().asString()
                                    : Actions.ALL);

                    MethodParameterInfo annotatedParameter = eventSubscriberInstance.target().asMethodParameter();
                    DotName annotatedParameterType = annotatedParameter.method().parameterTypes()
                            .get(annotatedParameter.position())
                            .name();
                    if (!eventDefinition.getPayloadType().equals(annotatedParameterType)) {
                        throw new IllegalStateException(
                                "Parameter subscribing to a GitHub '" + eventDefinition.getEvent()
                                        + "' event should be of type '" + eventDefinition.getPayloadType()
                                        + "'. Offending method: " + methodInfo.declaringClass().name() + "#" + methodInfo);
                    }

                    configuration
                            .getOrCreateActionConfiguration(name, eventDefinition.getEvent(),
                                    eventDefinition.getPayloadType().toString())
                            .addEventAnnotation(action, eventSubscriberInstance,
                                    eventSubscriberInstance.valuesWithDefaults(index));
                    configuration.addActionDispatchingMethod(
                            new ActionDispatchingMethod(name, eventSubscriberInstance, methodInfo));

                    hasEventListeners = true;
                }
            }

            if (!hasEventListeners) {
                configuration.getOrCreateActionConfiguration(name, EventDefinition.ALL, null);
                configuration.addActionDispatchingMethod(new ActionDispatchingMethod(name, null, methodInfo));
            }
        }

        // Methods listening to an event but with no @Action annotation, we consider them unnamed.
        for (EventDefinition eventDefinition : allEventDefinitions) {
            Collection<AnnotationInstance> eventSubscriberInstances = index.getAnnotations(eventDefinition.getAnnotation())
                    .stream()
                    .filter(ai -> ai.target().kind() == Kind.METHOD_PARAMETER)
                    .filter(ai -> !ai.target().asMethodParameter().method().hasAnnotation(ACTION))
                    .collect(Collectors.toList());
            for (AnnotationInstance eventSubscriberInstance : eventSubscriberInstances) {
                MethodParameterInfo annotatedParameter = eventSubscriberInstance.target().asMethodParameter();
                MethodInfo methodInfo = annotatedParameter.method();
                String name = Action.UNNAMED;

                String action = eventDefinition.getAction() != null ? eventDefinition.getAction()
                        : (eventSubscriberInstance.value() != null ? eventSubscriberInstance.value().asString() : Actions.ALL);
                DotName annotatedParameterType = annotatedParameter.method().parameterTypes().get(annotatedParameter.position())
                        .name();
                if (!eventDefinition.getPayloadType().equals(annotatedParameterType)) {
                    throw new IllegalStateException(
                            "Parameter subscribing to a GitHub '" + eventDefinition.getEvent()
                                    + "' action should be of type '" + eventDefinition.getPayloadType()
                                    + "'. Offending method: " + methodInfo.declaringClass().name() + "#" + methodInfo);
                }

                configuration
                        .getOrCreateActionConfiguration(name, eventDefinition.getEvent(),
                                eventDefinition.getPayloadType().toString())
                        .addEventAnnotation(action, eventSubscriberInstance, eventSubscriberInstance.valuesWithDefaults(index));
                configuration.addActionDispatchingMethod(
                        new ActionDispatchingMethod(name, eventSubscriberInstance, methodInfo));
            }
        }

        return configuration;
    }

    private static void generateAnnotationLiterals(ClassOutput classOutput, DispatchingConfiguration dispatchingConfiguration) {
        for (ActionDispatchingConfiguration eventDispatchingConfiguration : dispatchingConfiguration
                .getActionDispatchingConfigurations()) {
            for (EventAnnotationLiteral eventAnnotationLiteral : eventDispatchingConfiguration.getEventAnnotationLiterals()) {
                String literalClassName = getLiteralClassName(eventAnnotationLiteral.getName());

                String signature = SignatureBuilder.forClass()
                        .setSuperClass(parameterizedType(classType(AnnotationLiteral.class),
                                classType(eventAnnotationLiteral.getName())))
                        .addInterface(classType(eventAnnotationLiteral.getName()))
                        .build();

                ClassCreator literalClassCreator = ClassCreator.builder().classOutput(classOutput)
                        .className(literalClassName)
                        .signature(signature)
                        .superClass(AnnotationLiteral.class)
                        .interfaces(eventAnnotationLiteral.getName().toString())
                        .build();

                Class<?>[] parameterTypes = new Class<?>[eventAnnotationLiteral.getAttributes().size()];
                Arrays.fill(parameterTypes, String.class);

                MethodCreator constructorCreator = literalClassCreator.getMethodCreator("<init>", "V",
                        (Object[]) parameterTypes);
                constructorCreator.invokeSpecialMethod(MethodDescriptor.ofConstructor(AnnotationLiteral.class),
                        constructorCreator.getThis());
                for (int i = 0; i < eventAnnotationLiteral.getAttributes().size(); i++) {
                    constructorCreator.writeInstanceField(
                            FieldDescriptor.of(literalClassName, eventAnnotationLiteral.getAttributes().get(i), String.class),
                            constructorCreator.getThis(), constructorCreator.getMethodParam(i));
                    constructorCreator.setModifiers(Modifier.PUBLIC);
                }
                constructorCreator.returnValue(null);

                for (String attribute : eventAnnotationLiteral.getAttributes()) {
                    // we only support String for now
                    literalClassCreator.getFieldCreator(attribute, String.class)
                            .setModifiers(Modifier.PRIVATE);
                    MethodCreator getterCreator = literalClassCreator.getMethodCreator(attribute, String.class);
                    getterCreator.setModifiers(Modifier.PUBLIC);
                    getterCreator.returnValue(getterCreator.readInstanceField(
                            FieldDescriptor.of(literalClassName, attribute, String.class), getterCreator.getThis()));
                }

                literalClassCreator.close();
            }
        }
    }

    private static void generatePayloadTypeResolver(ClassOutput beanClassOutput,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses,
            Collection<EventDefinition> eventDefinitions) {
        String payloadTypeResolverClassName = GitHubEvent.class.getPackageName() + ".PayloadTypeResolverImpl";

        reflectiveClasses.produce(new ReflectiveClassBuildItem(true, true, payloadTypeResolverClassName));

        ClassCreator payloadTypeResolverClassCreator = ClassCreator.builder().classOutput(beanClassOutput)
                .className(payloadTypeResolverClassName)
                .interfaces(PayloadTypeResolver.class)
                .build();

        payloadTypeResolverClassCreator.addAnnotation(Singleton.class);

        Map<String, DotName> payloadTypeMapping = eventDefinitions.stream()
                .collect(Collectors.toMap(ed -> ed.getEvent(), ed -> ed.getPayloadType(), (ed1, ed2) -> ed1));

        MethodCreator getPayloadTypeMethodCreator = payloadTypeResolverClassCreator.getMethodCreator("getPayloadType",
                Class.class, String.class);

        ResultHandle eventRh = getPayloadTypeMethodCreator.getMethodParam(0);

        for (Entry<String, DotName> payloadTypeMappingEntry : payloadTypeMapping.entrySet()) {
            BytecodeCreator matches = getPayloadTypeMethodCreator.ifTrue(getPayloadTypeMethodCreator.invokeVirtualMethod(
                    MethodDescriptors.OBJECT_EQUALS, getPayloadTypeMethodCreator.load(payloadTypeMappingEntry.getKey()),
                    eventRh))
                    .trueBranch();
            matches.returnValue(matches.loadClass(payloadTypeMappingEntry.getValue().toString()));
        }

        getPayloadTypeMethodCreator.returnValue(getPayloadTypeMethodCreator.loadNull());

        payloadTypeResolverClassCreator.close();
    }

    /**
     * This method generates the <code>@QuarkusMain</code> class.
     * <p>
     * It emits the GitHub events as CDI events that will then be caught by the multiplexers.
     */
    private static void generateActionMain(ClassOutput beanClassOutput,
            CombinedIndexBuildItem combinedIndex,
            LaunchModeBuildItem launchMode,
            DispatchingConfiguration dispatchingConfiguration,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {
        String gitHubEventHandlerClassName = GitHubEventHandler.class.getName() + "Impl";

        reflectiveClasses.produce(new ReflectiveClassBuildItem(true, true, gitHubEventHandlerClassName));

        ClassCreator gitHubEventHandlerClassCreator = ClassCreator.builder().classOutput(beanClassOutput)
                .className(gitHubEventHandlerClassName)
                .interfaces(GitHubEventHandler.class)
                .build();
        gitHubEventHandlerClassCreator.addAnnotation(Singleton.class);

        FieldCreator eventFieldCreator = gitHubEventHandlerClassCreator.getFieldCreator(EVENT_EMITTER_FIELD, Event.class);
        eventFieldCreator.addAnnotation(Inject.class);
        eventFieldCreator.setModifiers(Modifier.PROTECTED);
        eventFieldCreator.setSignature(SignatureBuilder.forField()
                .setType(parameterizedType(classType(Event.class), classType(GitHubEvent.class)))
                .build());

        MethodCreator handleMethodCreator = gitHubEventHandlerClassCreator.getMethodCreator(
                "handle",
                void.class,
                GitHubEvent.class);
        handleMethodCreator.setModifiers(Modifier.PUBLIC);

        ResultHandle gitHubEventRh = handleMethodCreator.getMethodParam(0);
        ResultHandle dispatchedNameRh = handleMethodCreator.invokeVirtualMethod(GITHUB_EVENT_GET_NAME, gitHubEventRh);
        ResultHandle dispatchedEventRh = handleMethodCreator.invokeVirtualMethod(GITHUB_EVENT_GET_EVENT, gitHubEventRh);
        ResultHandle dispatchedActionRh = handleMethodCreator.invokeVirtualMethod(GITHUB_EVENT_GET_EVENT_ACTION,
                gitHubEventRh);

        for (Entry<String, Map<String, ActionDispatchingConfiguration>> actionConfigurationEntry : dispatchingConfiguration
                .getActionConfigurations().entrySet()) {
            String name = actionConfigurationEntry.getKey();
            Map<String, ActionDispatchingConfiguration> actionConfiguration = actionConfigurationEntry.getValue();

            BytecodeCreator nameMatchesCreator = handleMethodCreator
                    .ifTrue(handleMethodCreator.invokeVirtualMethod(MethodDescriptors.OBJECT_EQUALS,
                            handleMethodCreator.load(name),
                            dispatchedNameRh))
                    .trueBranch();

            ResultHandle actionAnnotationLiteralRh = nameMatchesCreator.newInstance(MethodDescriptor
                    .ofConstructor(ActionLiteral.class, String.class),
                    new ResultHandle[] { nameMatchesCreator.load(name) });

            for (Entry<String, ActionDispatchingConfiguration> eventConfigurationEntry : actionConfiguration.entrySet()) {
                String event = eventConfigurationEntry.getKey();
                ActionDispatchingConfiguration eventDispatchingConfiguration = eventConfigurationEntry.getValue();

                if (EventDefinition.ALL.equals(event)) {
                    ResultHandle annotationLiteralArrayRh = nameMatchesCreator.newArray(Annotation.class, 1);
                    nameMatchesCreator.writeArrayValue(annotationLiteralArrayRh, 0, actionAnnotationLiteralRh);

                    fireEvent(nameMatchesCreator, gitHubEventHandlerClassCreator.getClassName(),
                            gitHubEventRh, annotationLiteralArrayRh);

                    continue;
                }

                BytecodeCreator eventMatchesCreator = nameMatchesCreator
                        .ifTrue(nameMatchesCreator.invokeVirtualMethod(MethodDescriptors.OBJECT_EQUALS,
                                nameMatchesCreator.load(event),
                                dispatchedEventRh))
                        .trueBranch();

                for (Entry<String, EventAnnotation> eventAnnotationEntry : eventDispatchingConfiguration.getEventAnnotations()
                        .entrySet()) {
                    String action = eventAnnotationEntry.getKey();
                    EventAnnotation eventAnnotation = eventAnnotationEntry.getValue();

                    Class<?>[] literalParameterTypes = new Class<?>[eventAnnotation.getValues().size()];
                    Arrays.fill(literalParameterTypes, String.class);
                    List<ResultHandle> literalParameters = new ArrayList<>();
                    for (AnnotationValue eventAnnotationValue : eventAnnotation.getValues()) {
                        literalParameters.add(eventMatchesCreator.load(eventAnnotationValue.asString()));
                    }

                    ResultHandle eventAnnotationLiteralRh = eventMatchesCreator.newInstance(MethodDescriptor
                            .ofConstructor(getLiteralClassName(eventAnnotation.getName()), (Object[]) literalParameterTypes),
                            literalParameters.toArray(ResultHandle[]::new));
                    ResultHandle annotationLiteralArrayRh = eventMatchesCreator.newArray(Annotation.class, 2);
                    eventMatchesCreator.writeArrayValue(annotationLiteralArrayRh, 0, actionAnnotationLiteralRh);
                    eventMatchesCreator.writeArrayValue(annotationLiteralArrayRh, 1, eventAnnotationLiteralRh);

                    if (Actions.ALL.equals(action)) {
                        fireEvent(eventMatchesCreator, gitHubEventHandlerClassCreator.getClassName(),
                                gitHubEventRh, annotationLiteralArrayRh);
                    } else {
                        BytecodeCreator actionMatchesCreator = eventMatchesCreator
                                .ifTrue(eventMatchesCreator.invokeVirtualMethod(MethodDescriptors.OBJECT_EQUALS,
                                        eventMatchesCreator.load(action), dispatchedActionRh))
                                .trueBranch();

                        fireEvent(actionMatchesCreator, gitHubEventHandlerClassCreator.getClassName(),
                                gitHubEventRh, annotationLiteralArrayRh);
                    }
                }
            }
        }

        handleMethodCreator.returnValue(null);

        gitHubEventHandlerClassCreator.close();
    }

    /**
     * Multiplexers listen to the sync events emitted by the main class.
     * <p>
     * They are subclasses of the application classes registering actions or listening to GitHub events through our annotations.
     * <p>
     * They are useful for several purposes:
     * <ul>
     * <li>A single application method can listen to multiple event types: the event types are qualifiers and CDI wouldn't allow
     * that (only events matching all the qualifiers would be received by the application method). That's why this class is
     * called a multiplexer: it will generate one method per event type and each generated method will delegate to the original
     * method.</li>
     * <li>The multiplexer also handles the resolution of payloads, config files...</li>
     * <li>We can inject a properly configured instance of GitHub or DynamicGraphQLClient into the method.</li>
     * </ul>
     */
    private static void generateMultiplexers(ClassOutput beanClassOutput,
            DispatchingConfiguration dispatchingConfiguration,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {
        for (Entry<DotName, TreeSet<ActionDispatchingMethod>> actionDispatchingMethodsEntry : dispatchingConfiguration
                .getMethods().entrySet()) {
            DotName declaringClassName = actionDispatchingMethodsEntry.getKey();
            TreeSet<ActionDispatchingMethod> actionDispatchingMethods = actionDispatchingMethodsEntry.getValue();
            ClassInfo declaringClass = actionDispatchingMethods.iterator().next().getMethod().declaringClass();

            reflectiveClasses.produce(new ReflectiveClassBuildItem(true, true, declaringClassName.toString()));

            String multiplexerClassName = declaringClassName + "_Multiplexer";
            reflectiveClasses.produce(new ReflectiveClassBuildItem(true, true, multiplexerClassName));

            ClassCreator multiplexerClassCreator = ClassCreator.builder().classOutput(beanClassOutput)
                    .className(multiplexerClassName)
                    .superClass(declaringClassName.toString())
                    .build();

            multiplexerClassCreator.addAnnotation(Multiplexer.class);

            if (!BuiltinScope.isDeclaredOn(declaringClass)) {
                multiplexerClassCreator.addAnnotation(Singleton.class);
            }

            for (AnnotationInstance classAnnotation : declaringClass.declaredAnnotations()) {
                multiplexerClassCreator.addAnnotation(classAnnotation);
            }

            // Copy the constructors
            for (MethodInfo originalConstructor : declaringClass.constructors()) {
                MethodCreator constructorCreator = multiplexerClassCreator.getMethodCreator(MethodDescriptor.ofConstructor(
                        multiplexerClassName,
                        originalConstructor.parameterTypes().stream().map(t -> t.name().toString()).toArray(String[]::new)));

                List<AnnotationInstance> originalMethodAnnotations = originalConstructor.annotations().stream()
                        .filter(ai -> ai.target().kind() == Kind.METHOD).collect(Collectors.toList());
                for (AnnotationInstance originalMethodAnnotation : originalMethodAnnotations) {
                    constructorCreator.addAnnotation(originalMethodAnnotation);
                }

                Map<Short, List<AnnotationInstance>> originalConstructorParameterAnnotationMapping = originalConstructor
                        .annotations().stream()
                        .filter(ai -> ai.target().kind() == Kind.METHOD_PARAMETER)
                        .collect(Collectors.groupingBy(ai -> ai.target().asMethodParameter().position()));

                List<ResultHandle> parametersRh = new ArrayList<>();
                for (short i = 0; i < originalConstructor.parameterTypes().size(); i++) {
                    parametersRh.add(constructorCreator.getMethodParam(i));

                    AnnotatedElement parameterAnnotations = constructorCreator.getParameterAnnotations(i);
                    List<AnnotationInstance> originalConstructorParameterAnnotations = originalConstructorParameterAnnotationMapping
                            .getOrDefault(i, Collections.emptyList());
                    for (AnnotationInstance originalConstructorParameterAnnotation : originalConstructorParameterAnnotations) {
                        parameterAnnotations.addAnnotation(originalConstructorParameterAnnotation);
                    }
                }

                constructorCreator.invokeSpecialMethod(MethodDescriptor.of(originalConstructor), constructorCreator.getThis(),
                        parametersRh.toArray(ResultHandle[]::new));
                constructorCreator.returnValue(null);
            }

            // Generate the multiplexed event dispatching methods
            for (ActionDispatchingMethod actionDispatchingMethod : actionDispatchingMethods) {
                String name = actionDispatchingMethod.getName();
                AnnotationInstance eventSubscriberInstance = actionDispatchingMethod.getEventSubscriberInstance();
                MethodInfo originalMethod = actionDispatchingMethod.getMethod();
                Map<Short, List<AnnotationInstance>> originalMethodParameterAnnotationMapping = originalMethod.annotations()
                        .stream()
                        .filter(ai -> ai.target().kind() == Kind.METHOD_PARAMETER)
                        .collect(Collectors.groupingBy(ai -> ai.target().asMethodParameter().position()));

                // if the method already has an @Observes or @ObservesAsync annotation
                if (originalMethod.hasAnnotation(DotNames.OBSERVES) || originalMethod.hasAnnotation(DotNames.OBSERVES_ASYNC)) {
                    LOG.warn(
                            "Methods listening to GitHub actions may not be annotated with @Observes or @ObservesAsync. Offending method: "
                                    + originalMethod.declaringClass().name() + "#" + originalMethod);
                }

                List<String> parameterTypes = new ArrayList<>();
                List<Type> originalMethodParameterTypes = originalMethod.parameterTypes();

                // detect the parameter that is a payload
                short payloadParameterPosition = -1;
                if (eventSubscriberInstance != null) {
                    for (short i = 0; i < originalMethodParameterTypes.size(); i++) {
                        List<AnnotationInstance> parameterAnnotations = originalMethodParameterAnnotationMapping.getOrDefault(i,
                                Collections.emptyList());
                        if (parameterAnnotations.stream().anyMatch(ai -> ai.name().equals(eventSubscriberInstance.name()))) {
                            payloadParameterPosition = i;
                            break;
                        }
                    }
                }

                short j = 0;
                Map<Short, Short> parameterMapping = new HashMap<>();
                for (short i = 0; i < originalMethodParameterTypes.size(); i++) {
                    List<AnnotationInstance> originalMethodAnnotations = originalMethodParameterAnnotationMapping
                            .getOrDefault(i, Collections.emptyList());
                    if (originalMethodAnnotations.stream().anyMatch(ai -> CONFIG_FILE.equals(ai.name())) ||
                            INJECTABLE_TYPES.contains(originalMethodParameterTypes.get(i).name()) ||
                            i == payloadParameterPosition) {
                        // if the parameter is annotated with @ConfigFile, is of an injectable type or is the payload, we skip it
                        continue;
                    }

                    parameterTypes.add(originalMethodParameterTypes.get(i).name().toString());
                    parameterMapping.put(i, j);
                    j++;
                }
                int configFileReaderParameterPosition = -1;
                if (originalMethod.hasAnnotation(CONFIG_FILE)) {
                    parameterTypes.add(ConfigFileReader.class.getName());
                    configFileReaderParameterPosition = j;
                    j++;
                }
                parameterTypes.add(GitHubEvent.class.getName());
                int gitHubEventParameterPosition = j;

                MethodCreator methodCreator = multiplexerClassCreator.getMethodCreator(
                        originalMethod.name() + "_"
                                + HashUtil.sha1(originalMethod.toString() + "_" + (eventSubscriberInstance != null
                                        ? eventSubscriberInstance.toString()
                                        : EventDefinition.ALL)),
                        originalMethod.returnType().name().toString(),
                        parameterTypes.toArray());

                for (Type exceptionType : originalMethod.exceptions()) {
                    methodCreator.addException(exceptionType.name().toString());
                }

                ResultHandle[] parameterValues = new ResultHandle[originalMethod.parameterTypes().size()];

                // copy annotations except for @ConfigFile
                for (short i = 0; i < originalMethodParameterTypes.size(); i++) {
                    List<AnnotationInstance> parameterAnnotations = originalMethodParameterAnnotationMapping.getOrDefault(i,
                            Collections.emptyList());
                    if (parameterAnnotations.isEmpty()) {
                        continue;
                    }

                    // Elements that are not in the mapping are ignored
                    Short generatedParameterIndex = parameterMapping.get(i);
                    if (generatedParameterIndex == null) {
                        continue;
                    }

                    AnnotatedElement generatedParameterAnnotations = methodCreator
                            .getParameterAnnotations(generatedParameterIndex);
                    for (AnnotationInstance annotationInstance : parameterAnnotations) {
                        generatedParameterAnnotations.addAnnotation(annotationInstance);
                    }
                }

                // add annotations to the GitHubEvent parameter
                AnnotatedElement gitHubEventParameterAnnotations = methodCreator
                        .getParameterAnnotations(gitHubEventParameterPosition);
                gitHubEventParameterAnnotations.addAnnotation(DotNames.OBSERVES.toString());
                gitHubEventParameterAnnotations.addAnnotation(Action.class).addValue("value", name);
                if (eventSubscriberInstance != null) {
                    gitHubEventParameterAnnotations.addAnnotation(eventSubscriberInstance);
                }

                ResultHandle gitHubEventRh = methodCreator.getMethodParam(gitHubEventParameterPosition);

                // generate the code of the method
                for (short originalMethodParameterIndex = 0; originalMethodParameterIndex < originalMethodParameterTypes
                        .size(); originalMethodParameterIndex++) {
                    List<AnnotationInstance> parameterAnnotations = originalMethodParameterAnnotationMapping.getOrDefault(
                            originalMethodParameterIndex,
                            Collections.emptyList());
                    Short multiplexerMethodParameterIndex = parameterMapping.get(originalMethodParameterIndex);
                    if (originalMethodParameterIndex == payloadParameterPosition) {
                        parameterValues[originalMethodParameterIndex] = methodCreator.invokeVirtualMethod(
                                MethodDescriptor.ofMethod(GitHubEvent.class, "getPayload", GHEventPayload.class),
                                gitHubEventRh);
                    } else if (INJECTABLE_TYPES
                            .contains(originalMethodParameterTypes.get(originalMethodParameterIndex).name())) {
                        DotName injectableType = originalMethodParameterTypes.get(originalMethodParameterIndex).name();
                        parameterValues[originalMethodParameterIndex] = methodCreator.invokeVirtualMethod(
                                MethodDescriptor.ofMethod(GitHubEvent.class, "get" + injectableType.withoutPackagePrefix(),
                                        injectableType.toString()),
                                gitHubEventRh);
                    } else if (parameterAnnotations.stream().anyMatch(ai -> ai.name().equals(CONFIG_FILE))) {
                        AnnotationInstance configFileAnnotationInstance = parameterAnnotations.stream()
                                .filter(ai -> ai.name().equals(CONFIG_FILE)).findFirst().get();
                        String configObjectType = originalMethodParameterTypes.get(originalMethodParameterIndex).name()
                                .toString();

                        boolean isOptional = false;
                        if (Optional.class.getName().equals(configObjectType)) {
                            if (originalMethodParameterTypes.get(originalMethodParameterIndex)
                                    .kind() != Type.Kind.PARAMETERIZED_TYPE) {
                                throw new IllegalStateException("Optional is used but not parameterized for method " +
                                        originalMethod.declaringClass().name() + "#" + originalMethod);
                            }
                            isOptional = true;
                            configObjectType = originalMethodParameterTypes.get(originalMethodParameterIndex)
                                    .asParameterizedType().arguments().get(0)
                                    .name().toString();
                        }

                        ResultHandle gitHubRh = methodCreator.invokeVirtualMethod(MethodDescriptor
                                .ofMethod(GitHubEvent.class, "getGitHub", GitHub.class),
                                gitHubEventRh);
                        ResultHandle contextRh = methodCreator.invokeVirtualMethod(MethodDescriptor
                                .ofMethod(GitHubEvent.class, "getContext", Context.class),
                                gitHubEventRh);
                        ResultHandle repositoryRh = methodCreator.invokeInterfaceMethod(MethodDescriptor
                                .ofMethod(Context.class, "getGitHubRepository", String.class),
                                contextRh);

                        ResultHandle configFileReaderRh = methodCreator.getMethodParam(configFileReaderParameterPosition);
                        ResultHandle configObject = methodCreator.invokeVirtualMethod(
                                MethodDescriptor.ofMethod(ConfigFileReader.class, "getConfigObject", Object.class,
                                        GitHub.class, String.class, String.class, Class.class),
                                configFileReaderRh,
                                gitHubRh,
                                repositoryRh,
                                methodCreator.load(configFileAnnotationInstance.value().asString()),
                                methodCreator.loadClass(configObjectType));
                        configObject = methodCreator.checkCast(configObject, configObjectType);

                        if (isOptional) {
                            configObject = methodCreator.invokeStaticMethod(
                                    MethodDescriptor.ofMethod(Optional.class, "ofNullable", Optional.class, Object.class),
                                    configObject);
                        }

                        parameterValues[originalMethodParameterIndex] = configObject;
                    } else {
                        parameterValues[originalMethodParameterIndex] = methodCreator
                                .getMethodParam(multiplexerMethodParameterIndex);
                    }
                }

                ResultHandle returnValue = methodCreator.invokeVirtualMethod(originalMethod, methodCreator.getThis(),
                        parameterValues);
                methodCreator.returnValue(returnValue);
            }

            multiplexerClassCreator.close();
        }

    }

    private static void fireEvent(BytecodeCreator bytecodeCreator, String className,
            ResultHandle gitHubEventRh, ResultHandle annotationLiteralArrayRh) {
        ResultHandle cdiEventRh = bytecodeCreator.invokeInterfaceMethod(EVENT_SELECT,
                bytecodeCreator.readInstanceField(
                        FieldDescriptor.of(className, EVENT_EMITTER_FIELD, Event.class),
                        bytecodeCreator.getThis()),
                annotationLiteralArrayRh);

        bytecodeCreator.invokeInterfaceMethod(EVENT_FIRE, cdiEventRh, gitHubEventRh);
    }

    private static String getLiteralClassName(DotName annotationName) {
        return annotationName + "_AnnotationLiteral";
    }

    @SuppressWarnings("unused")
    private static void systemOutPrintln(BytecodeCreator bytecodeCreator, ResultHandle resultHandle) {
        bytecodeCreator.invokeVirtualMethod(MethodDescriptor.ofMethod(PrintStream.class, "println", void.class, String.class),
                bytecodeCreator.readStaticField(FieldDescriptor.of(System.class, "out", PrintStream.class)),
                bytecodeCreator.invokeVirtualMethod(MethodDescriptor.ofMethod(Object.class, "toString", String.class),
                        resultHandle));
    }

    private static class RemoveBridgeMethodsClassVisitor extends ClassVisitor {

        private static final Logger LOG = Logger.getLogger(RemoveBridgeMethodsClassVisitor.class);

        private final String className;
        private final Set<String> methodsWithBridges;

        public RemoveBridgeMethodsClassVisitor(ClassVisitor visitor, String className, Set<String> methodsWithBridges) {
            super(Gizmo.ASM_API_VERSION, visitor);

            this.className = className;
            this.methodsWithBridges = methodsWithBridges;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[] exceptions) {
            if (methodsWithBridges.contains(name) && ((access & Opcodes.ACC_BRIDGE) != 0)
                    && ((access & Opcodes.ACC_SYNTHETIC) != 0)) {

                LOG.debugf("Class %1$s - Removing method %2$s %3$s(%4$s)", className,
                        org.objectweb.asm.Type.getReturnType(descriptor), name,
                        Arrays.toString(org.objectweb.asm.Type.getArgumentTypes(descriptor)));

                return null;
            }

            return super.visitMethod(access, name, descriptor, signature, exceptions);
        }
    }
}
