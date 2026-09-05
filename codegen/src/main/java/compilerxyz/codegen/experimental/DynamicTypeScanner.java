package compilerxyz.codegen.experimental;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.CodeBlock;
import compilerxyz.prelude.util.TMap;

public final class DynamicTypeScanner {
    private DynamicTypeScanner() {}

    static {
        StaticJavaParser.getParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21);
    }

    public static void walkNested(
            String pkg, String enclosingSimpleChain, TypeDeclaration<?> t, TMap<ClassName, CodeBlock> out) {
        var simple = t.getNameAsString();
        var cn = ClassName.get(pkg, enclosingSimpleChain, simple);
        putDescriptor(out, cn, "Gen" + enclosingSimpleChain.replace(".", "") + simple, t);
        for (var member : t.getMembers()) {
            if (member instanceof TypeDeclaration<?> nested) {
                walkNested(pkg, enclosingSimpleChain + "." + simple, nested, out);
            }
        }
    }

    public static void putDescriptor(
            TMap<ClassName, CodeBlock> out, ClassName cn, String companionSimple, TypeDeclaration<?> t) {
        boolean hasBuiltinFullAnnotation;
        _hasBuiltinFullAnnotation:
        {
            for (var a : t.getAnnotations()) {
                var n = a.getNameAsString();
                if (n.equals("BuiltinFull") || n.equals("compilerxyz.prelude.annotations.BuiltinFull")) {
                    {
                        hasBuiltinFullAnnotation = true;
                        break _hasBuiltinFullAnnotation;
                    }
                }
            }
            {
                hasBuiltinFullAnnotation = false;
                break _hasBuiltinFullAnnotation;
            }
        }
        boolean full = hasBuiltinFullAnnotation;

        boolean hasNullaryDynamicType;

        _hasNullaryDynamicType:
        {
            if (t instanceof ClassOrInterfaceDeclaration || t instanceof RecordDeclaration || t.isEnumDeclaration()) {
                for (var m : t.getMethods()) {
                    boolean isNullaryStaticDynamicType;
                    _isNullaryStaticDynamicType:
                    {
                        if (!m.getNameAsString().equals("dynamicType")) {
                            isNullaryStaticDynamicType = false;
                            break _isNullaryStaticDynamicType;
                        }
                        if (!m.isStatic()) {
                            isNullaryStaticDynamicType = false;
                            break _isNullaryStaticDynamicType;
                        }
                        if (!m.getParameters().isEmpty()) {
                            isNullaryStaticDynamicType = false;
                            break _isNullaryStaticDynamicType;
                        }
                        {
                            isNullaryStaticDynamicType = true;
                            break _isNullaryStaticDynamicType;
                        }
                    }
                    if (isNullaryStaticDynamicType) {
                        hasNullaryDynamicType = true;
                        break _hasNullaryDynamicType;
                    }
                }
            }
            {
                hasNullaryDynamicType = false;
                break _hasNullaryDynamicType;
            }
        }

        boolean handWritten = hasNullaryDynamicType;

        if (full && handWritten) {
            throw new IllegalStateException("@BuiltinFull type "
                    + cn.canonicalName()
                    + " also hand-declares a nullary dynamicType(); its descriptor can't be both"
                    + " generated and hand-written. Mark it @BuiltinLight (codegen generates no"
                    + " companion) or drop the hand-written dynamicType().");
        }
        if (full) {
            out.put(cn, CodeBlock.of("$T.dynamicType()", ClassName.get("compilerxyz.main.gen", companionSimple)));
        } else if (handWritten) {
            out.put(cn, CodeBlock.of("$T.dynamicType()", cn));
        }
    }
}
