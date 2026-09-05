package compilerxyz.lint.convention;

import compilerxyz.lint.reach.ConventionChecks.Finding;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class StructureChecks {
    private static final String GROUP = "compilerxyz";
    private static final Pattern PACKAGE = Pattern.compile("^\\s*package\\s+([A-Za-z_][\\w.]*)\\s*;");

    private final Path base;
    private final JavaNames names = new JavaNames();

    public StructureChecks(Path base) {
        this.base = base;
    }

    public List<Finding> run() throws IOException {
        List<Finding> findings = new ArrayList<>();
        Path parentPom = base.resolve("pom.xml");
        if (!Files.isRegularFile(parentPom)) {
            findings.add(new Finding("StructureParentPom", rel(parentPom), 1, "parent pom.xml not found"));
            return findings;
        }
        List<String> declared = new Pom(parentPom).modules();

        for (String module : declared) {
            Path pom = base.resolve(module).resolve("pom.xml");
            if (!Files.isRegularFile(pom)) {
                findings.add(new Finding(
                        "StructureModulePom", module + "/pom.xml", 1, "declared module has no pom.xml at " + module));
                continue;
            }
            String expected = "allig3-" + module;
            Set<String> artifacts = new Pom(pom).artifactIds();
            if (!artifacts.contains(expected)) {
                findings.add(new Finding(
                        "StructureModulePom",
                        rel(pom),
                        1,
                        "module '" + module + "' must declare artifactId '" + expected + "' (found " + artifacts
                                + ")"));
            }
        }

        try (var entries = Files.newDirectoryStream(base, Files::isDirectory)) {
            for (Path dir : entries) {
                String name = dir.getFileName().toString();
                if (name.startsWith(".")) continue;
                if (!Files.isRegularFile(dir.resolve("pom.xml"))) continue;
                if (!declared.contains(name)) {
                    findings.add(new Finding(
                            "StructureUnlistedModule",
                            rel(dir.resolve("pom.xml")),
                            1,
                            "directory '" + name + "' is a Maven module but is not listed in parent <modules>"));
                }
            }
        }

        for (String module : declared) {
            String expectedRoot = GROUP + "." + module;
            for (String sourceRoot : List.of("src/main/java", "src/test/java", "target/generated-sources/codegen")) {
                Path root = base.resolve(module).resolve(sourceRoot);
                if (!Files.isDirectory(root)) continue;
                try (var walk = Files.walk(root)) {
                    for (Path entry : (Iterable<Path>) walk.filter(p -> !p.equals(root))::iterator) {
                        String name = entry.getFileName().toString();
                        if (Files.isDirectory(entry)) {
                            if (!JavaNames.PACKAGE_SEGMENT
                                    .matcher(names.bare(name))
                                    .matches()) {
                                findings.add(new Finding(
                                        "StructureDirectoryName",
                                        rel(entry),
                                        1,
                                        "package directory '" + name + "' must be lowercase [a-z0-9_]; "
                                                + JavaNames.UNDERSCORE_RULE));
                            }
                            continue;
                        }
                        if (!name.endsWith(".java")) continue;
                        String stem = name.substring(0, name.length() - ".java".length());
                        if (stem.equals("package-info") || stem.equals("module-info")) continue;
                        if (!JavaNames.UPPER_CAMEL.matcher(stem).matches()) {
                            findings.add(new Finding(
                                    "StructureFileName",
                                    rel(entry),
                                    1,
                                    "java file '" + name + "' must be UpperCamelCase.java (letters and digits only)"));
                        }
                    }
                }
                try (var walk = Files.walk(root)) {
                    for (Path file :
                            (Iterable<Path>) walk.filter(p -> p.toString().endsWith(".java")
                                    && !p.getFileName().toString().equals("module-info.java"))::iterator) {
                        String declaredPackage = "";
                        for (String line : Files.readAllLines(file)) {
                            Matcher packageMatcher = PACKAGE.matcher(line);
                            if (packageMatcher.find()) {
                                declaredPackage = packageMatcher.group(1);
                                break;
                            }
                        }
                        String relFile = rel(file);
                        if (declaredPackage.isEmpty()) {
                            findings.add(new Finding(
                                    "StructureModulePackage",
                                    relFile,
                                    1,
                                    "file has no package; expected under " + expectedRoot));
                            continue;
                        }
                        if (!declaredPackage.equals(expectedRoot) && !declaredPackage.startsWith(expectedRoot + ".")) {
                            findings.add(new Finding(
                                    "StructureModulePackage",
                                    relFile,
                                    1,
                                    "package '" + declaredPackage + "' must start with '" + expectedRoot + "' (module "
                                            + module + ")"));
                        }
                        String dirPackage = root.relativize(file.getParent())
                                .toString()
                                .replace('/', '.')
                                .replace('\\', '.');
                        if (!dirPackage.equals(declaredPackage)) {
                            findings.add(new Finding(
                                    "StructurePackageDir",
                                    relFile,
                                    1,
                                    "package '" + declaredPackage + "' does not match directory '" + dirPackage + "'"));
                        }
                    }
                }
            }
        }
        return findings;
    }

    private String rel(Path p) {
        return base.relativize(p).toString();
    }
}
