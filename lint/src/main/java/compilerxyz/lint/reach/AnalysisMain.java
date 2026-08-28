package compilerxyz.lint.reach;

import compilerxyz.lint.convention.Pom;
import compilerxyz.lint.convention.StructureChecks;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import spoon.Launcher;
import spoon.reflect.CtModel;

public final class AnalysisMain {
    private AnalysisMain() {}

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
            sourceRoots.add(module + "/target/generated-sources/codegen");
        }
        for (String _ssRoot : sourceRoots) {
            Path _ssSrc = base.resolve(_ssRoot);
            if (!Files.isDirectory(_ssSrc)) continue;
            try (var walk = Files.walk(_ssSrc)) {
                for (Path _ssP : (Iterable<Path>) walk.filter(x -> x.toString().endsWith(".java"))::iterator) {
                    Path _ssTarget = staged.resolve(_ssSrc.relativize(_ssP));
                    Files.createDirectories(_ssTarget.getParent());
                    Files.copy(_ssP, _ssTarget, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }

        Set<String> generatedFiles = new HashSet<>();
        for (String module : modules) {
            Path _sffSrc = base.resolve(module + "/target/generated-sources/codegen");
            if (!Files.isDirectory(_sffSrc)) continue;
            try (var walk = Files.walk(_sffSrc)) {
                for (Path _sffP : (Iterable<Path>) walk.filter(x -> x.toString().endsWith(".java"))::iterator) {
                    generatedFiles.add(staged.resolve(_sffSrc.relativize(_sffP))
                            .toAbsolutePath()
                            .normalize()
                            .toString());
                }
            }
        }

        Launcher launcher = new Launcher();
        launcher.getEnvironment().setComplianceLevel(21);
        launcher.getEnvironment().setNoClasspath(true);
        launcher.getEnvironment().setCommentEnabled(false);
        launcher.getEnvironment().setIgnoreDuplicateDeclarations(true);
        launcher.addInputResource(staged.toString());
        launcher.buildModel();
        CtModel model = launcher.getModel();

        List<ConventionChecks.Finding> conventionFindings = new ArrayList<>();
        conventionFindings.addAll(new ConventionChecks(generatedFiles).run(model));
        conventionFindings.addAll(new ImmutabilityChecks(generatedFiles).run(model));
        conventionFindings.addAll(new StateChecks(generatedFiles).run(model));
        conventionFindings.addAll(new InheritanceChecks(generatedFiles).run(model));
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
                    String _sfPath = f.file();
                    int _sfI = _sfPath.indexOf("spoon-src/");
                    String _sfShort = _sfI >= 0 ? _sfPath.substring(_sfI + "spoon-src/".length()) : _sfPath;
                    detail.append(String.format("%s:%d [%s] %s%n", _sfShort, f.line(), f.check(), f.message()));
                });
        Path conventionsReport = Path.of("lint/target/conventions-report.txt");
        Files.createDirectories(conventionsReport.getParent());
        Files.writeString(conventionsReport, detail.toString());
        System.out.println("  (per-location detail in " + conventionsReport + ")");
        System.out.println();

        int reachabilityFindings = new ReachabilityMain(generatedFiles).run(model);

        int total = conventionFindings.size() + reachabilityFindings;
        if (total > 0) {
            throw new IllegalStateException(total + " lint violations — see lint/target/conventions-report.txt"
                    + " and lint/target/reachability-report.txt");
        }
    }
}
