package compilerxyz.main.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import compilerxyz.main.eval.CompileId;
import compilerxyz.main.eval.ExprErr;
import compilerxyz.main.eval.ImportStateId;
import compilerxyz.main.eval.SourceId;
import compilerxyz.main.langhelp.Compile;
import compilerxyz.main.langhelp.GlobalContext;
import compilerxyz.main.langhelp.cache.CacheReal;
import compilerxyz.main.meta.Meta;
import compilerxyz.main.meta.MetaProbe;
import compilerxyz.main.meta.MetaSupport;
import compilerxyz.main.meta.Metas;
import compilerxyz.main.safejson.JsonValueObject;
import compilerxyz.main.safejson.JsonValueString;
import compilerxyz.main.syntax.SourceExpr;
import compilerxyz.main.syntax.SourceModule;
import compilerxyz.main.syntax.SourceSpecAbsolute;
import compilerxyz.prelude.util.Result;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TMap;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

public final class T0210TypeMetaTest {
    private static void expectOk(Result<Compile.CompileResult, TList<ExprErr>> res) {
        switch (res) {
            case Result.Ok<Compile.CompileResult, TList<ExprErr>> ok -> {}
            case Result.Err<Compile.CompileResult, TList<ExprErr>> err ->
                throw new AssertionError("compile failed: " + err.error());
        }
    }

    private static TList<String> onlyFields(Metas metas, SourceId source, SourceExpr expr) {
        var rows = metas.metasExprFields(source, expr.id().value());
        assertEquals(1, rows.size(), "expected one field set for expr " + expr.id() + ": " + rows);
        return rows.get(0).names();
    }

    private static Meta onlyHit(TList<MetaProbe.Hit> hits, TList<String> path) {
        assertEquals(1, hits.size(), "expected one evaluation: " + hits);
        var followed = MetaSupport.follow(hits.get(0).xx(), hits.get(0).meta(), path);
        assertTrue(followed.optIsSome(), "path " + path + " resolves");
        return followed.optUnwrap();
    }

    private static Meta scopeMeta(MetaProbe.Hit hit, String name) {
        for (var entry : hit.scope()) {
            if (entry.name().equals(name)) return entry.meta();
        }
        throw new AssertionError("no scope entry " + name + " in " + hit.scope());
    }

    private static TList<String> names(Meta meta) {
        var out = new TList<String>();
        for (var field : MetaSupport.fields(meta)) out.add(field.name());
        return out;
    }

    private record Program(
            TList<SourceExpr> exprs,
            SourceExpr hello,
            SourceExpr world,
            SourceExpr recordExpr,
            SourceExpr recordUse,
            SourceExpr fieldUse) {}

    private static Program program(TestMb mb) {
        var hello = mb.str("hello");
        var world = mb.str("world");
        var recordExpr =
                mb.recordOf(TList.of(new TestMb.RecordPair("a", mb.str("x")), new TestMb.RecordPair("b", mb.num(1.0))));
        var recordUse = mb.scopeUse("r");
        var fieldUse = mb.field(recordUse, mb.str("a"));
        var exprs = TList.of(
                mb.bind("s", hello), mb.bind("t", world), mb.bind("r", recordExpr), mb.call(mb.log(), fieldUse));
        return new Program(exprs, hello, world, recordExpr, recordUse, fieldUse);
    }

    @Test
    public void test00100FieldNamesAndScopeEvents() {
        var mb = new TestMb();
        var program = program(mb);
        var modules = new TMap<String, SourceModule>();
        modules.put("main", new SourceModule.V1(program.exprs()));
        var gxa = Helpers.newGxWithArtifacts(modules);
        expectOk(Helpers.compile(gxa.gx(), new SourceSpecAbsolute.Virtual("main")));
        var metas = gxa.metas();
        var source = new SourceId(0);

        assertEquals(new TList<>(), onlyFields(metas, source, program.hello()));
        assertEquals(TList.of("a", "b"), onlyFields(metas, source, program.recordExpr()));
        assertEquals(TList.of("a", "b"), onlyFields(metas, source, program.recordUse()));
        assertEquals(new TList<>(), onlyFields(metas, source, program.fieldUse()));

        var events = metas.metasScopeEvents(new CompileId(new ImportStateId(0), source));
        var pushes = new TList<String>();
        var pops = new TList<String>();
        for (var ev : events) {
            switch (ev.kind()) {
                case PUSH -> pushes.add(ev.name());
                case POP -> pops.add(ev.name());
                case SET -> {}
                case ENTER -> {}
                case BEGIN -> {}
                case END -> {}
            }
        }
        assertTrue(pushes.contains("s") && pushes.contains("t") && pushes.contains("r"), "pushes: " + pushes);
        assertTrue(pushes.indexOf("s") < pushes.indexOf("r"), "pushes keep source order: " + pushes);
        assertTrue(pops.contains("s") && pops.contains("r"), "pops: " + pops);
    }

    @Test
    public void test00150ProbeSeesTypeAndScopeAtExpression() {
        var mb = new TestMb();
        var program = program(mb);
        var modules = new TMap<String, SourceModule>();
        modules.put("main", new SourceModule.V1(program.exprs()));
        var gxa = Helpers.newGxWithArtifacts(modules);
        var spec = new SourceSpecAbsolute.Virtual("main");
        var module = Helpers.moduleOf(gxa.gx(), spec);

        var fieldHits = Helpers.probe(gxa, spec, module, program.fieldUse());
        assertEquals("string_const", onlyHit(fieldHits, new TList<>()).kind);
        var scopeNames = new TList<String>();
        for (var entry : fieldHits.get(0).scope()) scopeNames.add(entry.name());
        assertEquals(TList.of("builtins", "s", "t", "r"), scopeNames);
        assertEquals("string_const", scopeMeta(fieldHits.get(0), "s").kind);
        var rMeta = scopeMeta(fieldHits.get(0), "r");
        assertEquals("binding_record", rMeta.kind);
        assertEquals(TList.of("a", "b"), names(rMeta));
        assertEquals(
                "string_const",
                MetaSupport.follow(fieldHits.get(0).xx(), rMeta, TList.of("a")).optUnwrap().kind);

        var recordHits = Helpers.probe(gxa, spec, module, program.recordExpr());
        assertEquals("record", onlyHit(recordHits, new TList<>()).kind);
        assertEquals("string_const", onlyHit(recordHits, TList.of("a")).kind);
        assertTrue(
                MetaSupport.follow(recordHits.get(0).xx(), recordHits.get(0).meta(), TList.of("zzz"))
                        .optIsNone(),
                "a missing field resolves to nothing");
        assertTrue(gxa.artifacts().data.size() == 0, "the probe stores no artifacts: " + gxa.artifacts().data);
    }

    @Test
    public void test00200FieldNamesPersistAcrossCacheInstances(@TempDir Path tmp) throws IOException {
        var mb = new TestMb();
        var program = program(mb);
        var src = tmp.resolve("src");
        Files.createDirectories(src);
        var cacheDir = tmp.resolve("cache");
        var mainFile = src.resolve("main.at");
        Helpers.writeModule(mainFile, program.exprs());
        var spec = new SourceSpecAbsolute.Local(mainFile.toString());

        SourceId source;
        try (var cache = new CacheReal(cacheDir)) {
            var gx = GlobalContext.newContext(cache, cache, cache);
            expectOk(Helpers.compile(gx, spec));
            source = Helpers.moduleOf(gx, spec).id();
            assertEquals(TList.of("a", "b"), onlyFields(cache, source, program.recordExpr()));
        }
        try (var cache = new CacheReal(cacheDir)) {
            var gx = GlobalContext.newContext(cache, cache, cache);
            assertEquals(TList.of("a", "b"), onlyFields(cache, source, program.recordExpr()), "rows survive reopen");
            var events = cache.metasScopeEvents(new CompileId(new ImportStateId(0), source));
            assertTrue(!events.isEmpty(), "scope events survive reopen");
            expectOk(Helpers.compile(gx, spec));
            assertEquals(
                    TList.of("a", "b"), onlyFields(cache, source, program.recordExpr()), "cached compile keeps rows");
        }
    }

    @Test
    public void test00300ProbeSeesImportedType(@TempDir Path tmp) throws IOException {
        var e = new TestMbShorthand();
        var src = tmp.resolve("src");
        Files.createDirectories(src);
        var cacheDir = tmp.resolve("cache");
        var libFile = src.resolve("lib.at");
        var mainFile = src.resolve("main.at");
        Helpers.writeModule(
                libFile,
                TList.of(e.ret(e.lower(e.seq(
                        e.let("d", e.define()),
                        e.let("obj", e.defObject(e.v("d"), "thing")),
                        e.objectImpl(e.v("obj"), e.rec(e.p("name", e.mb.typesString()))),
                        e.methodImpl(
                                e.v("obj"),
                                "name",
                                e.mb.tupleOf(),
                                e.retSome(e.mb.typesString()),
                                e.stage(e.fld(e.v("inner"), "name"))),
                        e.let("mk", e.defFunc(e.v("d"), "mk")),
                        e.funcImpl(
                                e.v("mk"),
                                e.mb.typesString(),
                                e.retSome(e.fld(e.v("obj"), "type")),
                                e.mb.optNoneType(),
                                e.stage(e.call(e.lower(e.fld(e.v("obj"), "new")), e.rec(e.p("name", e.v("arg")))))),
                        e.finish(e.v("d")),
                        e.ret(e.rec(e.p("mk", e.fld(e.v("mk"), "func")))))))));
        var instance = e.call(e.fld(e.v("r"), "mk"), e.s("first"));
        var importEntry = new SourceExpr.RecordEntry(e.mb.str("v"), e.mb.sourceLocal(e.mb.str("lib.at")));
        Helpers.writeModule(
                mainFile,
                TList.of(e.seq(
                        e.let("r", e.mb.field(e.mb.importRecord(TList.of(importEntry)), e.mb.str("v"))),
                        e.let("inst", instance),
                        e.call(e.mb.log(), e.callM(e.v("inst"), "name", e.tup())))));
        var mainSpec = new SourceSpecAbsolute.Local(mainFile.toString());

        try (var cache = new CacheReal(cacheDir)) {
            var gx = GlobalContext.newContext(cache, cache, cache);
            expectOk(Helpers.compile(gx, mainSpec));
        }
        try (var cache = new CacheReal(cacheDir)) {
            var gx = GlobalContext.newContext(cache, cache, cache);
            var module = Helpers.moduleOf(gx, mainSpec);
            var hits = Compile.probe(
                    cache, cache, cache, mainSpec, module, instance.id().value());
            var instanceMeta = onlyHit(hits, new TList<>());
            assertEquals("source_object", instanceMeta.kind);
            var instanceData = (JsonValueObject) instanceMeta.data;
            assertEquals(
                    "thing",
                    ((JsonValueString) instanceData.entries().getOpt("name").optUnwrap()).value());
            assertEquals(TList.of("name"), names(instanceMeta));
            assertEquals("source_method", onlyHit(hits, TList.of("name")).kind);
            assertEquals("string_dynamic", onlyHit(hits, TList.of("name", "ret")).kind);
        }
    }
}
