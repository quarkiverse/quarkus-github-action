package io.quarkiverse.githubaction.deployment;

import static io.quarkiverse.githubaction.deployment.GitHubActionDotNames.ACTION;
import static io.quarkiverse.githubaction.deployment.GitHubActionDotNames.CONFIG_FILE;
import static io.quarkiverse.githubaction.deployment.GitHubActionDotNames.EVENT;
import static io.quarkiverse.githubaction.deployment.GitHubActionDotNames.INJECTABLE_TYPES;
import static org.jboss.jandex.gizmo2.Jandex2Gizmo.classDescOf;

import java.lang.annotation.Annotation;
import java.lang.annotation.RetentionPolicy;
import java.lang.constant.ClassDesc;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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
import org.jboss.jandex.gizmo2.Jandex2Gizmo;
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
import io.quarkiverse.githubapp.event.Actions;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.arc.deployment.AnnotationsTransformerBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanBuildItem;
import io.quarkus.arc.deployment.GeneratedBeanGizmo2Adaptor;
import io.quarkus.arc.processor.BuiltinScope;
import io.quarkus.arc.processor.DotNames;
import io.quarkus.deployment.GeneratedClassGizmo2Adaptor;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.BytecodeTransformerBuildItem;
import io.quarkus.deployment.builditem.CombinedIndexBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.GeneratedClassBuildItem;
import io.quarkus.deployment.builditem.GeneratedResourceBuildItem;
import io.quarkus.deployment.builditem.RunTimeConfigurationDefaultBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveClassBuildItem;
import io.quarkus.deployment.builditem.nativeimage.ReflectiveHierarchyBuildItem;
import io.quarkus.gizmo2.ClassOutput;
import io.quarkus.gizmo2.Const;
import io.quarkus.gizmo2.Expr;
import io.quarkus.gizmo2.GenericType;
import io.quarkus.gizmo2.Gizmo;
import io.quarkus.gizmo2.LocalVar;
import io.quarkus.gizmo2.ParamVar;
import io.quarkus.gizmo2.TypeArgument;
import io.quarkus.gizmo2.creator.BlockCreator;
import io.quarkus.gizmo2.desc.ConstructorDesc;
import io.quarkus.gizmo2.desc.FieldDesc;
import io.quarkus.gizmo2.desc.MethodDesc;
import io.quarkus.runtime.util.HashUtil;

class GitHubActionProcessor {

    private static final Logger LOG = Logger.getLogger(GitHubActionProcessor.class);

    private static final String FEATURE = "github-action";

    private static final String EVENT_EMITTER_FIELD = "eventEmitter";

    private static final MethodDesc GITHUB_EVENT_GET_NAME = MethodDesc.of(GitHubEvent.class, "getName",
            String.class);
    private static final MethodDesc GITHUB_EVENT_GET_EVENT = MethodDesc.of(GitHubEvent.class, "getEvent",
            String.class);
    private static final MethodDesc GITHUB_EVENT_GET_EVENT_ACTION = MethodDesc.of(GitHubEvent.class,
            "getEventAction", String.class);

    private static final MethodDesc EVENT_SELECT = MethodDesc.of(Event.class, "select", Event.class,
            Annotation[].class);
    private static final MethodDesc EVENT_FIRE = MethodDesc.of(Event.class, "fire",
            void.class, Object.class);

    private static final MethodDesc OBJECT_EQUALS = MethodDesc.of(Object.class, "equals", boolean.class, Object.class);

    private static final DotName WITH_BRIDGE_METHODS = DotName
            .createSimple("com.infradna.tool.bridge_method_injector.WithBridgeMethods");

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
            BuildProducer<ReflectiveHierarchyBuildItem> reflectiveHierarchies) {
        // Types used for config files
        for (AnnotationInstance configFileAnnotationInstance : combinedIndex.getIndex().getAnnotations(CONFIG_FILE)) {
            MethodParameterInfo methodParameter = configFileAnnotationInstance.target().asMethodParameter();
            short parameterPosition = methodParameter.position();
            Type parameterType = methodParameter.method().parameterTypes().get(parameterPosition);
            reflectiveHierarchies.produce(ReflectiveHierarchyBuildItem.builder(parameterType)
                    .index(combinedIndex.getIndex())
                    .source(GitHubActionProcessor.class.getSimpleName() + " > " + methodParameter.method().declaringClass()
                            + "#"
                            + methodParameter.method())
                    .build());
        }
    }

    /**
     * The bridge methods added for binary compatibility in the GitHub API are causing issues with Mockito
     * and more specifically with Byte Buddy (see <a href="https://github.com/raphw/byte-buddy/issues/1162">...</a>).
     * They don't bring much to the plate for new applications that are regularly updated so let's remove them altogether.
     */
    @BuildStep
    void removeCompatibilityBridgeMethodsFromGitHubApi(
            CombinedIndexBuildItem combinedIndex,
            BuildProducer<BytecodeTransformerBuildItem> bytecodeTransformers) {
        Map<String, Set<String>> bridgeMethodsByClassName = new HashMap<>();

        for (AnnotationInstance bridgeAnnotation : combinedIndex.getIndex().getAnnotations(WITH_BRIDGE_METHODS)) {
            if (bridgeAnnotation.target().kind() != Kind.METHOD) {
                continue;
            }

            String className = bridgeAnnotation.target().asMethod().declaringClass().name().toString();
            bridgeMethodsByClassName.computeIfAbsent(className, cn -> new HashSet<>())
                    .add(bridgeAnnotation.target().asMethod().name());
        }

        for (Entry<String, Set<String>> bridgeMethodsByClassNameEntry : bridgeMethodsByClassName.entrySet()) {
            bytecodeTransformers.produce(new BytecodeTransformerBuildItem.Builder()
                    .setClassToTransform(bridgeMethodsByClassNameEntry.getKey())
                    .setVisitorFunction((ignored, visitor) -> new RemoveBridgeMethodsClassVisitor(visitor,
                            bridgeMethodsByClassNameEntry.getKey(),
                            bridgeMethodsByClassNameEntry.getValue()))
                    .build());
        }
    }

    @BuildStep
    void generateClasses(CombinedIndexBuildItem combinedIndex,
            BuildProducer<AdditionalBeanBuildItem> additionalBeans,
            BuildProducer<GeneratedBeanBuildItem> generatedBeans,
            BuildProducer<GeneratedClassBuildItem> generatedClasses,
            BuildProducer<GeneratedResourceBuildItem> generatedResources,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses,
            BuildProducer<AnnotationsTransformerBuildItem> annotationsTransformer) {
        IndexView index = combinedIndex.getIndex();
        Collection<EventDefinition> allEventDefinitions = getAllEventDefinitions(index);

        // Add @Vetoed to all the user-defined event listening classes
        annotationsTransformer
                .produce(new AnnotationsTransformerBuildItem(new VetoUserDefinedEventListeningClassesAnnotationsTransformer(
                        allEventDefinitions.stream().map(EventDefinition::getAnnotation).collect(Collectors.toSet()))));

        // Add the qualifiers as beans
        String[] subscriberAnnotations = allEventDefinitions.stream().map(d -> d.getAnnotation().toString())
                .toArray(String[]::new);
        additionalBeans.produce(new AdditionalBeanBuildItem(subscriberAnnotations));
        additionalBeans.produce(new AdditionalBeanBuildItem(Action.class));

        DispatchingConfiguration dispatchingConfiguration = getDispatchingConfiguration(
                index, allEventDefinitions);

        ClassOutput classOutput = new GeneratedClassGizmo2Adaptor(generatedClasses, generatedResources, true);
        generateAnnotationLiterals(classOutput, dispatchingConfiguration);

        ClassOutput beanClassOutput = new GeneratedBeanGizmo2Adaptor(generatedBeans);
        generatePayloadTypeResolver(beanClassOutput, reflectiveClasses, allEventDefinitions);
        generateActionMain(beanClassOutput, dispatchingConfiguration, reflectiveClasses);
        generateMultiplexers(beanClassOutput, index, dispatchingConfiguration, reflectiveClasses);
    }

    private static Collection<EventDefinition> getAllEventDefinitions(IndexView index) {
        Collection<EventDefinition> mainEventDefinitions = new ArrayList<>();

        for (AnnotationInstance eventInstance : index.getAnnotations(EVENT)) {
            if (eventInstance.target().kind() == Kind.CLASS) {
                mainEventDefinitions.add(new EventDefinition(eventInstance.target().asClass().name(),
                        eventInstance.value("name").asString(),
                        null,
                        eventInstance.value("payload").asClass().name()));
            }
        }

        Collection<EventDefinition> allEventDefinitions = new ArrayList<>(mainEventDefinitions);

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
                .toList();
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
                    .toList();
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
        Set<EventAnnotationLiteral> eventAnnotationLiterals = dispatchingConfiguration.getActionDispatchingConfigurations()
                .stream()
                .flatMap(c -> c.getEventAnnotationLiterals().stream())
                .collect(Collectors.toSet());

        Gizmo gizmo = Gizmo.create(classOutput).withDebugInfo(false).withParameters(false);

        for (EventAnnotationLiteral eventAnnotationLiteral : eventAnnotationLiterals) {
            String literalClassName = getLiteralClassName(eventAnnotationLiteral.getName());

            gizmo.class_(literalClassName, cc -> {
                cc.extends_(GenericType.ofClass(AnnotationLiteral.class,
                        TypeArgument.of(classDescOf(eventAnnotationLiteral.getName()))));
                cc.implements_(classDescOf(eventAnnotationLiteral.getName()));

                List<String> attributes = eventAnnotationLiteral.getAttributes();

                // Create instance fields for attributes and collect their descriptors
                List<FieldDesc> attributeFields = new ArrayList<>();
                for (String attribute : attributes) {
                    FieldDesc fieldDesc = cc.field(attribute, fc -> {
                        fc.setType(String.class);
                        fc.private_();
                    });
                    attributeFields.add(fieldDesc);
                }

                // Create constructor
                cc.constructor(constr -> {
                    constr.public_();
                    List<ParamVar> params = new ArrayList<>();
                    for (int i = 0; i < attributes.size(); i++) {
                        params.add(constr.parameter("param" + i, String.class));
                    }
                    constr.body(bc -> {
                        bc.invokeSpecial(ConstructorDesc.of(AnnotationLiteral.class), cc.this_());
                        for (int i = 0; i < attributes.size(); i++) {
                            bc.set(cc.this_().field(attributeFields.get(i)), params.get(i));
                        }
                        bc.return_();
                    });
                });

                // Create getter methods
                for (int i = 0; i < attributes.size(); i++) {
                    String attribute = attributes.get(i);
                    FieldDesc fieldDesc = attributeFields.get(i);
                    cc.method(attribute, mc -> {
                        mc.public_();
                        mc.returning(String.class);
                        mc.body(bc -> {
                            bc.return_(cc.this_().field(fieldDesc));
                        });
                    });
                }
            });
        }
    }

    private static void generatePayloadTypeResolver(ClassOutput beanClassOutput,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses,
            Collection<EventDefinition> eventDefinitions) {
        String payloadTypeResolverClassName = GitHubEvent.class.getPackageName() + ".PayloadTypeResolverImpl";

        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(payloadTypeResolverClassName)
                .constructors().methods().fields().build());

        Map<String, DotName> payloadTypeMapping = eventDefinitions.stream()
                .collect(Collectors.toMap(EventDefinition::getEvent, EventDefinition::getPayloadType, (ed1, ed2) -> ed1));

        Gizmo gizmo = Gizmo.create(beanClassOutput).withDebugInfo(false).withParameters(false);

        gizmo.class_(payloadTypeResolverClassName, cc -> {
            cc.implements_(PayloadTypeResolver.class);
            cc.addAnnotation(Singleton.class);
            cc.defaultConstructor();

            cc.method("getPayloadType", mc -> {
                mc.public_();
                mc.returning(Class.class);
                ParamVar eventParam = mc.parameter("event", String.class);

                mc.body(bc -> {
                    for (Entry<String, DotName> payloadTypeMappingEntry : payloadTypeMapping.entrySet()) {
                        bc.if_(bc.invokeVirtual(OBJECT_EQUALS,
                                Const.of(payloadTypeMappingEntry.getKey()),
                                eventParam),
                                b -> {
                                    b.return_(Const.of(ClassDesc.of(payloadTypeMappingEntry.getValue().toString())));
                                });
                    }
                    bc.return_(Const.ofNull(Class.class));
                });
            });
        });
    }

    /**
     * This method generates the <code>@QuarkusMain</code> class.
     * <p>
     * It emits the GitHub events as CDI events that will then be caught by the multiplexers.
     */
    private static void generateActionMain(ClassOutput beanClassOutput,
            DispatchingConfiguration dispatchingConfiguration,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {
        String gitHubEventHandlerClassName = GitHubEventHandler.class.getName() + "Impl";

        reflectiveClasses.produce(ReflectiveClassBuildItem.builder(gitHubEventHandlerClassName)
                .constructors().methods().fields().build());

        Gizmo gizmo = Gizmo.create(beanClassOutput).withDebugInfo(false).withParameters(false);

        gizmo.class_(gitHubEventHandlerClassName, cc -> {
            cc.implements_(GitHubEventHandler.class);
            cc.addAnnotation(Singleton.class);
            cc.defaultConstructor();

            cc.field(EVENT_EMITTER_FIELD, fc -> {
                fc.setType(GenericType.ofClass(Event.class,
                        TypeArgument.of(GitHubEvent.class)));
                fc.addAnnotation(Inject.class);
                fc.protected_();
            });

            cc.method("handle", mc -> {
                mc.public_();
                mc.returning(void.class);
                ParamVar gitHubEventParam = mc.parameter("gitHubEvent", GitHubEvent.class);

                mc.body(b0 -> {
                    Expr thisExpr = mc.this_();
                    Expr gitHubEventExpr = gitHubEventParam;

                    LocalVar dispatchedNameVar = b0.localVar("dispatchedName", String.class,
                            b0.invokeVirtual(GITHUB_EVENT_GET_NAME, gitHubEventExpr));
                    LocalVar dispatchedEventVar = b0.localVar("dispatchedEvent", String.class,
                            b0.invokeVirtual(GITHUB_EVENT_GET_EVENT, gitHubEventExpr));
                    LocalVar dispatchedActionVar = b0.localVar("dispatchedAction", String.class,
                            b0.invokeVirtual(GITHUB_EVENT_GET_EVENT_ACTION, gitHubEventExpr));

                    for (Entry<String, Map<String, ActionDispatchingConfiguration>> actionConfigurationEntry : dispatchingConfiguration
                            .getActionConfigurations().entrySet()) {
                        String name = actionConfigurationEntry.getKey();
                        Map<String, ActionDispatchingConfiguration> actionConfiguration = actionConfigurationEntry.getValue();

                        b0.if_(b0.invokeVirtual(OBJECT_EQUALS,
                                Const.of(name),
                                dispatchedNameVar),
                                nameMatchesBlock -> {

                                    LocalVar actionAnnotationLiteralVar = nameMatchesBlock.localVar(
                                            "actionAnnotationLiteral", ActionLiteral.class,
                                            nameMatchesBlock.new_(
                                                    ConstructorDesc.of(ActionLiteral.class, String.class),
                                                    Const.of(name)));

                                    for (Entry<String, ActionDispatchingConfiguration> eventConfigurationEntry : actionConfiguration
                                            .entrySet()) {
                                        String event = eventConfigurationEntry.getKey();
                                        ActionDispatchingConfiguration eventDispatchingConfiguration = eventConfigurationEntry
                                                .getValue();

                                        if (EventDefinition.ALL.equals(event)) {
                                            Expr annotationLiteralArrayExpr = nameMatchesBlock.newArray(Annotation.class,
                                                    List.of(actionAnnotationLiteralVar));

                                            fireEvent(nameMatchesBlock, gitHubEventHandlerClassName,
                                                    thisExpr, gitHubEventExpr, annotationLiteralArrayExpr);

                                            continue;
                                        }

                                        nameMatchesBlock.if_(nameMatchesBlock.invokeVirtual(OBJECT_EQUALS,
                                                Const.of(event),
                                                dispatchedEventVar),
                                                eventMatchesBlock -> {

                                                    for (Entry<String, EventAnnotation> eventAnnotationEntry : eventDispatchingConfiguration
                                                            .getEventAnnotations()
                                                            .entrySet()) {
                                                        String action = eventAnnotationEntry.getKey();
                                                        EventAnnotation eventAnnotation = eventAnnotationEntry.getValue();

                                                        ClassDesc stringClassDesc = ClassDesc.of(String.class.getName());
                                                        ClassDesc[] literalParameterTypes = Collections
                                                                .nCopies(eventAnnotation.getValues().size(), stringClassDesc)
                                                                .toArray(new ClassDesc[0]);
                                                        List<Expr> literalParameters = new ArrayList<>();
                                                        for (AnnotationValue eventAnnotationValue : eventAnnotation
                                                                .getValues()) {
                                                            literalParameters
                                                                    .add(Const.of(eventAnnotationValue.asString()));
                                                        }

                                                        Expr eventAnnotationLiteralExpr = eventMatchesBlock.new_(
                                                                ConstructorDesc.of(
                                                                        ClassDesc.of(getLiteralClassName(
                                                                                eventAnnotation.getName())),
                                                                        literalParameterTypes),
                                                                literalParameters.toArray(new Expr[0]));

                                                        List<Expr> arrayElements = new ArrayList<>();
                                                        arrayElements.add(actionAnnotationLiteralVar);
                                                        arrayElements.add(eventAnnotationLiteralExpr);
                                                        LocalVar annotationLiteralArrayVar = eventMatchesBlock
                                                                .localVar("annotationLiteralArray", Annotation[].class,
                                                                        eventMatchesBlock.newArray(Annotation.class,
                                                                                arrayElements));

                                                        if (Actions.ALL.equals(action)) {
                                                            fireEvent(eventMatchesBlock,
                                                                    gitHubEventHandlerClassName,
                                                                    thisExpr, gitHubEventExpr,
                                                                    annotationLiteralArrayVar);
                                                        } else {
                                                            eventMatchesBlock.if_(
                                                                    eventMatchesBlock.invokeVirtual(OBJECT_EQUALS,
                                                                            Const.of(action), dispatchedActionVar),
                                                                    actionMatchesBlock -> {
                                                                        fireEvent(actionMatchesBlock,
                                                                                gitHubEventHandlerClassName,
                                                                                thisExpr, gitHubEventExpr,
                                                                                annotationLiteralArrayVar);
                                                                    });
                                                        }
                                                    }
                                                });
                                    }
                                });
                    }

                    b0.return_();
                });
            });
        });
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
            IndexView index,
            DispatchingConfiguration dispatchingConfiguration,
            BuildProducer<ReflectiveClassBuildItem> reflectiveClasses) {
        for (Entry<DotName, TreeSet<ActionDispatchingMethod>> actionDispatchingMethodsEntry : dispatchingConfiguration
                .getMethods().entrySet()) {
            DotName declaringClassName = actionDispatchingMethodsEntry.getKey();
            TreeSet<ActionDispatchingMethod> actionDispatchingMethods = actionDispatchingMethodsEntry.getValue();
            ClassInfo declaringClass = actionDispatchingMethods.iterator().next().getMethod().declaringClass();

            reflectiveClasses.produce(ReflectiveClassBuildItem.builder(declaringClassName.toString())
                    .constructors().methods().fields().build());

            String multiplexerClassName = declaringClassName + "_Multiplexer";
            reflectiveClasses.produce(ReflectiveClassBuildItem.builder(multiplexerClassName)
                    .constructors().methods().fields().build());

            Gizmo gizmo = Gizmo.create(beanClassOutput).withDebugInfo(false).withParameters(false);

            gizmo.class_(multiplexerClassName, cc -> {
                cc.extends_(classDescOf(declaringClassName));
                cc.addAnnotation(Multiplexer.class);

                if (!BuiltinScope.isDeclaredOn(declaringClass)) {
                    cc.addAnnotation(Singleton.class);
                }

                for (AnnotationInstance classAnnotation : declaringClass.declaredAnnotations()) {
                    Jandex2Gizmo.addAnnotation(cc, classAnnotation, index);
                }

                // Copy the constructors
                for (MethodInfo originalConstructor : declaringClass.constructors()) {
                    List<AnnotationInstance> originalMethodAnnotations = originalConstructor.annotations().stream()
                            .filter(ai -> ai.target().kind() == Kind.METHOD).toList();
                    Map<Short, List<AnnotationInstance>> originalConstructorParameterAnnotationMapping = originalConstructor
                            .annotations().stream()
                            .filter(ai -> ai.target().kind() == Kind.METHOD_PARAMETER)
                            .collect(Collectors.groupingBy(ai -> ai.target().asMethodParameter().position()));

                    cc.constructor(constr -> {
                        // Add method-level annotations
                        for (AnnotationInstance originalMethodAnnotation : originalMethodAnnotations) {
                            Jandex2Gizmo.addAnnotation(constr, originalMethodAnnotation, index);
                        }

                        // Create parameters with annotations
                        List<ParamVar> constructorParamsVar = new ArrayList<>();
                        for (short i = 0; i < originalConstructor.parameterTypes().size(); i++) {
                            final short paramIndex = i;
                            List<AnnotationInstance> originalConstructorParameterAnnotations = originalConstructorParameterAnnotationMapping
                                    .getOrDefault(paramIndex, Collections.emptyList());

                            ParamVar param = constr.parameter("param" + i, pc -> {
                                pc.setType(classDescOf(originalConstructor.parameterTypes().get(paramIndex).name()));

                                for (AnnotationInstance originalConstructorParameterAnnotation : originalConstructorParameterAnnotations) {
                                    Jandex2Gizmo.addAnnotation(pc, originalConstructorParameterAnnotation, index);
                                }
                            });
                            constructorParamsVar.add(param);
                        }

                        constr.body(bc -> {
                            bc.invokeSpecial(
                                    ConstructorDesc.of(classDescOf(originalConstructor.declaringClass().name()),
                                            originalConstructor.parameterTypes().stream()
                                                    .map(t -> classDescOf(t.name()))
                                                    .toArray(ClassDesc[]::new)),
                                    cc.this_(),
                                    constructorParamsVar.toArray(new Expr[0]));
                            bc.return_();
                        });
                    });
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
                    if (originalMethod.hasAnnotation(DotNames.OBSERVES)
                            || originalMethod.hasAnnotation(DotNames.OBSERVES_ASYNC)) {
                        LOG.warn(
                                "Methods listening to GitHub actions may not be annotated with @Observes or @ObservesAsync. Offending method: "
                                        + originalMethod.declaringClass().name() + "#" + originalMethod);
                    }

                    List<ClassDesc> parameterTypes = new ArrayList<>();
                    List<Type> originalMethodParameterTypes = originalMethod.parameterTypes();

                    // detect the parameter that is a payload
                    short payloadParameterPosition = -1;
                    if (eventSubscriberInstance != null) {
                        for (short i = 0; i < originalMethodParameterTypes.size(); i++) {
                            List<AnnotationInstance> parameterAnnotations = originalMethodParameterAnnotationMapping
                                    .getOrDefault(i,
                                            Collections.emptyList());
                            if (parameterAnnotations.stream()
                                    .anyMatch(ai -> ai.name().equals(eventSubscriberInstance.name()))) {
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

                        parameterTypes.add(classDescOf(originalMethodParameterTypes.get(i).name()));
                        parameterMapping.put(i, j);
                        j++;
                    }
                    int configFileReaderParameterPosition = -1;
                    if (originalMethod.hasAnnotation(CONFIG_FILE)) {
                        parameterTypes.add(ClassDesc.of(ConfigFileReader.class.getName()));
                        configFileReaderParameterPosition = j;
                        j++;
                    }
                    parameterTypes.add(ClassDesc.of(GitHubEvent.class.getName()));
                    int gitHubEventParameterPosition = j;

                    // Build reverse mapping (generated index -> original index) for annotation copying
                    Map<Short, Short> reverseParameterMapping = new HashMap<>();
                    for (Map.Entry<Short, Short> entry : parameterMapping.entrySet()) {
                        reverseParameterMapping.put(entry.getValue(), entry.getKey());
                    }

                    final short finalPayloadParameterPosition = payloadParameterPosition;
                    final int finalConfigFileReaderParameterPosition = configFileReaderParameterPosition;
                    final int finalGitHubEventParameterPosition = gitHubEventParameterPosition;

                    cc.method(originalMethod.name() + "_"
                            + HashUtil.sha1(originalMethod + "_" + (eventSubscriberInstance != null
                                    ? eventSubscriberInstance.toString()
                                    : EventDefinition.ALL)),
                            mc -> {
                                mc.returning(classDescOf(originalMethod.returnType().name()));

                                for (Type exceptionType : originalMethod.exceptions()) {
                                    mc.throws_(classDescOf(exceptionType.name()));
                                }

                                // Create parameters with annotations
                                List<ParamVar> methodParams = new ArrayList<>();
                                for (int paramIdx = 0; paramIdx < parameterTypes.size(); paramIdx++) {
                                    final int idx = paramIdx;

                                    if (idx == finalGitHubEventParameterPosition) {
                                        // GitHubEvent parameter with @Observes, @Action, and event subscriber annotations
                                        ParamVar param = mc.parameter("gitHubEvent", pc -> {
                                            pc.setType(GitHubEvent.class);
                                            pc.addAnnotation(ClassDesc.of(DotNames.OBSERVES.toString()),
                                                    RetentionPolicy.RUNTIME, ab -> {
                                                    });
                                            pc.addAnnotation(Action.class, ab -> {
                                                ab.add("value", name);
                                            });
                                            if (eventSubscriberInstance != null) {
                                                Jandex2Gizmo.addAnnotation(pc, eventSubscriberInstance, index);
                                            }
                                        });
                                        methodParams.add(param);
                                    } else if (idx == finalConfigFileReaderParameterPosition) {
                                        // ConfigFileReader parameter (no special annotations)
                                        methodParams.add(mc.parameter("configFileReader", ConfigFileReader.class));
                                    } else {
                                        // Mapped parameter from original method - copy annotations
                                        Short origIdx = reverseParameterMapping.get((short) idx);
                                        ParamVar param = mc.parameter("param" + idx, pc -> {
                                            pc.setType(parameterTypes.get(idx));
                                            if (origIdx != null) {
                                                List<AnnotationInstance> paramAnnotations = originalMethodParameterAnnotationMapping
                                                        .getOrDefault(origIdx, Collections.emptyList());
                                                for (AnnotationInstance annotationInstance : paramAnnotations) {
                                                    Jandex2Gizmo.addAnnotation(pc, annotationInstance, index);
                                                }
                                            }
                                        });
                                        methodParams.add(param);
                                    }
                                }

                                mc.body(b0 -> {
                                    Expr thisExpr = mc.this_();
                                    Expr gitHubEventExpr = methodParams.get(finalGitHubEventParameterPosition);

                                    // generate the code of the method
                                    Expr[] parameterValues = new Expr[originalMethod.parameterTypes().size()];
                                    for (short originalMethodParameterIndex = 0; originalMethodParameterIndex < originalMethodParameterTypes
                                            .size(); originalMethodParameterIndex++) {
                                        List<AnnotationInstance> parameterAnnotations = originalMethodParameterAnnotationMapping
                                                .getOrDefault(
                                                        originalMethodParameterIndex,
                                                        Collections.emptyList());
                                        Short multiplexerMethodParameterIndex = parameterMapping
                                                .get(originalMethodParameterIndex);
                                        if (originalMethodParameterIndex == finalPayloadParameterPosition) {
                                            parameterValues[originalMethodParameterIndex] = b0.cast(
                                                    b0.invokeVirtual(
                                                            MethodDesc.of(GitHubEvent.class, "getPayload",
                                                                    GHEventPayload.class),
                                                            gitHubEventExpr),
                                                    classDescOf(originalMethodParameterTypes
                                                            .get(originalMethodParameterIndex).name()));
                                        } else if (INJECTABLE_TYPES
                                                .contains(originalMethodParameterTypes.get(originalMethodParameterIndex)
                                                        .name())) {
                                            DotName injectableType = originalMethodParameterTypes
                                                    .get(originalMethodParameterIndex).name();
                                            parameterValues[originalMethodParameterIndex] = b0.invokeVirtual(
                                                    MethodDesc.of(GitHubEvent.class,
                                                            "get" + injectableType.withoutPackagePrefix(),
                                                            java.lang.constant.MethodTypeDesc.of(
                                                                    classDescOf(injectableType))),
                                                    gitHubEventExpr);
                                        } else if (parameterAnnotations.stream()
                                                .anyMatch(ai -> ai.name().equals(CONFIG_FILE))) {
                                            AnnotationInstance configFileAnnotationInstance = parameterAnnotations.stream()
                                                    .filter(ai -> ai.name().equals(CONFIG_FILE))
                                                    .findFirst()
                                                    .orElseThrow(
                                                            () -> new AssertionError("ConfigFile annotation not present"));
                                            String configObjectType = originalMethodParameterTypes
                                                    .get(originalMethodParameterIndex).name()
                                                    .toString();

                                            boolean isOptional = false;
                                            if (Optional.class.getName().equals(configObjectType)) {
                                                if (originalMethodParameterTypes.get(originalMethodParameterIndex)
                                                        .kind() != Type.Kind.PARAMETERIZED_TYPE) {
                                                    throw new IllegalStateException(
                                                            "Optional is used but not parameterized for method " +
                                                                    originalMethod.declaringClass().name() + "#"
                                                                    + originalMethod);
                                                }
                                                isOptional = true;
                                                configObjectType = originalMethodParameterTypes
                                                        .get(originalMethodParameterIndex)
                                                        .asParameterizedType().arguments().get(0)
                                                        .name().toString();
                                            }

                                            Expr gitHubExpr = b0.invokeVirtual(MethodDesc
                                                    .of(GitHubEvent.class, "getGitHub", GitHub.class),
                                                    gitHubEventExpr);
                                            Expr contextExpr = b0.invokeVirtual(MethodDesc
                                                    .of(GitHubEvent.class, "getContext", Context.class),
                                                    gitHubEventExpr);
                                            Expr repositoryExpr = b0.invokeInterface(MethodDesc
                                                    .of(Context.class, "getGitHubRepository", String.class),
                                                    contextExpr);

                                            Expr configFileReaderExpr = methodParams
                                                    .get(finalConfigFileReaderParameterPosition);
                                            Expr configObjectExpr = b0.invokeVirtual(
                                                    MethodDesc.of(ConfigFileReader.class, "getConfigObject",
                                                            Object.class,
                                                            GitHub.class, String.class, String.class, Class.class),
                                                    configFileReaderExpr,
                                                    gitHubExpr,
                                                    repositoryExpr,
                                                    Const.of(configFileAnnotationInstance.value().asString()),
                                                    Const.of(ClassDesc.of(configObjectType)));
                                            configObjectExpr = b0.cast(configObjectExpr,
                                                    ClassDesc.of(configObjectType));

                                            if (isOptional) {
                                                configObjectExpr = b0.invokeStatic(
                                                        MethodDesc.of(Optional.class, "ofNullable", Optional.class,
                                                                Object.class),
                                                        configObjectExpr);
                                            }

                                            parameterValues[originalMethodParameterIndex] = configObjectExpr;
                                        } else {
                                            parameterValues[originalMethodParameterIndex] = methodParams
                                                    .get(multiplexerMethodParameterIndex);
                                        }
                                    }

                                    // Invoke the original method
                                    ClassDesc originalMethodOwner = classDescOf(originalMethod.declaringClass().name());
                                    ClassDesc originalMethodReturnType = classDescOf(originalMethod.returnType().name());
                                    ClassDesc[] originalMethodParamTypes = originalMethod.parameterTypes().stream()
                                            .map(t -> classDescOf(t.name()))
                                            .toArray(ClassDesc[]::new);
                                    java.lang.constant.MethodTypeDesc originalMethodTypeDesc = java.lang.constant.MethodTypeDesc
                                            .of(originalMethodReturnType, originalMethodParamTypes);
                                    MethodDesc originalMethodDesc = io.quarkus.gizmo2.desc.ClassMethodDesc.of(
                                            originalMethodOwner,
                                            originalMethod.name(),
                                            originalMethodTypeDesc);
                                    Expr returnValue = b0.invokeVirtual(originalMethodDesc, thisExpr, parameterValues);

                                    if (originalMethod.returnType().kind() == Type.Kind.VOID) {
                                        b0.return_();
                                    } else {
                                        b0.return_(returnValue);
                                    }
                                });
                            });
                }
            });
        }
    }

    private static void fireEvent(BlockCreator bc, String className,
            Expr thisExpr, Expr gitHubEventExpr, Expr annotationLiteralArrayExpr) {
        Expr cdiEventExpr = bc.invokeInterface(EVENT_SELECT,
                thisExpr.field(FieldDesc.of(ClassDesc.of(className), EVENT_EMITTER_FIELD, Event.class)),
                annotationLiteralArrayExpr);

        bc.invokeInterface(EVENT_FIRE, cdiEventExpr, gitHubEventExpr);
    }

    private static String getLiteralClassName(DotName annotationName) {
        return annotationName + "_AnnotationLiteral";
    }

    private static class RemoveBridgeMethodsClassVisitor extends ClassVisitor {

        private static final Logger LOG = Logger.getLogger(RemoveBridgeMethodsClassVisitor.class);

        private final String className;
        private final Set<String> methodsWithBridges;

        public RemoveBridgeMethodsClassVisitor(ClassVisitor visitor, String className, Set<String> methodsWithBridges) {
            super(Opcodes.ASM9, visitor);

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
