package compilerxyz.main.langhelp.serve;

import compilerxyz.main.eval.CompileId;
import compilerxyz.main.eval.CompileModule;
import compilerxyz.main.eval.CompileRes;
import compilerxyz.main.eval.CompileResResult;
import compilerxyz.main.eval.ExprSubErr;
import compilerxyz.main.eval.ImportStateId;
import compilerxyz.main.eval.SourceId;
import compilerxyz.main.langhelp.Compile;
import compilerxyz.main.langhelp.Compiles;
import compilerxyz.main.langhelp.GlobalContext;
import compilerxyz.main.langhelp.SourceModuleToModule;
import compilerxyz.main.langhelp.Sources;
import compilerxyz.main.langhelp.cache.CacheReal;
import compilerxyz.main.langserver.AnnounceCompiled;
import compilerxyz.main.langserver.AnnounceDirty;
import compilerxyz.main.langserver.AnnounceRemoved;
import compilerxyz.main.langserver.AnnounceSettled;
import compilerxyz.main.langserver.ClientMessage;
import compilerxyz.main.langserver.CompileError;
import compilerxyz.main.langserver.Compiled;
import compilerxyz.main.langserver.ExprFields;
import compilerxyz.main.langserver.FileState;
import compilerxyz.main.langserver.Location;
import compilerxyz.main.langserver.Log;
import compilerxyz.main.langserver.MetaField;
import compilerxyz.main.langserver.Related;
import compilerxyz.main.langserver.RequestExprFields;
import compilerxyz.main.langserver.RequestExprMeta;
import compilerxyz.main.langserver.RequestFileState;
import compilerxyz.main.langserver.RequestFlush;
import compilerxyz.main.langserver.RequestPause;
import compilerxyz.main.langserver.RequestScopeEntryMeta;
import compilerxyz.main.langserver.RequestScopeNames;
import compilerxyz.main.langserver.RequestSourceRead;
import compilerxyz.main.langserver.Response;
import compilerxyz.main.langserver.ResponseExprFields;
import compilerxyz.main.langserver.ResponseExprMeta;
import compilerxyz.main.langserver.ResponseFileState;
import compilerxyz.main.langserver.ResponseFlush;
import compilerxyz.main.langserver.ResponsePause;
import compilerxyz.main.langserver.ResponseScopeEntryMeta;
import compilerxyz.main.langserver.ResponseScopeNames;
import compilerxyz.main.langserver.ResponseSourceRead;
import compilerxyz.main.langserver.ScopeNames;
import compilerxyz.main.langserver.ServerMessageEv;
import compilerxyz.main.langserver.ServerMessageRes;
import compilerxyz.main.langserver.Source;
import compilerxyz.main.langserver.SourceLocal;
import compilerxyz.main.langserver.SourceRemote;
import compilerxyz.main.meta.Meta;
import compilerxyz.main.meta.MetaProbe;
import compilerxyz.main.meta.MetaSupport;
import compilerxyz.main.syntax.SourceExpr;
import compilerxyz.main.syntax.SourceModule;
import compilerxyz.main.syntax.SourceSpecAbsolute;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.Result;
import compilerxyz.prelude.util.TList;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.TreeSet;
import java.util.concurrent.LinkedBlockingQueue;

public final class ServeCompiler {
    public sealed interface Job {
        record Changes(TList<ServeWatcher.Change> changes) implements Job {}

        record Message(ClientMessage message) implements Job {}

        record Stop() implements Job {}
    }

    private final Path dir;
    private final CacheReal cache;
    private final GlobalContext gx;
    private final ServeWatcher watcher;
    private final ServeProtocol protocol;
    private final LinkedBlockingQueue<Job> jobs = new LinkedBlockingQueue<>();
    private boolean paused = false;
    private final TreeSet<Path> queued = new TreeSet<>();
    private final TreeSet<Path> dirty = new TreeSet<>();

    public ServeCompiler(Path dir, CacheReal cache, ServeWatcher watcher, ServeProtocol protocol) {
        this.dir = dir;
        this.cache = cache;
        this.gx = GlobalContext.newContext(cache, cache, cache);
        this.watcher = watcher;
        this.protocol = protocol;
    }

    public void compilerPost(Job job) {
        jobs.add(job);
    }

    public void compilerRun() {
        for (var module : watcher.watcherModules()) markDirty(module);
        runWave();
        while (true) {
            Job job;
            try {
                job = jobs.take();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return;
            }
            switch (job) {
                case Job.Stop s -> {
                    return;
                }
                case Job.Changes c -> {
                    applyChanges(c.changes());
                    if (!paused) runWave();
                }
                case Job.Message m -> {
                    Response res =
                            switch (m.message().req()) {
                                case RequestPause p -> {
                                    paused = p.paused();
                                    if (!paused) runWave();
                                    yield new ResponsePause();
                                }
                                case RequestFlush f -> {
                                    applyChanges(watcher.watcherDrain(false));
                                    yield new ResponseFlush(runWave());
                                }
                                case RequestFileState s -> {
                                    var compiles = new TList<Compiled>();
                                    for (var unit : cache.compilesUnitsOf(
                                            cache.sourcesIdFor(new SourceSpecAbsolute.Local(s.path())))) {
                                        var stored = cache.compilesGetStored(unit);
                                        if (stored.optIsSome()) compiles.add(compiled(unit, stored.optUnwrap()));
                                    }
                                    yield new ResponseFileState(
                                            new FileState(compiles, dirty.contains(Path.of(s.path()))));
                                }
                                case RequestSourceRead r -> {
                                    yield new ResponseSourceRead(
                                            switch (cache.sourcesGetText(specOf(r.source()))) {
                                                case Result.Ok<String, String> ok -> Opt.some(ok.value());
                                                case Result.Err<String, String> err -> Opt.none();
                                            });
                                }
                                case RequestExprFields r -> {
                                    var sid = cache.sourcesIdFor(specOf(r.source()));
                                    var out = new TList<ExprFields>();
                                    for (var row : cache.metasExprFields(sid, r.expr())) {
                                        out.add(new ExprFields(
                                                row.compile().importState().value(),
                                                row.names(),
                                                source(row.compile().source())));
                                    }
                                    yield new ResponseExprFields(out);
                                }
                                case RequestScopeNames r -> {
                                    var spec = specOf(r.source());
                                    var sid = cache.sourcesIdFor(spec);
                                    Opt<Long> statement =
                                            switch (cache.sourcesGet(spec)) {
                                                case Result.Ok<Sources.SourceResolved, String> ok ->
                                                    switch (ok.value()) {
                                                        case Sources.SourceResolved.Module mod -> {
                                                            Opt<Long> found = Opt.none();
                                                            var roots =
                                                                    switch (mod.module()) {
                                                                        case SourceModule.V1 v1 -> v1.exprs();
                                                                    };
                                                            for (var root : roots) {
                                                                found = statementOf(
                                                                        root,
                                                                        r.expr(),
                                                                        root.id()
                                                                                .value());
                                                                if (found.optIsSome()) break;
                                                            }
                                                            yield found;
                                                        }
                                                        case Sources.SourceResolved.Bundle b -> Opt.none();
                                                        case Sources.SourceResolved.File f -> Opt.none();
                                                    };
                                                case Result.Err<Sources.SourceResolved, String> err -> Opt.none();
                                            };
                                    var scopes = new TList<ScopeNames>();
                                    if (statement.optIsSome()) {
                                        for (var unit : cache.compilesUnitsOf(sid)) {
                                            var frames = new ArrayList<TList<String>>();
                                            frames.add(new TList<>());
                                            Opt<TList<String>> names = Opt.none();
                                            for (var ev : cache.metasScopeEvents(unit)) {
                                                var top = frames.get(frames.size() - 1);
                                                switch (ev.kind()) {
                                                    case BEGIN -> frames.add(new TList<>());
                                                    case END -> {
                                                        if (frames.size() > 1) frames.remove(frames.size() - 1);
                                                    }
                                                    case PUSH -> top.add(ev.name());
                                                    case SET -> {}
                                                    case POP -> {
                                                        for (int i = top.size() - 1; i >= 0; i--) {
                                                            if (top.get(i).equals(ev.name())) {
                                                                top.removeAt(i);
                                                                break;
                                                            }
                                                        }
                                                    }
                                                    case ENTER -> {
                                                        if (ev.loc().source().equals(sid)
                                                                && ev.loc()
                                                                        .expr()
                                                                        .optIsSome()
                                                                && ev.loc()
                                                                        .expr()
                                                                        .optUnwrap()
                                                                        .equals(statement.optUnwrap())) {
                                                            names = Opt.some(TList.from(top));
                                                        }
                                                    }
                                                }
                                                if (names.optIsSome()) break;
                                            }
                                            if (names.optIsNone()) continue;
                                            scopes.add(new ScopeNames(
                                                    unit.importState().value(),
                                                    names.optUnwrap(),
                                                    source(unit.source())));
                                        }
                                    }
                                    yield new ResponseScopeNames(scopes);
                                }
                                case RequestExprMeta r -> {
                                    var out = new TList<compilerxyz.main.langserver.Meta>();
                                    var seen = Collections.newSetFromMap(new IdentityHashMap<Meta, Boolean>());
                                    for (var hit : probe(r.source(), r.expr())) {
                                        var followed = MetaSupport.follow(hit.xx(), hit.meta(), r.path());
                                        if (followed.optIsNone() || !seen.add(followed.optUnwrap())) continue;
                                        out.add(metaRecord(followed.optUnwrap()));
                                    }
                                    yield new ResponseExprMeta(out);
                                }
                                case RequestScopeEntryMeta r -> {
                                    var out = new TList<compilerxyz.main.langserver.Meta>();
                                    var seen = Collections.newSetFromMap(new IdentityHashMap<Meta, Boolean>());
                                    for (var hit : probe(r.source(), r.expr())) {
                                        Opt<Meta> entry = Opt.none();
                                        for (var scopeEntry : hit.scope()) {
                                            if (scopeEntry.name().equals(r.name())) entry = Opt.some(scopeEntry.meta());
                                        }
                                        if (entry.optIsNone()) continue;
                                        var followed = MetaSupport.follow(hit.xx(), entry.optUnwrap(), r.path());
                                        if (followed.optIsNone() || !seen.add(followed.optUnwrap())) continue;
                                        out.add(metaRecord(followed.optUnwrap()));
                                    }
                                    yield new ResponseScopeEntryMeta(out);
                                }
                            };
                    protocol.protocolWrite(new ServerMessageRes(m.message().id(), res));
                }
            }
        }
    }

    private void applyChanges(TList<ServeWatcher.Change> changes) {
        for (var change : changes) {
            switch (change) {
                case ServeWatcher.Change.Changed c -> invalidate(c.path(), false);
                case ServeWatcher.Change.Deleted d -> invalidate(d.path(), true);
            }
        }
    }

    private void invalidate(Path path, boolean deleted) {
        var sid = cache.sourcesIdFor(new SourceSpecAbsolute.Local(path.toString()));
        var affected = new HashSet<CompileId>();
        var pending = new ArrayDeque<CompileId>();
        pending.add(new CompileId(new ImportStateId(0), sid));
        for (var unit : cache.compilesUnitsOf(sid)) pending.add(unit);
        for (var unit : cache.compilesDataUsers(path.toString())) pending.add(unit);
        while (!pending.isEmpty()) {
            var unit = pending.poll();
            if (!affected.add(unit)) continue;
            for (var dependent : cache.compilesDependents(unit.source())) pending.add(dependent);
        }
        var paths = new TreeSet<Path>();
        for (var unit : affected) {
            cache.compilesEvict(unit);
            var spec = cache.sourcesSpecOf(unit.source());
            if (spec.optIsNone()) continue;
            switch (spec.optUnwrap()) {
                case SourceSpecAbsolute.Local l -> paths.add(Path.of(l.path()));
                case SourceSpecAbsolute.Remote r -> {}
                case SourceSpecAbsolute.Virtual v -> {}
            }
        }
        for (var p : paths) {
            if (!p.startsWith(dir)) continue;
            if (p.equals(path) && deleted) {
                dirty.remove(p);
                queued.remove(p);
                protocol.protocolWrite(new ServerMessageEv(new AnnounceRemoved(p.toString())));
            } else {
                markDirty(p);
            }
        }
    }

    private void markDirty(Path path) {
        queued.add(path);
        if (dirty.add(path)) protocol.protocolWrite(new ServerMessageEv(new AnnounceDirty(path.toString())));
    }

    private TList<Compiled> runWave() {
        var roots = new TList<Path>();
        roots.addAll(queued);
        queued.clear();
        var reported = new HashSet<CompileId>();
        var out = new TList<Compiled>();
        for (var root : roots) {
            if (!Files.isRegularFile(root)) continue;
            var spec = new SourceSpecAbsolute.Local(root.toString());
            Sources.SourceResolved.Module module;
            switch (cache.sourcesGet(spec)) {
                case Result.Err<Sources.SourceResolved, String> err -> {
                    var now = System.currentTimeMillis();
                    var error = new CompileError(
                            new Location(Opt.none(), new SourceLocal(root.toString())), err.error(), new TList<>());
                    var compiled =
                            new Compiled(now, TList.of(error), 0, new TList<>(), new SourceLocal(root.toString()), now);
                    protocol.protocolWrite(new ServerMessageEv(new AnnounceCompiled(compiled)));
                    out.add(compiled);
                    continue;
                }
                case Result.Ok<Sources.SourceResolved, String> ok -> {
                    switch (ok.value()) {
                        case Sources.SourceResolved.Module m -> module = m;
                        case Sources.SourceResolved.Bundle b ->
                            throw new IllegalStateException("module path resolved to a bundle: " + root);
                        case Sources.SourceResolved.File f ->
                            throw new IllegalStateException("module path resolved to a plain file: " + root);
                    }
                }
            }
            var rootId = new CompileId(new ImportStateId(0), module.id());
            cache.compilesPrepare(rootId);
            cache.compilesGetOrCompute(rootId, spec, () -> {
                var m = SourceModuleToModule.sourceModuleToModule(module.id(), module.module());
                var cir = CompileModule.compileModuleId(gx, new ImportStateId(0), m, new TList<>(), spec);
                return new Compiles.CompileProduct(
                        cir.res(),
                        module.local(),
                        cir.localDeps(),
                        cir.localData(),
                        cir.exprFields(),
                        cir.scopeEvents());
            });
            var pending = new ArrayDeque<CompileId>();
            pending.add(rootId);
            while (!pending.isEmpty()) {
                var unit = pending.poll();
                if (!reported.add(unit)) continue;
                for (var dep : cache.compilesDependencies(unit)) pending.add(dep);
                var stored = cache.compilesGetStored(unit);
                if (stored.optIsNone()) continue;
                var compiled = compiled(unit, stored.optUnwrap());
                protocol.protocolWrite(new ServerMessageEv(new AnnounceCompiled(compiled)));
                out.add(compiled);
            }
        }
        for (var p : dirty) protocol.protocolWrite(new ServerMessageEv(new AnnounceSettled(p.toString())));
        dirty.clear();
        return out;
    }

    private Compiled compiled(CompileId unit, CompileRes res) {
        var errors = new TList<CompileError>();
        switch (res.result()) {
            case CompileResResult.Ok ok -> {}
            case CompileResResult.Err err -> {
                for (var e : err.errors()) {
                    var location = new Location(e.loc().expr(), source(e.loc().source()));
                    for (var sub : e.errors()) {
                        errors.add(
                                switch (sub) {
                                    case ExprSubErr.Opaque o -> new CompileError(location, o.message(), new TList<>());
                                    case ExprSubErr.ImportFailed f ->
                                        new CompileError(
                                                location,
                                                f.reason(),
                                                TList.of(new Related(
                                                        "the imported module",
                                                        new Location(Opt.none(), source(f.spec())))));
                                });
                    }
                }
            }
        }
        var logs = new TList<Log>();
        for (var log : res.logs()) logs.add(new Log(log.timestampMs(), log.message()));
        return new Compiled(
                res.endMs(), errors, unit.importState().value(), logs, source(unit.source()), res.startMs());
    }

    private static SourceSpecAbsolute specOf(Source source) {
        return switch (source) {
            case SourceLocal l -> new SourceSpecAbsolute.Local(l.path());
            case SourceRemote rm -> SourceSpecAbsolute.sourceSpecAbsoluteParseKey(rm.spec());
        };
    }

    private static Opt<Long> statementOf(SourceExpr expr, long target, long statement) {
        if (expr.id().value() == target) return Opt.some(statement);
        var isSeq =
                switch (expr.variant()) {
                    case SourceExpr.Seq s -> true;
                    default -> false;
                };
        var children = new TList<SourceExpr>();
        switch (expr.variant()) {
            case SourceExpr.As e -> {
                children.add(e.base());
                children.add(e.target());
            }
            case SourceExpr.Bind e -> {
                children.add(e.name());
                children.add(e.value());
            }
            case SourceExpr.Call e -> {
                children.add(e.func());
                children.add(e.arg());
            }
            case SourceExpr.Field e -> {
                children.add(e.base());
                children.add(e.key());
            }
            case SourceExpr.Foreach e -> {
                children.add(e.base());
                children.add(e.elementKey());
                children.add(e.body());
            }
            case SourceExpr.Graft e -> children.add(e.expr());
            case SourceExpr.If e -> {
                for (var branch : e.conditions()) {
                    children.add(branch.condition());
                    children.add(branch.body());
                }
                if (e.fallback().optIsSome()) children.add(e.fallback().optUnwrap());
            }
            case SourceExpr.Labeled e -> children.add(e.inner());
            case SourceExpr.Literal e -> {}
            case SourceExpr.Lower e -> children.add(e.expr());
            case SourceExpr.Match e -> {
                children.add(e.base());
                children.add(e.elementKey());
                for (var branch : e.cases()) {
                    children.add(branch.condition());
                    children.add(branch.body());
                }
                if (e.fallback().optIsSome()) children.add(e.fallback().optUnwrap());
            }
            case SourceExpr.OperatorBinary e -> {
                children.add(e.base());
                children.add(e.reference());
            }
            case SourceExpr.OperatorUnary e -> children.add(e.base());
            case SourceExpr.Record e -> {
                for (var entry : e.entries()) {
                    children.add(entry.key());
                    children.add(entry.value());
                }
            }
            case SourceExpr.Return e -> {
                if (e.value().optIsSome()) children.add(e.value().optUnwrap());
            }
            case SourceExpr.ScopeUse e -> children.add(e.key());
            case SourceExpr.Seq e -> children.addAll(e.exprs());
            case SourceExpr.Set e -> {
                children.add(e.base());
                children.add(e.value());
            }
            case SourceExpr.Stage e -> children.add(e.expr());
            case SourceExpr.Try e -> {
                children.add(e.body());
                if (e.catchClause().optIsSome()) {
                    var c = e.catchClause().optUnwrap();
                    children.add(c.bindingName());
                    children.add(c.exceptionType());
                    children.add(c.body());
                }
                if (e.finallyBlock().optIsSome()) children.add(e.finallyBlock().optUnwrap());
            }
            case SourceExpr.Tuple e -> children.addAll(e.exprs());
        }
        for (var child : children) {
            var found = statementOf(child, target, isSeq ? child.id().value() : statement);
            if (found.optIsSome()) return found;
        }
        return Opt.none();
    }

    private TList<MetaProbe.Hit> probe(Source source, long expr) {
        var spec = specOf(source);
        return switch (cache.sourcesGet(spec)) {
            case Result.Ok<Sources.SourceResolved, String> ok ->
                switch (ok.value()) {
                    case Sources.SourceResolved.Module module -> Compile.probe(cache, cache, cache, spec, module, expr);
                    case Sources.SourceResolved.Bundle b -> new TList<>();
                    case Sources.SourceResolved.File f -> new TList<>();
                };
            case Result.Err<Sources.SourceResolved, String> err -> new TList<>();
        };
    }

    private static compilerxyz.main.langserver.Meta metaRecord(Meta meta) {
        var fields = new TList<MetaField>();
        for (var field : MetaSupport.fields(meta)) {
            fields.add(new MetaField(field.field().data(), field.field().meta().optIsSome(), field.name()));
        }
        return new compilerxyz.main.langserver.Meta(meta.data, fields, meta.kind);
    }

    private Source source(SourceId id) {
        var spec = cache.sourcesSpecOf(id);
        if (spec.optIsNone()) throw new IllegalStateException("compile result names an unknown source id: " + id);
        return source(spec.optUnwrap());
    }

    private static Source source(SourceSpecAbsolute spec) {
        return switch (spec) {
            case SourceSpecAbsolute.Local l -> new SourceLocal(l.path());
            case SourceSpecAbsolute.Remote r -> new SourceRemote(r.sourceSpecAbsoluteSpecKey());
            case SourceSpecAbsolute.Virtual v -> new SourceRemote(v.sourceSpecAbsoluteSpecKey());
        };
    }
}
