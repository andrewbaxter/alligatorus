package compilerxyz.codegen.experimental;

import com.github.javaparser.ParserConfiguration;
import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.Parameter;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.expr.AnnotationExpr;
import com.github.javaparser.ast.expr.NormalAnnotationExpr;
import com.github.javaparser.ast.expr.StringLiteralExpr;
import com.github.javaparser.ast.nodeTypes.NodeWithAnnotations;
import com.github.javaparser.ast.nodeTypes.NodeWithTypeParameters;
import com.github.javaparser.ast.type.ArrayType;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.PrimitiveType;
import com.github.javaparser.ast.type.Type;
import com.github.javaparser.ast.type.VoidType;
import com.squareup.javapoet.ArrayTypeName;
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
import java.util.List;
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
            if (member instanceof ClassOrInterfaceDeclaration nestedCls && !nestedCls.isInterface()) {
                var hasBuiltin = nestedCls.getAnnotationByName("BuiltinFull").isPresent()
                        || nestedCls
                                .getAnnotationByName("compilerxyz.prelude.annotations.BuiltinFull")
                                .isPresent();
                if (hasBuiltin) {
                    var spec = extractBuiltinSpecClass(cu, nestedCls, Opt.some(outerSimple));
                    if (spec.optIsSome()) builtins.add(spec.optUnwrap());
                }
            }
            if (member instanceof EnumDeclaration nestedEnum) {
                var hasBuiltin = nestedEnum.getAnnotationByName("BuiltinFull").isPresent()
                        || nestedEnum
                                .getAnnotationByName("compilerxyz.prelude.annotations.BuiltinFull")
                                .isPresent();
                if (hasBuiltin) {
                    var spec = extractBuiltinSpecClass(cu, nestedEnum, Opt.some(outerSimple));
                    if (spec.optIsSome()) builtins.add(spec.optUnwrap());
                }
            }
            if (member instanceof TypeDeclaration<?> nested) {
                collectNestedBuiltinRecords(sourceFile, cu, nested, builtins);
            }
        }
    }

    public static Opt<BuiltinInterfacesGen.BuiltinSpec> extractBuiltinSpecClass(
            CompilationUnit cu, TypeDeclaration<?> clsDecl, Opt<String> outerSimpleName) {
        var nestedName = clsDecl.getNameAsString();
        var simpleName = outerSimpleName.optIsNone() ? nestedName : outerSimpleName.optUnwrap() + nestedName;
        var pkg = cu.getPackageDeclaration().orElseThrow().getNameAsString();
        var builtinAnn = getBuiltinAnnotation(clsDecl);
        var attrs = readAttrs(builtinAnn);
        var uniqueIdStr = attrs.get("uniqueId");
        if (uniqueIdStr == null) {
            throw new IllegalStateException("@Builtin on " + simpleName + " missing required uniqueId");
        }
        var uniqueId = Long.parseLong(uniqueIdStr);
        var lift = !"false".equals(attrs.get("lift"));
        var classImports = buildImportMap(cu);
        var classFields = new TList<BuiltinInterfacesGen.DataField>();
        for (var f : clsDecl.getFields()) {
            if (f.isStatic() || !f.isPublic() || !f.isFinal()) continue;
            for (var v : f.getVariables()) {
                classFields.add(new BuiltinInterfacesGen.DataField(
                        NameDerivation.snakeCase(v.getNameAsString()),
                        v.getNameAsString(),
                        typeNameOf(f.getElementType(), classImports, pkg)));
            }
        }
        var classMethods = new TList<Method>();
        for (var m : clsDecl.getMethods()) {
            if (m.isStatic() || !m.isPublic()) continue;
            var params = m.getParameters();
            var needsEvalContext = injectsEvalContext(m, params, simpleName + "." + m.getNameAsString());
            var methodParams = new TList<Param>();
            for (int i = needsEvalContext ? 1 : 0; i < params.size(); i++) {
                var p = params.get(i);
                methodParams.add(new Param(
                        NameDerivation.snakeCase(p.getNameAsString()), typeNameOf(p.getType(), classImports, pkg)));
            }
            classMethods.add(new Method(
                    NameDerivation.snakeCase(m.getNameAsString()),
                    m.getNameAsString(),
                    methodParams,
                    typeNameOf(m.getType(), classImports, pkg),
                    true,
                    needsEvalContext));
        }
        {
            var identityToken = clsDecl.getAnnotationByName("IdentityToken").isPresent()
                    || clsDecl.getAnnotationByName("compilerxyz.prelude.annotations.IdentityToken")
                            .isPresent();
            return Opt.some(new BuiltinInterfacesGen.BuiltinSpec(
                    simpleName,
                    pkg,
                    uniqueId,
                    0L,
                    false,
                    false,
                    false,
                    classMethods,
                    classFields,
                    outerSimpleName,
                    outerSimpleName.optIsNone() ? Opt.none() : Opt.some(nestedName),
                    identityToken,
                    lift));
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
        var lift = !"false".equals(attrs.get("lift"));
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
            if (m.isStatic() || !m.isPublic()) continue;
            if (recordComponentNames.contains(m.getNameAsString())) continue;
            var params = m.getParameters();
            var needsEvalContext = injectsEvalContext(m, params, simpleName + "." + m.getNameAsString());
            var methodParams = new TList<Param>();
            for (int i = needsEvalContext ? 1 : 0; i < params.size(); i++) {
                var p = params.get(i);
                methodParams.add(new Param(
                        NameDerivation.snakeCase(p.getNameAsString()), typeNameOf(p.getType(), importMap, pkg)));
            }
            methods.add(new Method(
                    NameDerivation.snakeCase(m.getNameAsString()),
                    m.getNameAsString(),
                    methodParams,
                    typeNameOf(m.getType(), importMap, pkg),
                    true,
                    needsEvalContext));
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
                outerSimpleName.optIsNone() ? Opt.none() : Opt.some(nestedName),
                false,
                lift));
    }

    public static boolean injectsEvalContext(NodeWithAnnotations<?> m, List<Parameter> params, String where) {
        boolean annotated = m.getAnnotationByName("BuiltinEvalContext").isPresent()
                || m.getAnnotationByName("compilerxyz.prelude.annotations.BuiltinEvalContext")
                        .isPresent();
        if (!annotated) return false;
        if (params.isEmpty() || !params.get(0).getType().asString().endsWith("EvalContext")) {
            throw new IllegalStateException(
                    "@BuiltinEvalContext on " + where + " requires the first parameter to be an EvalContext");
        }
        return true;
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
                String literalValueOf;
                _literalValueOf:
                {
                    var v = p.getValue();
                    if (v instanceof StringLiteralExpr s) {
                        literalValueOf = s.asString();
                        break _literalValueOf;
                    }
                    var raw = v.toString();
                    if (raw.endsWith("L") || raw.endsWith("l")) raw = raw.substring(0, raw.length() - 1);
                    {
                        literalValueOf = raw;
                        break _literalValueOf;
                    }
                }
                out.put(p.getNameAsString(), literalValueOf);
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
        if (t instanceof ArrayType a) {
            return ArrayTypeName.of(typeNameOf(a.getComponentType(), imports, sourcePkg));
        }
        if (t instanceof ClassOrInterfaceType c) {
            String simple = c.getNameAsString();
            {
                var node = c.getParentNode();
                while (node.isPresent()) {
                    var n = node.get();
                    if (n instanceof NodeWithTypeParameters<?> withParams) {
                        for (var tp : withParams.getTypeParameters()) {
                            if (tp.getNameAsString().equals(simple)) {
                                if (tp.getTypeBound().isEmpty()) return ClassName.OBJECT;
                                return typeNameOf(tp.getTypeBound().get(0), imports, sourcePkg);
                            }
                        }
                    }
                    node = n.getParentNode();
                }
            }
            {
                var node = c.getParentNode();
                while (node.isPresent()) {
                    var n = node.get();
                    if (n instanceof TypeDeclaration<?> decl) {
                        for (var member : decl.getMembers()) {
                            if (member instanceof TypeDeclaration<?> nested
                                    && nested.getNameAsString().equals(simple)) {
                                var outers = new TList<String>();
                                var up = decl;
                                while (true) {
                                    outers.add(up.getNameAsString());
                                    var parent = up.getParentNode();
                                    if (parent.isPresent() && parent.get() instanceof TypeDeclaration<?> pd) {
                                        up = pd;
                                    } else {
                                        break;
                                    }
                                }
                                var outermost = outers.size() - 1;
                                var rest = new String[outers.size()];
                                for (int i = 0; i < outermost; i++) {
                                    rest[i] = outers.get(outermost - 1 - i);
                                }
                                rest[outermost] = simple;
                                return ClassName.get(sourcePkg, outers.get(outermost), rest);
                            }
                        }
                    }
                    node = n.getParentNode();
                }
            }
            if (c.getScope().isPresent()) {
                var scopeName = typeNameOf(c.getScope().get(), imports, sourcePkg);
                if (scopeName instanceof ClassName scn) return scn.nestedClass(simple);
                throw new IllegalStateException("Unhandled scoped type: " + t);
            }
            ClassName classNameOf;
            _classNameOf:
            {
                var imported = imports.getOpt(simple);
                String fqn;
                if (imported.optIsSome()) {
                    fqn = imported.optUnwrap();
                } else {
                    try {
                        fqn = Class.forName("java.lang." + simple).getName();
                    } catch (ClassNotFoundException e) {
                        fqn = sourcePkg + "." + simple;
                    }
                }
                var idx = fqn.lastIndexOf('.');
                {
                    classNameOf = ClassName.get(fqn.substring(0, idx), fqn.substring(idx + 1));
                    break _classNameOf;
                }
            }
            ClassName raw = classNameOf;

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
