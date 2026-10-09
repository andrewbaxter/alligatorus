package compilerxyz.lint.reach;

import compilerxyz.lint.convention.Pom;
import compilerxyz.lint.convention.StructureChecks;
import compilerxyz.prelude.util.IList;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import spoon.Launcher;
import spoon.reflect.CtModel;

public final class AnalysisMain {
    private AnalysisMain() {}

    public static final IList<String> GENERATED_ROOTS = IList.of("codegen", "langserver");

    public static void main(String[] args) throws IOException {
        Path base = Path.of(args.length > 0 ? args[0] : ".");
        Path staged = base.resolve("lint/target/spoon-src");
        if (Files.exists(staged)) {
            try (var walk = Files.walk(staged)) {
                walk.sorted(Comparator.reverseOrder()).forEach(p -> p.toFile().delete());
            }
        }
        Files.createDirectories(staged);
        List<String> modules = new Pom(base.resolve("pom.xml")).modules();
        List<String> sourceRoots = new ArrayList<>();
        for (String module : modules) {
            sourceRoots.add(module + "/src/main/java");
            sourceRoots.add(module + "/src/test/java");
            for (String generated : GENERATED_ROOTS) sourceRoots.add(module + "/target/generated-sources/" + generated);
        }
        for (String root : sourceRoots) {
            Path src = base.resolve(root);
            if (!Files.isDirectory(src)) continue;
            try (var walk = Files.walk(src)) {
                for (Path ssP : (Iterable<Path>) walk.filter(x -> x.toString().endsWith(".java")
                        && !x.getFileName().toString().equals("module-info.java"))::iterator) {
                    Path target = staged.resolve(src.relativize(ssP));
                    Files.createDirectories(target.getParent());
                    Files.copy(ssP, target, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }

        Set<String> generatedFiles = new HashSet<>();
        for (String module : modules) {
            for (String generated : GENERATED_ROOTS) {
                Path sffSrc = base.resolve(module + "/target/generated-sources/" + generated);
                if (!Files.isDirectory(sffSrc)) continue;
                try (var walk = Files.walk(sffSrc)) {
                    for (Path sffP :
                            (Iterable<Path>) walk.filter(x -> x.toString().endsWith(".java"))::iterator) {
                        generatedFiles.add(staged.resolve(sffSrc.relativize(sffP))
                                .toAbsolutePath()
                                .normalize()
                                .toString());
                    }
                }
            }
        }

        Set<String> dispatchRoots = new HashSet<>();
        for (String module : modules) {
            Path rootsFile = base.resolve(module + "/target/generated-sources/codegen/dispatch-roots.txt");
            if (!Files.exists(rootsFile)) continue;
            for (String line : Files.readAllLines(rootsFile)) {
                if (!line.isEmpty()) dispatchRoots.add(line);
            }
        }

        Set<String> classpath = new LinkedHashSet<>();
        for (String module : modules) {
            Path cpFile = base.resolve(module + "/target/analysis-classpath.txt");
            if (!Files.exists(cpFile)) {
                throw new IllegalStateException("missing " + cpFile + "; run the full build so the"
                        + " analysis-classpath execution has written every module's classpath");
            }
            for (String entry : Files.readString(cpFile).trim().split(File.pathSeparator)) {
                if (!entry.isBlank()) classpath.add(entry);
            }
        }

        Launcher launcher = new Launcher();
        launcher.getEnvironment().setComplianceLevel(21);
        launcher.getEnvironment().setNoClasspath(false);
        launcher.getEnvironment().setSourceClasspath(classpath.toArray(String[]::new));
        launcher.getEnvironment().setCommentEnabled(false);
        launcher.getEnvironment().setIgnoreDuplicateDeclarations(true);
        launcher.addInputResource(staged.toString());
        launcher.buildModel();
        CtModel model = launcher.getModel();

        var index = new ModelIndex(model);
        List<ConventionChecks.Finding> conventionFindings = new ArrayList<>();
        conventionFindings.addAll(new ConventionChecks(generatedFiles).run(index));
        conventionFindings.addAll(new ImmutabilityChecks(generatedFiles).run(index));
        conventionFindings.addAll(new StateChecks(generatedFiles).run(index));
        conventionFindings.addAll(new InheritanceChecks(generatedFiles).run(index));
        conventionFindings.addAll(new VariableChecks(generatedFiles).run(index));
        conventionFindings.addAll(new NamingChecks(generatedFiles).run(index));
        conventionFindings.addAll(new AspectChecks(generatedFiles).run(index));
        conventionFindings.addAll(new TypecheckConvertChecks(generatedFiles).run(index));
        conventionFindings.addAll(new StructureChecks(base).run());
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (var f : conventionFindings) counts.merge(f.check(), 1, Integer::sum);
        System.out.println("=== Convention checks (" + conventionFindings.size() + " findings) ===");
        counts.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .forEach(e -> System.out.printf("  %4d  %s%n", e.getValue(), e.getKey()));
        StringBuilder detail = new StringBuilder();
        conventionFindings.stream()
                .sorted(Comparator.comparing(ConventionChecks.Finding::file)
                        .thenComparingInt(ConventionChecks.Finding::line))
                .forEach(f -> {
                    String path = f.file();
                    int i = path.indexOf("spoon-src/");
                    String sfShort = i >= 0 ? path.substring(i + "spoon-src/".length()) : path;
                    detail.append(String.format("%s:%d [%s] %s%n", sfShort, f.line(), f.check(), f.message()));
                });
        Path conventionsReport = Path.of("lint/target/conventions-report.txt");
        Files.createDirectories(conventionsReport.getParent());
        Files.writeString(conventionsReport, detail.toString());
        System.out.println("  (per-location detail in " + conventionsReport + ")");
        System.out.println();

        int reachabilityFindings = new ReachabilityMain(generatedFiles, dispatchRoots).run(model, index);

        int total = conventionFindings.size() + reachabilityFindings;
        if (total > 0) {
            throw new IllegalStateException(total + " lint violations — see lint/target/conventions-report.txt"
                    + " and lint/target/reachability-report.txt");
        }
    }
}
