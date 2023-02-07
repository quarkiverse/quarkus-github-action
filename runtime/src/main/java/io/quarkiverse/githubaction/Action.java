package io.quarkiverse.githubaction;

import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Documented;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import jakarta.enterprise.util.AnnotationLiteral;
import jakarta.inject.Qualifier;

/**
 * Qualifier used to specify which action will be run when using a named action.
 */
@Target({ METHOD, PARAMETER })
@Retention(RUNTIME)
@Documented
@Qualifier
public @interface Action {

    static final String UNNAMED = "<<UNNAMED>>";

    String value() default UNNAMED;

    public class ActionLiteral extends AnnotationLiteral<Action> implements Action {

        private String value;

        public ActionLiteral() {
            this(UNNAMED);
        }

        public ActionLiteral(String value) {
            this.value = value;
        }

        @Override
        public String value() {
            return value;
        }
    }
}
