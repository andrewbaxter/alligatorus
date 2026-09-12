package compilerxyz.main.tests;

import static compilerxyz.main.tests.T0130DropThrowTest.compileOk;
import static compilerxyz.main.tests.T0130DropThrowTest.messages;
import static org.junit.jupiter.api.Assertions.assertEquals;

import compilerxyz.main.syntax.SourceExpr;
import compilerxyz.main.syntax.SourceModule;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TMap;
import org.junit.jupiter.api.Test;

public final class T0052OptionSpecializedTest {
    private static SourceExpr optionInt(TestMb mb) {
        return mb.call(mb.typesOption(), mb.typesInt());
    }

    private static SourceExpr logIsSome(TestMb mb, String name) {
        return mb.ifOf(
                TList.of(new TestMb.Branch(
                        mb.field(mb.scopeUse(name), mb.str("is_some")), mb.call(mb.log(), mb.str(name + " some")))),
                Opt.some(mb.call(mb.log(), mb.str(name + " none"))));
    }

    @Test
    public void someNoneValueIsSome() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(mb.seqOf(
                        mb.bind(
                                "o",
                                mb.call(mb.field(optionInt(mb), mb.str("some")), mb.as(mb.num(3.0), mb.typesInt()))),
                        logIsSome(mb, "o"),
                        mb.call(
                                mb.log(),
                                mb.call(
                                        mb.field(mb.field(mb.scopeUse("o"), mb.str("value")), mb.str("to_string")),
                                        mb.tupleOf())),
                        mb.bind("n", mb.call(mb.field(optionInt(mb), mb.str("none")), mb.literalNull())),
                        logIsSome(mb, "n"))));
        assertEquals(TList.of("o some", "3", "n none"), messages(compileOk(modules)));
    }
}
