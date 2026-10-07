package compilerxyz.codegen;

import com.github.javaparser.StaticJavaParser;
import com.github.javaparser.ast.CompilationUnit;
import com.github.javaparser.ast.body.ClassOrInterfaceDeclaration;
import com.github.javaparser.ast.body.EnumDeclaration;
import com.github.javaparser.ast.body.RecordDeclaration;
import com.github.javaparser.ast.body.TypeDeclaration;
import com.github.javaparser.ast.nodeTypes.NodeWithImplements;
import com.github.javaparser.ast.type.ClassOrInterfaceType;
import com.github.javaparser.ast.type.Type;
import compilerxyz.codegen.CodecsGen.Component;
import compilerxyz.codegen.CodecsGen.TypeKind;
import compilerxyz.prelude.util.IList;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TMap;
import compilerxyz.prelude.util.TOrderedMap;
import compilerxyz.prelude.util.TOrderedSet;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.stream.Stream;

public final class CodecsGen {
    private CodecsGen() {}

    static final String GEN_PACKAGE = "compilerxyz.main.gen.codec";

    public static final IList<String> HAND_CODED = IList.of(
            "compilerxyz.main.safejson.JsonValue",
            "compilerxyz.main.artifact.Artifact",
            "compilerxyz.main.artifact.ArtifactBody",
            "compilerxyz.main.artifact.ArtifactBodyExporter",
            "compilerxyz.main.artifact.ArtifactIdSource",
            "compilerxyz.prelude.util.Opt",
            "compilerxyz.prelude.util.Opt.Some",
            "compilerxyz.prelude.util.Opt.None");

    private static final String MARKER = "compilerxyz.main.safejson.SafeJsonSerializable";
    private static final String STREAM_MARKER = "compilerxyz.main.safejson.SafeJsonStreamable";

    public static void walk(String qname, TypeIndex index, TOrderedSet<String> seen) {
        if (!seen.add(qname)) return;
        if (HAND_CODED.contains(qname)) return;
        var tOpt = index.byQname.getOpt(qname);
        if (tOpt.optIsNone()) return;
        var t = tOpt.optUnwrap();
        if (t.kind == TypeKind.SEALED) {
            for (var perm : t.permittedSubclasses) {
                walk(perm, index, seen);
            }
        } else if (t.kind == TypeKind.RECORD) {
            for (var comp : t.components) {
                for (var dep : referencedTypeNames(comp.type, index, qname)) {
                    walk(dep, index, seen);
                }
            }
        }
    }

    static TList<String> referencedTypeNames(ParsedType type, TypeIndex index, String contextQname) {
        var out = new TList<String>();
        if (type.kind == ParsedTypeKind.NAMED) {
            var resolved = index.resolve(type.name, contextQname);
            if (resolved.optIsSome()) out.add(resolved.optUnwrap());
            for (var ta : type.typeArgs) {
                out.addAll(referencedTypeNames(ta, index, contextQname));
            }
        }
        return out;
    }

    enum TypeKind {
        RECORD,
        SEALED,
        ENUM,
        OTHER
    }

    enum ParsedTypeKind {
        NAMED,
        PRIMITIVE,
        BYTE_ARRAY
    }

    static final class ParsedType {
        final ParsedTypeKind kind;
        String name;
        final TList<ParsedType> typeArgs;

        ParsedType(ParsedTypeKind kind, String name, TList<ParsedType> typeArgs) {
            this.kind = kind;
            this.name = name;
            this.typeArgs = typeArgs;
        }
    }

    static final class Component {
        final String name;
        final ParsedType type;

        Component(String name, ParsedType type) {
            this.name = name;
            this.type = type;
        }
    }

    static final class TypeInfo {
        TypeKind kind = TypeKind.OTHER;
        boolean implementsMarker;
        boolean implementsStreamMarker;
        final TList<Component> components = new TList<>();
        final TList<String> permittedSubclasses = new TList<>();
        final TList<String> enumConstants = new TList<>();

        final TList<String> implementedSimpleNames = new TList<>();

        final TMap<String, String> imports = new TMap<>();

        final String pkg = "";
        String packageName = "";

        @Override
        public String toString() {
            return kind + " marker=" + implementsMarker + " comps=" + components.size();
        }
    }

    static final class TypeIndex {

        final TOrderedMap<String, TypeInfo> byQname = new TOrderedMap<>();

        final TMap<String, TList<String>> byPackage = new TMap<>();

        void scan(Path runtimeSourceRoot) throws IOException {
            try (Stream<Path> walk = Files.walk(runtimeSourceRoot)) {
                var files = walk.filter(p -> p.toString().endsWith(".java")).toList();
                for (var p : files) {
                    Path file = p;

                    CompilationUnit cu;
                    try {
                        cu = StaticJavaParser.parse(file);
                    } catch (Exception e) {
                        throw new IOException("JavaParser failed on " + file, e);
                    }
                    var pkgDecl = cu.getPackageDeclaration()
                            .map(pd -> pd.getNameAsString())
                            .orElse("");

                    var importMap = new TMap<String, String>();
                    for (var imp : cu.getImports()) {
                        if (imp.isAsterisk() || imp.isStatic()) continue;
                        var full = imp.getNameAsString();
                        var dot = full.lastIndexOf('.');
                        var simple = dot < 0 ? full : full.substring(dot + 1);
                        importMap.put(simple, full);
                    }
                    var cuRegistered = new TList<String>();
                    for (var td : cu.getTypes()) {
                        registerType(td, pkgDecl, pkgDecl, importMap, cuRegistered);
                    }

                    for (var sealedQname : cuRegistered) {
                        var sealedInfoOpt = byQname.getOpt(sealedQname);
                        if (sealedInfoOpt.optIsNone()) continue;
                        var sealedInfo = sealedInfoOpt.optUnwrap();
                        if (sealedInfo.kind != TypeKind.SEALED) continue;
                        if (!sealedInfo.permittedSubclasses.isEmpty()) continue;
                        var sealedSimple = sealedQname.substring(sealedQname.lastIndexOf('.') + 1);
                        for (var otherQname : cuRegistered) {
                            if (otherQname.equals(sealedQname)) continue;
                            var otherOpt = byQname.getOpt(otherQname);
                            if (otherOpt.optIsNone()) continue;
                            var other = otherOpt.optUnwrap();
                            if (other.implementedSimpleNames.contains(sealedSimple)) {
                                sealedInfo.permittedSubclasses.add(otherQname);
                            }
                        }
                    }
                }
            }
        }

        private void registerType(
                TypeDeclaration<?> td,
                String pkg,
                String enclosingQname,
                TMap<String, String> importMap,
                TList<String> cuRegistered) {
            var simple = td.getNameAsString();
            var qname = enclosingQname.isEmpty() ? simple : enclosingQname + "." + simple;

            var info = new TypeInfo();
            info.packageName = pkg;
            info.imports.putAll(importMap);

            if (td instanceof RecordDeclaration rd) {
                info.kind = TypeKind.RECORD;
                for (var param : rd.getParameters()) {
                    var pt = parseType(param.getType());
                    info.components.add(new Component(param.getNameAsString(), pt));
                }
                for (var iface : rd.getImplementedTypes()) {
                    info.implementedSimpleNames.add(iface.getNameAsString());
                }
                info.implementsMarker =
                        checkMarker(rd, importMap, pkg, MARKER) || checkMarker(rd, importMap, pkg, STREAM_MARKER);
                info.implementsStreamMarker = checkMarker(rd, importMap, pkg, STREAM_MARKER);
            } else if (td instanceof ClassOrInterfaceDeclaration cid) {
                if (cid.isInterface()
                        && cid.getModifiers().stream()
                                .anyMatch(m -> m.getKeyword().asString().equals("sealed"))) {
                    info.kind = TypeKind.SEALED;
                    for (var pt : cid.getPermittedTypes()) {
                        var qn = resolveSimpleName(pt.getNameAsString(), importMap, pkg);
                        info.permittedSubclasses.add(qn);
                    }

                    if (info.permittedSubclasses.isEmpty()) {
                        for (var member : cid.getMembers()) {
                            if (member instanceof TypeDeclaration<?> nested) {
                                String parentSimpleName = cid.getNameAsString();
                                boolean isImplementedBy;
                                _isImplementedBy:
                                {
                                    if (nested instanceof RecordDeclaration rd) {
                                        for (var iface : rd.getImplementedTypes()) {
                                            if (iface.getNameAsString().equals(parentSimpleName)) {
                                                isImplementedBy = true;
                                                break _isImplementedBy;
                                            }
                                        }
                                    } else if (nested instanceof ClassOrInterfaceDeclaration coid) {
                                        for (var iface : coid.getImplementedTypes()) {
                                            if (iface.getNameAsString().equals(parentSimpleName)) {
                                                isImplementedBy = true;
                                                break _isImplementedBy;
                                            }
                                        }
                                        for (var iface : coid.getExtendedTypes()) {
                                            if (iface.getNameAsString().equals(parentSimpleName)) {
                                                isImplementedBy = true;
                                                break _isImplementedBy;
                                            }
                                        }
                                    }
                                    {
                                        isImplementedBy = false;
                                        break _isImplementedBy;
                                    }
                                }
                                boolean implementedBy = isImplementedBy;

                                if (implementedBy) {
                                    info.permittedSubclasses.add(qname + "." + nested.getNameAsString());
                                }
                            }
                        }
                    }
                    info.implementsMarker =
                            checkMarker(cid, importMap, pkg, MARKER) || checkMarker(cid, importMap, pkg, STREAM_MARKER);
                    info.implementsStreamMarker = checkMarker(cid, importMap, pkg, STREAM_MARKER);
                } else {
                    info.kind = TypeKind.OTHER;
                    info.implementsMarker =
                            checkMarker(cid, importMap, pkg, MARKER) || checkMarker(cid, importMap, pkg, STREAM_MARKER);
                    info.implementsStreamMarker = checkMarker(cid, importMap, pkg, STREAM_MARKER);
                }
            } else if (td instanceof EnumDeclaration ed) {
                info.kind = TypeKind.ENUM;
                for (var c : ed.getEntries()) {
                    info.enumConstants.add(c.getNameAsString());
                }
            }

            byQname.put(qname, info);
            byPackage.computeIfAbsentT(pkg, k -> new TList<>()).add(qname);
            cuRegistered.add(qname);

            for (var member : td.getMembers()) {
                if (member instanceof TypeDeclaration<?> nested) {
                    registerType(nested, pkg, qname, importMap, cuRegistered);
                }
            }
        }

        private boolean checkMarker(
                NodeWithImplements<?> nwi, TMap<String, String> importMap, String pkg, String marker) {
            for (var iface : nwi.getImplementedTypes()) {
                var qn = resolveSimpleName(iface.getNameAsString(), importMap, pkg);
                if (qn.equals(marker)) return true;
            }

            if (nwi instanceof ClassOrInterfaceDeclaration cid && cid.isInterface()) {
                for (var iface : cid.getExtendedTypes()) {
                    var qn = resolveSimpleName(iface.getNameAsString(), importMap, pkg);
                    if (qn.equals(marker)) return true;
                }
            }
            return false;
        }

        private String resolveSimpleName(String name, TMap<String, String> importMap, String pkg) {

            var firstDot = name.indexOf('.');
            var head = firstDot < 0 ? name : name.substring(0, firstDot);
            var tail = firstDot < 0 ? "" : name.substring(firstDot);

            var fromImportOpt = importMap.getOpt(head);
            if (fromImportOpt.optIsSome()) {
                var fromImport = fromImportOpt.optUnwrap();
                return fromImport + tail;
            }

            var samePackageOpt = byPackage.getOpt(pkg);
            if (samePackageOpt.optIsSome()) {
                var samePackage = samePackageOpt.optUnwrap();
                for (var qn : samePackage) {
                    var lastDot = qn.lastIndexOf('.');
                    var simple = lastDot < 0 ? qn : qn.substring(lastDot + 1);
                    if (simple.equals(head)) return qn + tail;
                }
            }

            if (head.equals("String")
                    || head.equals("Integer")
                    || head.equals("Long")
                    || head.equals("Double")
                    || head.equals("Float")
                    || head.equals("Boolean")
                    || head.equals("Short")
                    || head.equals("Byte")
                    || head.equals("Character")
                    || head.equals("Object")) {
                return "java.lang." + name;
            }
            return name + tail;
        }

        ParsedType parseType(Type t) {
            if (t.isPrimitiveType()) {
                return new ParsedType(
                        ParsedTypeKind.PRIMITIVE, t.asPrimitiveType().asString(), TList.of());
            }
            if (t.isArrayType()) {
                var component = t.asArrayType().getComponentType();
                if (component.isPrimitiveType()
                        && component.asPrimitiveType().asString().equals("byte")) {
                    return new ParsedType(ParsedTypeKind.BYTE_ARRAY, "byte[]", TList.of());
                }
                throw new IllegalStateException("Unsupported array type: " + t);
            }
            if (t instanceof ClassOrInterfaceType cit) {
                var name = cit.getNameAsString();

                var scopeOpt = cit.getScope();
                if (scopeOpt.isPresent()) {
                    name = scopeOpt.get().toString() + "." + cit.getNameAsString();
                }
                var typeArgs = new TList<ParsedType>();
                var taOpt = cit.getTypeArguments();
                if (taOpt.isPresent()) {
                    for (var ta : taOpt.get()) {
                        typeArgs.add(parseType(ta));
                    }
                }
                return new ParsedType(ParsedTypeKind.NAMED, name, typeArgs);
            }
            throw new IllegalStateException("Unsupported type: " + t);
        }

        Opt<String> resolve(String name, String contextQname) {

            if (name.contains(".") && !name.isEmpty() && Character.isLowerCase(name.charAt(0))) {

                return Opt.some(name);
            }
            var ctxOpt = byQname.getOpt(contextQname);
            if (ctxOpt.optIsNone()) return Opt.none();
            var ctx = ctxOpt.optUnwrap();
            String pkg = ctx.packageName;
            Opt<String> sameCuOrPackageResolve;
            _sameCuOrPackageResolve:
            {
                var head = headOf(name);
                var tail = tailOf(name);

                var current = contextQname;
                while (current.contains(".")) {
                    var candidate = current + "." + name;
                    if (byQname.containsKey(candidate)) {
                        sameCuOrPackageResolve = Opt.some(candidate);
                        break _sameCuOrPackageResolve;
                    }
                    current = current.substring(0, current.lastIndexOf('.'));
                }

                var samePackageOpt = byPackage.getOpt(pkg);
                if (samePackageOpt.optIsSome()) {
                    var samePackage = samePackageOpt.optUnwrap();
                    for (var qn : samePackage) {
                        var last = qn.lastIndexOf('.');
                        var simple = last < 0 ? qn : qn.substring(last + 1);
                        if (simple.equals(head)) {
                            sameCuOrPackageResolve = Opt.some(qn + tail);
                            break _sameCuOrPackageResolve;
                        }
                    }
                }

                if (Set.of(
                                "String",
                                "Integer",
                                "Long",
                                "Double",
                                "Float",
                                "Boolean",
                                "Short",
                                "Byte",
                                "Character",
                                "Object")
                        .contains(head)) {
                    {
                        sameCuOrPackageResolve = Opt.some("java.lang." + name);
                        break _sameCuOrPackageResolve;
                    }
                }
                {
                    sameCuOrPackageResolve = Opt.none();
                    break _sameCuOrPackageResolve;
                }
            }
            var imported = ctx.imports.getOpt(headOf(name));
            return imported.optIsSome() ? Opt.some(imported.optUnwrap() + tailOf(name)) : sameCuOrPackageResolve;
        }

        private static String headOf(String n) {
            var d = n.indexOf('.');
            return d < 0 ? n : n.substring(0, d);
        }

        private static String tailOf(String n) {
            var d = n.indexOf('.');
            return d < 0 ? "" : n.substring(d);
        }
    }
}
