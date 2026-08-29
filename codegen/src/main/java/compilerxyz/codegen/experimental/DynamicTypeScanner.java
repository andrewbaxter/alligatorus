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
        boolean _rHasBuiltinFullAnnotation;
        _hasBuiltinFullAnnotation:
        {
            for (var a : t.getAnnotations()) {
                var n = a.getNameAsString();
                if (n.equals("BuiltinFull") || n.equals("compilerxyz.prelude.annotations.BuiltinFull")) {
                    {
                        _rHasBuiltinFullAnnotation = true;
                        break _hasBuiltinFullAnnotation;
                    }
                }
            }
            {
                _rHasBuiltinFullAnnotation = false;
                break _hasBuiltinFullAnnotation;
            }
        }
        boolean full = _rHasBuiltinFullAnnotation;

        boolean _rHasNullaryDynamicType;

        _hasNullaryDynamicType:
        {
            if (t instanceof ClassOrInterfaceDeclaration || t instanceof RecordDeclaration || t.isEnumDeclaration()) {
                for (var m : t.getMethods()) {
                    boolean _rIsNullaryStaticDynamicType;
                    _isNullaryStaticDynamicType:
                    {
                        if (!m.getNameAsString().equals("dynamicType")) {
                            _rIsNullaryStaticDynamicType = false;
                            break _isNullaryStaticDynamicType;
                        }
                        if (!m.isStatic()) {
                            _rIsNullaryStaticDynamicType = false;
                            break _isNullaryStaticDynamicType;
                        }
                        if (!m.getParameters().isEmpty()) {
                            _rIsNullaryStaticDynamicType = false;
                            break _isNullaryStaticDynamicType;
                        }
                        {
                            _rIsNullaryStaticDynamicType = true;
                            break _isNullaryStaticDynamicType;
                        }
                    }
                    if (_rIsNullaryStaticDynamicType) {
                        _rHasNullaryDynamicType = true;
                        break _hasNullaryDynamicType;
                    }
                }
            }
            {
                _rHasNullaryDynamicType = false;
                break _hasNullaryDynamicType;
            }
        }

        boolean handWritten = _rHasNullaryDynamicType;

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
