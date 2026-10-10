package compilerxyz.main.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import compilerxyz.main.eval.CompileId;
import compilerxyz.main.eval.ExprErr;
import compilerxyz.main.eval.ImportStateId;
import compilerxyz.main.eval.SourceId;
import compilerxyz.main.langhelp.Compile;
import compilerxyz.main.langhelp.GlobalContext;
import compilerxyz.main.langhelp.Sources;
import compilerxyz.main.langhelp.cache.CacheReal;
import compilerxyz.main.meta.MetaId;
import compilerxyz.main.meta.Metas;
import compilerxyz.main.safejson.JsonValue;
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
import java.nio.file.attribute.FileTime;
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

    private static JsonValueObject body(Metas metas, MetaId id) {
        var bodyOpt = metas.metasGet(id);
        assertTrue(bodyOpt.optIsSome(), "meta body missing for " + id);
        return (JsonValueObject) bodyOpt.optUnwrap();
    }

    private static String kind(JsonValueObject body) {
        return ((JsonValueString) body.entries().getOpt("kind").optUnwrap()).value();
    }

    private static JsonValueObject fields(JsonValueObject body) {
        return (JsonValueObject) body.entries().getOpt("fields").optUnwrap();
    }

    private static MetaId onlyMeta(Metas metas, SourceId source, SourceExpr expr) {
        var rows = metas.metasForExpr(source, expr.id().value());
        assertEquals(1, rows.size(), "expected one meta for expr " + expr.id() + ": " + rows);
        return rows.get(0).meta();
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
    public void test00100ExpressionMetasAndScopeEvents() {
        var mb = new TestMb();
        var program = program(mb);
        var modules = new TMap<String, SourceModule>();
        modules.put("main", new SourceModule.V1(program.exprs()));
        var gx = Helpers.newGx(modules);
        expectOk(Helpers.compile(gx, new SourceSpecAbsolute.Virtual("main")));
        var metas = gx.metas;
        var source = new SourceId(0);

        var helloMeta = onlyMeta(metas, source, program.hello());
        var worldMeta = onlyMeta(metas, source, program.world());
        assertEquals(helloMeta, worldMeta, "two string literals must share one meta");
        assertEquals("string_const", kind(body(metas, helloMeta)));

        var recordMeta = onlyMeta(metas, source, program.recordExpr());
        var recordBody = body(metas, recordMeta);
        assertEquals("record", kind(recordBody));
        var recordFields = fields(recordBody);
        assertTrue(recordFields.entries().containsKey("a"), "record meta lists field a: " + recordFields);
        assertTrue(recordFields.entries().containsKey("b"), "record meta lists field b: " + recordFields);
        var fieldA = (JsonValueObject) recordFields.entries().getOpt("a").optUnwrap();
        assertTrue(fieldA.entries().containsKey("meta"), "record field a carries its type meta: " + fieldA);

        var useMeta = onlyMeta(metas, source, program.recordUse());
        assertEquals("record", kind(body(metas, useMeta)));
        var fieldMeta = onlyMeta(metas, source, program.fieldUse());
        assertEquals("string_const", kind(body(metas, fieldMeta)));

        var events = metas.metasScopeEvents(new CompileId(new ImportStateId(0), source));
        var pushes = new TList<String>();
        var pops = new TList<String>();
        for (var ev : events) {
            switch (ev.kind()) {
                case PUSH -> {
                    pushes.add(ev.name());
                    assertTrue(ev.meta().optIsSome(), "push carries a meta: " + ev);
                }
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
        for (var ev : events) {
            if (ev.kind() == Metas.ScopeEventKind.PUSH && ev.name().equals("s")) {
                assertEquals(helloMeta, ev.meta().optUnwrap(), "binding meta of s is the string meta");
            }
        }
    }

    private static SourceId sourceIdOf(GlobalContext gx, SourceSpecAbsolute spec) {
        return switch (gx.sources.sourcesGet(spec)) {
            case Result.Ok<Sources.SourceResolved, String> ok ->
                switch (ok.value()) {
                    case Sources.SourceResolved.Module m -> m.id();
                    case Sources.SourceResolved.Bundle b -> throw new AssertionError("bundle");
                    case Sources.SourceResolved.File f -> throw new AssertionError("file");
                };
            case Result.Err<Sources.SourceResolved, String> err -> throw new AssertionError(err.error());
        };
    }

    @Test
    public void test00200MetasPersistAcrossCacheInstances(@TempDir Path tmp) throws IOException {
        var mb = new TestMb();
        var program = program(mb);
        var src = tmp.resolve("src");
        Files.createDirectories(src);
        var cacheDir = tmp.resolve("cache");
        var mainFile = src.resolve("main.at");
        Helpers.writeModule(mainFile, program.exprs());
        var spec = new SourceSpecAbsolute.Local(mainFile.toString());

        MetaId helloMeta;
        JsonValue helloBody;
        SourceId source;
        try (var cache = new CacheReal(cacheDir)) {
            var gx = GlobalContext.newContext(cache, cache, cache, cache);
            expectOk(Helpers.compile(gx, spec));
            source = sourceIdOf(gx, spec);
            helloMeta = onlyMeta(cache, source, program.hello());
            assertEquals(helloMeta, onlyMeta(cache, source, program.world()));
            helloBody = body(cache, helloMeta);
            assertEquals("string_const", kind((JsonValueObject) helloBody));
        }
        try (var cache = new CacheReal(cacheDir)) {
            var gx = GlobalContext.newContext(cache, cache, cache, cache);
            assertEquals(helloMeta, onlyMeta(cache, source, program.hello()), "rows survive reopen");
            assertEquals(helloBody, body(cache, helloMeta), "body survives reopen");
            var events = cache.metasScopeEvents(new CompileId(new ImportStateId(0), source));
            assertTrue(!events.isEmpty(), "scope events survive reopen");
            expectOk(Helpers.compile(gx, spec));
            assertEquals(helloMeta, onlyMeta(cache, source, program.hello()), "cached compile keeps rows");
            var again = onlyMeta(cache, source, program.world());
            assertEquals(helloMeta, again, "rows of the cached compile are unchanged");
        }
    }

    private static SourceExpr importField(TestMb mb, String key, SourceExpr spec) {
        var entry = new SourceExpr.RecordEntry(mb.str(key), spec);
        return mb.field(mb.importRecord(TList.of(entry)), mb.str(key));
    }

    private static SourceExpr objectLibrary(TestMbShorthand e) {
        return e.lower(e.seq(
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
                e.ret(e.rec(e.p("mk", e.fld(e.v("mk"), "func"))))));
    }

    private static SourceExpr instanceProgram(TestMbShorthand e, SourceExpr instance) {
        return e.seq(
                e.let("r", importField(e.mb, "v", e.mb.sourceLocal(e.mb.str("lib.at")))),
                e.let("inst", instance),
                e.call(e.mb.log(), e.callM(e.v("inst"), "name", e.tup())));
    }

    private static String dataName(JsonValueObject body) {
        var data = (JsonValueObject) body.entries().getOpt("data").optUnwrap();
        return ((JsonValueString) data.entries().getOpt("name").optUnwrap()).value();
    }

    @Test
    public void test00300ExportedTypeKeepsItsMetaId(@TempDir Path tmp) throws IOException {
        var e = new TestMbShorthand();
        var src = tmp.resolve("src");
        Files.createDirectories(src);
        var cacheDir = tmp.resolve("cache");
        var libFile = src.resolve("lib.at");
        var mainFile = src.resolve("main.at");
        Helpers.writeModule(libFile, TList.of(e.ret(objectLibrary(e))));
        var instance = e.call(e.fld(e.v("r"), "mk"), e.s("first"));
        Helpers.writeModule(mainFile, TList.of(instanceProgram(e, instance)));
        var mainSpec = new SourceSpecAbsolute.Local(mainFile.toString());

        MetaId instanceMeta;
        try (var cache = new CacheReal(cacheDir)) {
            var gx = GlobalContext.newContext(cache, cache, cache, cache);
            expectOk(Helpers.compile(gx, mainSpec));
            instanceMeta = onlyMeta(cache, sourceIdOf(gx, mainSpec), instance);
            var instanceBody = body(cache, instanceMeta);
            assertEquals("source_object", kind(instanceBody));
            assertEquals("thing", dataName(instanceBody));
            assertTrue(fields(instanceBody).entries().containsKey("name"), "object meta lists its method");
        }

        var instanceAgain = e.call(e.fld(e.v("r"), "mk"), e.s("second"));
        Helpers.writeModule(mainFile, TList.of(instanceProgram(e, instanceAgain)));
        Files.setLastModifiedTime(
                mainFile,
                FileTime.fromMillis(Files.getLastModifiedTime(mainFile).toMillis() + 100000));
        try (var cache = new CacheReal(cacheDir)) {
            var gx = GlobalContext.newContext(cache, cache, cache, cache);
            expectOk(Helpers.compile(gx, mainSpec));
            assertEquals(
                    instanceMeta,
                    onlyMeta(cache, sourceIdOf(gx, mainSpec), instanceAgain),
                    "a fresh process importing the cached type reuses its meta id");
        }
    }
}
