package io.routex.action;

import org.springframework.stereotype.Component;
import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface ActionHandler {
    String value() default ""; // optional explicit bean alias
}
