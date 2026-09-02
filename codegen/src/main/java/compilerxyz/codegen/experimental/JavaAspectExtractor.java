package compilerxyz.codegen.experimental;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.VoidType;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.ParameterizedTypeName;
import com.squareup.javapoet.TypeName;
import compilerxyz.codegen.BuiltinInterfacesGen;
import compilerxyz.codegen.util.BuiltinMethodEmit.Method;
import compilerxyz.codegen.util.BuiltinMethodEmit.Param;
import compilerxyz.codegen.util.NameDerivation;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TMap;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public final class JavaAspectExtractor {
    private JavaAspectExtractor() {}

    static {
        StaticJavaParser.getParserConfiguration().setLanguageLevel(ParserConfiguration.LanguageLevel.JAVA_21);
    }

    public record ScanResult(TList<BuiltinInterfacesGen.BuiltinSpec> builtins) {}

    public static void collectNestedBuiltinRecords(
            Path sourceFile,
            CompilationUnit cu,
            TypeDeclaration<?> outer,
            TList<BuiltinInterfacesGen.BuiltinSpec> builtins) {
        var outerSimple = outer.getNameAsString();
        for (var member : outer.getMembers()) {
            if (member instanceof RecordDeclaration rec) {
                var hasBuiltin = rec.getAnnotationByName("BuiltinFull").isPresent()
                        || rec.getAnnotationByName("compilerxyz.prelude.annotations.BuiltinFull")
                                .isPresent();
                if (hasBuiltin) {
                    var spec = extractBuiltinSpecRecord(cu, rec, Opt.some(outerSimple));
                    if (spec.optIsSome()) builtins.add(spec.optUnwrap());
                }
            }
            if (member instanceof TypeDeclaration<?> nested) {
                collectNestedBuiltinRecords(sourceFile, cu, nested, builtins);
            }
        }
    }

    public static Opt<BuiltinInterfacesGen.BuiltinSpec> extractBuiltinSpecRecord(
            CompilationUnit cu, RecordDeclaration recDecl, Opt<String> outerSimpleName) {
        var nestedName = recDecl.getNameAsString();
        var simpleName = outerSimpleName.optIsNone() ? nestedName : outerSimpleName.optUnwrap() + nestedName;
        var pkg = cu.getPackageDeclaration().orElseThrow().getNameAsString();
        var builtinAnn = getBuiltinAnnotation(recDecl);
        var attrs = readAttrs(builtinAnn);
        var uniqueIdStr = attrs.get("uniqueId");
        if (uniqueIdStr == null) {
            throw new IllegalStateException("@Builtin on " + simpleName + " missing required uniqueId");
        }
        var uniqueId = Long.parseLong(uniqueIdStr);
        var importMap = buildImportMap(cu);
        var dataFields = new TList<BuiltinInterfacesGen.DataField>();
        var methods = new TList<Method>();
        var recordComponentNames = new HashSet<String>();
        for (var comp : recDecl.getParameters()) {
            var compName = comp.getNameAsString();
            recordComponentNames.add(compName);
            dataFields.add(new BuiltinInterfacesGen.DataField(
                    NameDerivation.snakeCase(compName), compName, typeNameOf(comp.getType(), importMap, pkg)));
        }
        for (var m : recDecl.getMethods()) {
            if (m.isStatic()) continue;
            if (!m.getParameters().isEmpty()) continue;
            if (recordComponentNames.contains(m.getNameAsString())) continue;
            var sourceName = NameDerivation.snakeCase(m.getNameAsString());
            methods.add(new Method(
                    sourceName,
                    m.getNameAsString(),
                    new TList<Param>(),
                    typeNameOf(m.getType(), importMap, pkg),
                    true,
                    false));
        }
        return Opt.some(new BuiltinInterfacesGen.BuiltinSpec(
                simpleName,
                pkg,
                uniqueId,
                0L,
                false,
                true,
                false,
                methods,
                dataFields,
                outerSimpleName,
                outerSimpleName.optIsNone() ? Opt.none() : Opt.some(nestedName)));
    }

    public static AnnotationExpr getBuiltinAnnotation(TypeDeclaration<?> decl) {
        return decl.getAnnotationByName("BuiltinFull")
                .or(() -> decl.getAnnotationByName("compilerxyz.prelude.annotations.BuiltinFull"))
                .orElseThrow(() -> new IllegalStateException("@BuiltinFull missing on " + decl.getNameAsString()));
    }

    public static Map<String, String> readAttrs(AnnotationExpr annot) {
        var out = new HashMap<String, String>();
        if (annot instanceof NormalAnnotationExpr n) {
            for (var p : n.getPairs()) {
                String _rLiteralValueOf;
                _literalValueOf:
                {
                    var v = p.getValue();
                    if (v instanceof StringLiteralExpr s) {
                        _rLiteralValueOf = s.asString();
                        break _literalValueOf;
                    }
                    var raw = v.toString();
                    if (raw.endsWith("L") || raw.endsWith("l")) raw = raw.substring(0, raw.length() - 1);
                    {
                        _rLiteralValueOf = raw;
                        break _literalValueOf;
                    }
                }
                out.put(p.getNameAsString(), _rLiteralValueOf);
            }
        }
        return out;
    }

    public static TMap<String, String> buildImportMap(CompilationUnit cu) {
        var m = new TMap<String, String>();
        for (var imp : cu.getImports()) {
            var full = imp.getNameAsString();
            var simple = full.substring(full.lastIndexOf('.') + 1);
            m.put(simple, full);
        }
        return m;
    }

    public static TypeName typeNameOf(Type t, TMap<String, String> imports, String sourcePkg) {
        if (t instanceof VoidType) return TypeName.VOID;
        if (t instanceof PrimitiveType p) {
            return switch (p.getType()) {
                case BOOLEAN -> TypeName.BOOLEAN;
                case DOUBLE -> TypeName.DOUBLE;
                case INT -> TypeName.INT;
                case LONG -> TypeName.LONG;
                case FLOAT -> TypeName.FLOAT;
                case CHAR -> TypeName.CHAR;
                case BYTE -> TypeName.BYTE;
                case SHORT -> TypeName.SHORT;
            };
        }
        if (t instanceof ClassOrInterfaceType c) {
            String simple = c.getNameAsString();
            ClassName _rClassNameOf;
            _classNameOf:
            {
                var fqn = imports.getOrNull(simple);
                if (fqn == null) {
                    try {
                        fqn = Class.forName("java.lang." + simple).getName();
                    } catch (ClassNotFoundException e) {
                        fqn = sourcePkg + "." + simple;
                    }
                }
                var idx = fqn.lastIndexOf('.');
                {
                    _rClassNameOf = ClassName.get(fqn.substring(0, idx), fqn.substring(idx + 1));
                    break _classNameOf;
                }
            }
            ClassName raw = _rClassNameOf;

            if (c.getTypeArguments().isPresent() && !c.getTypeArguments().get().isEmpty()) {
                var argsList = c.getTypeArguments().get();
                var argNames = new TypeName[argsList.size()];
                for (int i = 0; i < argsList.size(); i++) {
                    argNames[i] = typeNameOf(argsList.get(i), imports, sourcePkg);
                }
                return ParameterizedTypeName.get(raw, argNames);
            }
            return raw;
        }
        throw new IllegalStateException("Unhandled type: " + t + " (" + t.getClass() + ")");
    }
}
