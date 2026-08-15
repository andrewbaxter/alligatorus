package compilerxyz.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a handwritten source-language "push struct" class — a value-object wrapper (e.g.
 * SourceUnionBuilder) that source code invokes via its instance methods. Codegen scans for
 * these and emits the corresponding GenObjectTypes registry entry from the class's declared
 * shape: constructor parameters become data fields, public final fields become PushFields,
 * and methods annotated with {@link SourceExposed} become PushMethods.
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.SOURCE)
public @interface BuiltinPushStruct {}
