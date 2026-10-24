package compilerxyz.main.tests;

import static compilerxyz.main.tests.T0130DropThrowTest.makeCanaryInstance;
import static org.junit.jupiter.api.Assertions.assertEquals;

import compilerxyz.main.syntax.SourceModule;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TMap;
import org.junit.jupiter.api.Test;

public final class T0224MatchPayloadDropTest {
    @Test
    public void test00000MatchCasePayloadDropsAtCaseEnd() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(TList.of(
                        mb.bind("canary", mb.lower(makeCanaryInstance(mb, "Canary", "dropped"))),
                        mb.bind(
                                "r",
                                mb.call(
                                        mb.field(mb.call(mb.typesEresult(), mb.scopeUse("canary")), mb.str("ok")),
                                        mb.as(mb.as(mb.str("X"), mb.typesString()), mb.scopeUse("canary")))),
                        mb.matchOf(
                                mb.scopeUse("r"),
                                mb.str("p"),
                                TList.of(
                                        new TestMb.Branch(mb.str("ok"), mb.call(mb.log(), mb.str("in"))),
                                        new TestMb.Branch(mb.str("errs"), mb.call(mb.log(), mb.str("errs")))),
                                Opt.none()),
                        mb.call(mb.log(), mb.str("after")))));
        var res = T0130DropThrowTest.compileOk(modules);
        var messages = new TList<String>();
        for (var l : res.logs()) messages.add(l.message());
        assertEquals(TList.of("in", "dropped", "after"), messages);
    }
}
