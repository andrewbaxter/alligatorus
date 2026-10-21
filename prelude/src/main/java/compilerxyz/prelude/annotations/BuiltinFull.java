package compilerxyz.prelude.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface BuiltinFull {
    long uniqueId();

    int scopeId() default 0;

    long identityId() default 0;

    boolean lift() default true;
}
