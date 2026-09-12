package compilerxyz.main.tests;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import compilerxyz.main.eval.ExprErr;
import compilerxyz.main.langhelp.Compile;
import compilerxyz.main.syntax.SourceExpr;
import compilerxyz.main.syntax.SourceModule;
import compilerxyz.main.syntax.SourceSpecAbsolute;
import compilerxyz.prelude.util.Opt;
import compilerxyz.prelude.util.Result;
import compilerxyz.prelude.util.TList;
import compilerxyz.prelude.util.TMap;
import org.junit.jupiter.api.Test;

public final class T0130DropThrowTest {
    private static SourceExpr ifaceMethods(TestMb mb, TList<TestMb.RecordPair> methods) {
        var staged = new TList<TestMb.RecordPair>(methods.size());
        for (var m : methods) {
            staged.add(new TestMb.RecordPair(m.key(), mb.stage(m.value())));
        }
        return mb.call(
                mb.field(mb.call(mb.typesMap(), mb.tupleOf(mb.typesConstkey(), mb.typesExpr())), mb.str("new")),
                mb.recordOf(staged));
    }

    static SourceExpr makeCanaryInstance(TestMb mb, String name, String dropMessage) {
        return mb.call(
                mb.lower(mb.seqOf(
                        mb.bind("d", mb.call(mb.define(), mb.tupleOf())),
                        mb.bind("u", mb.call(mb.field(mb.scopeUse("d"), mb.str("object")), mb.str(name))),
                        mb.call(mb.field(mb.scopeUse("u"), mb.str("set_inner")), mb.typesNull()),
                        mb.bind("mk", mb.call(mb.field(mb.scopeUse("d"), mb.str("interface")), mb.tupleOf())),
                        mb.call(mb.field(mb.scopeUse("mk"), mb.str("set_is_aspect")), mb.bool(true)),
                        mb.bind("mk_id", mb.call(mb.field(mb.scopeUse("mk"), mb.str("aspect_id")), mb.tupleOf())),
                        mb.call(
                                mb.field(mb.scopeUse("u"), mb.str("impl_interface")),
                                mb.recordOf(TList.of(
                                        new TestMb.RecordPair("iface_ref", mb.field(mb.scopeUse("mk"), mb.str("type"))),
                                        new TestMb.RecordPair("record", ifaceMethods(mb, TList.of()))))),
                        mb.call(
                                mb.field(mb.scopeUse("u"), mb.str("impl_interface")),
                                mb.recordOf(TList.of(
                                        new TestMb.RecordPair("iface_ref", mb.evalAspectInitialBindDynamic()),
                                        new TestMb.RecordPair(
                                                "record",
                                                ifaceMethods(
                                                        mb,
                                                        TList.of(
                                                                new TestMb.RecordPair(
                                                                        "initial_bind_dynamic",
                                                                        mb.scopeUse("outer")))))))),
                        mb.call(
                                mb.field(mb.scopeUse("u"), mb.str("impl_interface")),
                                mb.recordOf(TList.of(
                                        new TestMb.RecordPair("iface_ref", mb.evalAspectFieldUseDynamic()),
                                        new TestMb.RecordPair(
                                                "record",
                                                ifaceMethods(
                                                        mb,
                                                        TList.of(
                                                                new TestMb.RecordPair(
                                                                        "field_use_dynamic",
                                                                        mb.delegateAspect(
                                                                                mb.typesString(),
                                                                                mb.evalAspectFieldUseDynamic(),
                                                                                "field_use_dynamic",
                                                                                "xx",
                                                                                "loc",
                                                                                "source")))))))),
                        mb.call(
                                mb.field(mb.scopeUse("u"), mb.str("impl_interface")),
                                mb.recordOf(TList.of(
                                        new TestMb.RecordPair("iface_ref", mb.evalValue()),
                                        new TestMb.RecordPair(
                                                "record",
                                                ifaceMethods(
                                                        mb,
                                                        TList.of(
                                                                new TestMb.RecordPair(
                                                                        "type", mb.scopeUse("outer")))))))),
                        mb.call(
                                mb.field(mb.scopeUse("u"), mb.str("impl_interface")),
                                mb.recordOf(TList.of(
                                        new TestMb.RecordPair("iface_ref", mb.evalType()),
                                        new TestMb.RecordPair(
                                                "record",
                                                ifaceMethods(
                                                        mb,
                                                        TList.of(
                                                                new TestMb.RecordPair(
                                                                        "can_cast_from",
                                                                        mb.seqOf(
                                                                                mb.return_(
                                                                                        mb.field(
                                                                                                mb.call(
                                                                                                        mb.field(
                                                                                                                mb
                                                                                                                        .evalType(),
                                                                                                                mb.str(
                                                                                                                        "aspect")),
                                                                                                        mb.tupleOf(
                                                                                                                mb
                                                                                                                        .field(
                                                                                                                                mb
                                                                                                                                        .scopeUse(
                                                                                                                                                "arg"),
                                                                                                                                mb
                                                                                                                                        .str(
                                                                                                                                                "source_type")),
                                                                                                                mb
                                                                                                                        .recordOf(
                                                                                                                                TList
                                                                                                                                        .of(
                                                                                                                                                new TestMb
                                                                                                                                                        .RecordPair(
                                                                                                                                                        "xx",
                                                                                                                                                        mb
                                                                                                                                                                .field(
                                                                                                                                                                        mb
                                                                                                                                                                                .scopeUse(
                                                                                                                                                                                        "arg"),
                                                                                                                                                                        mb
                                                                                                                                                                                .str(
                                                                                                                                                                                        "xx"))),
                                                                                                                                                new TestMb
                                                                                                                                                        .RecordPair(
                                                                                                                                                        "key",
                                                                                                                                                        mb
                                                                                                                                                                .evalAspectStringDynamic()))))),
                                                                                                mb.str("is_some"))))),
                                                                new TestMb.RecordPair(
                                                                        "cast_from",
                                                                        mb.call(
                                                                                mb.field(
                                                                                        mb.call(
                                                                                                mb.typesEresult(),
                                                                                                mb.typesCve()),
                                                                                        mb.str("ok")),
                                                                                mb.call(
                                                                                        mb.evalCodevalue(),
                                                                                        mb.recordOf(
                                                                                                TList.of(
                                                                                                        new TestMb
                                                                                                                .RecordPair(
                                                                                                                "code",
                                                                                                                mb
                                                                                                                        .field(
                                                                                                                                mb
                                                                                                                                        .field(
                                                                                                                                                mb
                                                                                                                                                        .call(
                                                                                                                                                                mb
                                                                                                                                                                        .evalConstEval(),
                                                                                                                                                                mb
                                                                                                                                                                        .recordOf(
                                                                                                                                                                                TList
                                                                                                                                                                                        .of(
                                                                                                                                                                                                new TestMb
                                                                                                                                                                                                        .RecordPair(
                                                                                                                                                                                                        "xx",
                                                                                                                                                                                                        mb
                                                                                                                                                                                                                .field(
                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                .scopeUse(
                                                                                                                                                                                                                                        "arg"),
                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                .str(
                                                                                                                                                                                                                                        "xx"))),
                                                                                                                                                                                                new TestMb
                                                                                                                                                                                                        .RecordPair(
                                                                                                                                                                                                        "staged",
                                                                                                                                                                                                        mb
                                                                                                                                                                                                                .stage(
                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                .graft(
                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                .field(
                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                .scopeUse(
                                                                                                                                                                                                                                                                        "arg"),
                                                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                                                .str(
                                                                                                                                                                                                                                                                        "cv")))))))),
                                                                                                                                                mb
                                                                                                                                                        .str(
                                                                                                                                                                "value")),
                                                                                                                                mb
                                                                                                                                        .str(
                                                                                                                                                "code"))),
                                                                                                        new TestMb
                                                                                                                .RecordPair(
                                                                                                                "value",
                                                                                                                mb.call(
                                                                                                                        mb
                                                                                                                                .evalStack(),
                                                                                                                        mb
                                                                                                                                .recordOf(
                                                                                                                                        TList
                                                                                                                                                .of(
                                                                                                                                                        new TestMb
                                                                                                                                                                .RecordPair(
                                                                                                                                                                "xx",
                                                                                                                                                                mb
                                                                                                                                                                        .field(
                                                                                                                                                                                mb
                                                                                                                                                                                        .scopeUse(
                                                                                                                                                                                                "arg"),
                                                                                                                                                                                mb
                                                                                                                                                                                        .str(
                                                                                                                                                                                                "xx"))),
                                                                                                                                                        new TestMb
                                                                                                                                                                .RecordPair(
                                                                                                                                                                "type",
                                                                                                                                                                mb
                                                                                                                                                                        .scopeUse(
                                                                                                                                                                                "outer"))))))))))),
                                                                new TestMb.RecordPair(
                                                                        "desc", mb.seqOf(mb.return_(mb.str(name)))),
                                                                new TestMb.RecordPair(
                                                                        "aspect",
                                                                        mb.ifOf(
                                                                                TList.of(
                                                                                        new TestMb.Branch(
                                                                                                mb.operatorEq(
                                                                                                        mb.field(
                                                                                                                mb
                                                                                                                        .scopeUse(
                                                                                                                                "arg"),
                                                                                                                mb.str(
                                                                                                                        "key")),
                                                                                                        mb.lower(
                                                                                                                mb
                                                                                                                        .scopeUse(
                                                                                                                                "mk_id"))),
                                                                                                mb.call(
                                                                                                        mb.field(
                                                                                                                mb.call(
                                                                                                                        mb
                                                                                                                                .typesOption(),
                                                                                                                        mb
                                                                                                                                .typesAspect()),
                                                                                                                mb.str(
                                                                                                                        "some")),
                                                                                                        mb.scopeUse(
                                                                                                                "outer"))),
                                                                                        new TestMb.Branch(
                                                                                                mb.operatorEq(
                                                                                                        mb.field(
                                                                                                                mb
                                                                                                                        .scopeUse(
                                                                                                                                "arg"),
                                                                                                                mb.str(
                                                                                                                        "key")),
                                                                                                        mb
                                                                                                                .evalAspectBindId()),
                                                                                                mb.call(
                                                                                                        mb.field(
                                                                                                                mb.call(
                                                                                                                        mb
                                                                                                                                .typesOption(),
                                                                                                                        mb
                                                                                                                                .typesAspect()),
                                                                                                                mb.str(
                                                                                                                        "some")),
                                                                                                        mb.scopeUse(
                                                                                                                "outer"))),
                                                                                        new TestMb.Branch(
                                                                                                mb.operatorEq(
                                                                                                        mb.field(
                                                                                                                mb
                                                                                                                        .scopeUse(
                                                                                                                                "arg"),
                                                                                                                mb.str(
                                                                                                                        "key")),
                                                                                                        mb
                                                                                                                .evalAspectDropDynamicId()),
                                                                                                mb.call(
                                                                                                        mb.field(
                                                                                                                mb.call(
                                                                                                                        mb
                                                                                                                                .typesOption(),
                                                                                                                        mb
                                                                                                                                .typesAspect()),
                                                                                                                mb.str(
                                                                                                                        "some")),
                                                                                                        mb.scopeUse(
                                                                                                                "outer"))),
                                                                                        new TestMb.Branch(
                                                                                                mb.operatorEq(
                                                                                                        mb.field(
                                                                                                                mb
                                                                                                                        .scopeUse(
                                                                                                                                "arg"),
                                                                                                                mb.str(
                                                                                                                        "key")),
                                                                                                        mb
                                                                                                                .evalAspectConsumeDynamicId()),
                                                                                                mb.call(
                                                                                                        mb.field(
                                                                                                                mb.call(
                                                                                                                        mb
                                                                                                                                .typesOption(),
                                                                                                                        mb
                                                                                                                                .typesAspect()),
                                                                                                                mb.str(
                                                                                                                        "some")),
                                                                                                        mb.scopeUse(
                                                                                                                "outer"))),
                                                                                        new TestMb.Branch(
                                                                                                mb.operatorEq(
                                                                                                        mb.field(
                                                                                                                mb
                                                                                                                        .scopeUse(
                                                                                                                                "arg"),
                                                                                                                mb.str(
                                                                                                                        "key")),
                                                                                                        mb
                                                                                                                .evalAspectBindDynamicId()),
                                                                                                mb.call(
                                                                                                        mb.field(
                                                                                                                mb.call(
                                                                                                                        mb
                                                                                                                                .typesOption(),
                                                                                                                        mb
                                                                                                                                .typesAspect()),
                                                                                                                mb.str(
                                                                                                                        "some")),
                                                                                                        mb.scopeUse(
                                                                                                                "outer"))),
                                                                                        new TestMb.Branch(
                                                                                                mb.operatorEq(
                                                                                                        mb.field(
                                                                                                                mb
                                                                                                                        .scopeUse(
                                                                                                                                "arg"),
                                                                                                                mb.str(
                                                                                                                        "key")),
                                                                                                        mb.builtin(
                                                                                                                "eval",
                                                                                                                "aspect_field_use_dynamic_id")),
                                                                                                mb.call(
                                                                                                        mb.field(
                                                                                                                mb.call(
                                                                                                                        mb
                                                                                                                                .typesOption(),
                                                                                                                        mb
                                                                                                                                .typesAspect()),
                                                                                                                mb.str(
                                                                                                                        "some")),
                                                                                                        mb.scopeUse(
                                                                                                                "outer")))),
                                                                                Opt.some(
                                                                                        mb.call(
                                                                                                mb.field(
                                                                                                        mb.call(
                                                                                                                mb
                                                                                                                        .typesOption(),
                                                                                                                mb
                                                                                                                        .typesAspect()),
                                                                                                        mb.str("none")),
                                                                                                mb.literalNull())))),
                                                                new TestMb.RecordPair(
                                                                        "is_equal",
                                                                        mb.seqOf(
                                                                                mb.return_(
                                                                                        mb.field(
                                                                                                mb.call(
                                                                                                        mb
                                                                                                                .evalAspectLookup(),
                                                                                                        mb.tupleOf(
                                                                                                                mb
                                                                                                                        .field(
                                                                                                                                mb
                                                                                                                                        .scopeUse(
                                                                                                                                                "arg"),
                                                                                                                                mb
                                                                                                                                        .str(
                                                                                                                                                "other")),
                                                                                                                mb
                                                                                                                        .lower(
                                                                                                                                mb
                                                                                                                                        .field(
                                                                                                                                                mb
                                                                                                                                                        .scopeUse(
                                                                                                                                                                "mk"),
                                                                                                                                                mb
                                                                                                                                                        .str(
                                                                                                                                                                "type"))),
                                                                                                                mb
                                                                                                                        .field(
                                                                                                                                mb
                                                                                                                                        .scopeUse(
                                                                                                                                                "arg"),
                                                                                                                                mb
                                                                                                                                        .str(
                                                                                                                                                "xx")))),
                                                                                                mb.str("is_some"))))),
                                                                new TestMb.RecordPair(
                                                                        "can_cast_to",
                                                                        mb.seqOf(
                                                                                mb.return_(
                                                                                        mb.operatorEq(
                                                                                                mb.num(1.0),
                                                                                                mb.num(1.0))))),
                                                                new TestMb.RecordPair(
                                                                        "cast_to",
                                                                        mb.call(
                                                                                mb.field(
                                                                                        mb.call(
                                                                                                mb.typesEresult(),
                                                                                                mb.typesCve()),
                                                                                        mb.str("ok")),
                                                                                mb.field(
                                                                                        mb.scopeUse("arg"),
                                                                                        mb.str("cv")))))))))),
                        mb.call(
                                mb.field(mb.scopeUse("u"), mb.str("impl_interface")),
                                mb.recordOf(
                                        TList.of(
                                                new TestMb.RecordPair("iface_ref", mb.evalAspectDropDynamic()),
                                                new TestMb.RecordPair(
                                                        "record",
                                                        ifaceMethods(
                                                                mb,
                                                                TList.of(
                                                                        new TestMb.RecordPair(
                                                                                "drop",
                                                                                mb.seqOf(
                                                                                        mb.bind(
                                                                                                "underlying_code_",
                                                                                                mb.field(
                                                                                                        mb.call(
                                                                                                                mb
                                                                                                                        .field(
                                                                                                                                mb
                                                                                                                                        .evalAspectDropDynamicLight(),
                                                                                                                                mb
                                                                                                                                        .str(
                                                                                                                                                "drop_dynamic_light")),
                                                                                                                mb
                                                                                                                        .tupleOf(
                                                                                                                                mb
                                                                                                                                        .field(
                                                                                                                                                mb
                                                                                                                                                        .call(
                                                                                                                                                                mb
                                                                                                                                                                        .evalAspectLookup(),
                                                                                                                                                                mb
                                                                                                                                                                        .tupleOf(
                                                                                                                                                                                mb
                                                                                                                                                                                        .typesString(),
                                                                                                                                                                                mb
                                                                                                                                                                                        .evalAspectDropDynamicLight(),
                                                                                                                                                                                mb
                                                                                                                                                                                        .field(
                                                                                                                                                                                                mb
                                                                                                                                                                                                        .scopeUse(
                                                                                                                                                                                                                "arg"),
                                                                                                                                                                                                mb
                                                                                                                                                                                                        .str(
                                                                                                                                                                                                                "xx")))),
                                                                                                                                                mb
                                                                                                                                                        .str(
                                                                                                                                                                "value")),
                                                                                                                                mb
                                                                                                                                        .recordOf(
                                                                                                                                                TList
                                                                                                                                                        .of(
                                                                                                                                                                new TestMb
                                                                                                                                                                        .RecordPair(
                                                                                                                                                                        "xx",
                                                                                                                                                                        mb
                                                                                                                                                                                .field(
                                                                                                                                                                                        mb
                                                                                                                                                                                                .scopeUse(
                                                                                                                                                                                                        "arg"),
                                                                                                                                                                                        mb
                                                                                                                                                                                                .str(
                                                                                                                                                                                                        "xx"))),
                                                                                                                                                                new TestMb
                                                                                                                                                                        .RecordPair(
                                                                                                                                                                        "loc",
                                                                                                                                                                        mb
                                                                                                                                                                                .field(
                                                                                                                                                                                        mb
                                                                                                                                                                                                .scopeUse(
                                                                                                                                                                                                        "arg"),
                                                                                                                                                                                        mb
                                                                                                                                                                                                .str(
                                                                                                                                                                                                        "loc"))),
                                                                                                                                                                new TestMb
                                                                                                                                                                        .RecordPair(
                                                                                                                                                                        "cv",
                                                                                                                                                                        mb
                                                                                                                                                                                .field(
                                                                                                                                                                                        mb
                                                                                                                                                                                                .scopeUse(
                                                                                                                                                                                                        "arg"),
                                                                                                                                                                                        mb
                                                                                                                                                                                                .str(
                                                                                                                                                                                                        "cv"))))))),
                                                                                                        mb.str(
                                                                                                                "value"))),
                                                                                        mb.return_(
                                                                                                mb.constEvalCode(
                                                                                                        mb.call(
                                                                                                                mb
                                                                                                                        .evalConstEval(),
                                                                                                                mb
                                                                                                                        .recordOf(
                                                                                                                                TList
                                                                                                                                        .of(
                                                                                                                                                new TestMb
                                                                                                                                                        .RecordPair(
                                                                                                                                                        "xx",
                                                                                                                                                        mb
                                                                                                                                                                .field(
                                                                                                                                                                        mb
                                                                                                                                                                                .scopeUse(
                                                                                                                                                                                        "arg"),
                                                                                                                                                                        mb
                                                                                                                                                                                .str(
                                                                                                                                                                                        "xx"))),
                                                                                                                                                new TestMb
                                                                                                                                                        .RecordPair(
                                                                                                                                                        "staged",
                                                                                                                                                        mb
                                                                                                                                                                .stage(
                                                                                                                                                                        mb
                                                                                                                                                                                .seqOf(
                                                                                                                                                                                        mb
                                                                                                                                                                                                .call(
                                                                                                                                                                                                        mb
                                                                                                                                                                                                                .log(),
                                                                                                                                                                                                        mb
                                                                                                                                                                                                                .str(
                                                                                                                                                                                                                        dropMessage)),
                                                                                                                                                                                        mb
                                                                                                                                                                                                .return_(
                                                                                                                                                                                                        mb
                                                                                                                                                                                                                .graft(
                                                                                                                                                                                                                        mb
                                                                                                                                                                                                                                .scopeUse(
                                                                                                                                                                                                                                        "underlying_code_"))))))))))))))))))),
                        mb.call(
                                mb.field(mb.scopeUse("u"), mb.str("impl_interface")),
                                mb.recordOf(TList.of(
                                        new TestMb.RecordPair("iface_ref", mb.evalBinding()),
                                        new TestMb.RecordPair(
                                                "record",
                                                ifaceMethods(
                                                        mb,
                                                        TList.of(
                                                                new TestMb.RecordPair(
                                                                        "drop",
                                                                        mb.constEvalCode(
                                                                                mb.call(
                                                                                        mb.evalConstEval(),
                                                                                        mb.recordOf(
                                                                                                TList.of(
                                                                                                        new TestMb
                                                                                                                .RecordPair(
                                                                                                                "xx",
                                                                                                                mb
                                                                                                                        .field(
                                                                                                                                mb
                                                                                                                                        .scopeUse(
                                                                                                                                                "arg"),
                                                                                                                                mb
                                                                                                                                        .str(
                                                                                                                                                "xx"))),
                                                                                                        new TestMb
                                                                                                                .RecordPair(
                                                                                                                "staged",
                                                                                                                mb
                                                                                                                        .stage(
                                                                                                                                mb
                                                                                                                                        .call(
                                                                                                                                                mb
                                                                                                                                                        .log(),
                                                                                                                                                mb
                                                                                                                                                        .str(
                                                                                                                                                                dropMessage))))))))),
                                                                new TestMb.RecordPair(
                                                                        "use",
                                                                        mb.call(
                                                                                mb.evalCodevalue(),
                                                                                mb.recordOf(
                                                                                        TList.of(
                                                                                                new TestMb.RecordPair(
                                                                                                        "code",
                                                                                                        mb.matchOf(
                                                                                                                mb.call(
                                                                                                                        mb
                                                                                                                                .evalConstEval(),
                                                                                                                        mb
                                                                                                                                .recordOf(
                                                                                                                                        TList
                                                                                                                                                .of(
                                                                                                                                                        new TestMb
                                                                                                                                                                .RecordPair(
                                                                                                                                                                "xx",
                                                                                                                                                                mb
                                                                                                                                                                        .field(
                                                                                                                                                                                mb
                                                                                                                                                                                        .scopeUse(
                                                                                                                                                                                                "arg"),
                                                                                                                                                                                mb
                                                                                                                                                                                        .str(
                                                                                                                                                                                                "xx"))),
                                                                                                                                                        new TestMb
                                                                                                                                                                .RecordPair(
                                                                                                                                                                "staged",
                                                                                                                                                                mb
                                                                                                                                                                        .stage(
                                                                                                                                                                                mb
                                                                                                                                                                                        .as(
                                                                                                                                                                                                mb
                                                                                                                                                                                                        .str(
                                                                                                                                                                                                                ""),
                                                                                                                                                                                                mb
                                                                                                                                                                                                        .typesString())))))),
                                                                                                                mb.str(
                                                                                                                        "__use_p"),
                                                                                                                TList
                                                                                                                        .of(
                                                                                                                                new TestMb
                                                                                                                                        .Branch(
                                                                                                                                        mb
                                                                                                                                                .str(
                                                                                                                                                        "ok"),
                                                                                                                                        mb
                                                                                                                                                .field(
                                                                                                                                                        mb
                                                                                                                                                                .scopeUse(
                                                                                                                                                                        "__use_p"),
                                                                                                                                                        mb
                                                                                                                                                                .str(
                                                                                                                                                                        "code"))),
                                                                                                                                new TestMb
                                                                                                                                        .Branch(
                                                                                                                                        mb
                                                                                                                                                .str(
                                                                                                                                                        "errs"),
                                                                                                                                        mb
                                                                                                                                                .panic(
                                                                                                                                                        mb
                                                                                                                                                                .str(
                                                                                                                                                                        "canary use placeholder failed")))),
                                                                                                                Opt
                                                                                                                        .none())),
                                                                                                new TestMb.RecordPair(
                                                                                                        "value",
                                                                                                        mb.call(
                                                                                                                mb
                                                                                                                        .evalStack(),
                                                                                                                mb
                                                                                                                        .recordOf(
                                                                                                                                TList
                                                                                                                                        .of(
                                                                                                                                                new TestMb
                                                                                                                                                        .RecordPair(
                                                                                                                                                        "xx",
                                                                                                                                                        mb
                                                                                                                                                                .field(
                                                                                                                                                                        mb
                                                                                                                                                                                .scopeUse(
                                                                                                                                                                                        "arg"),
                                                                                                                                                                        mb
                                                                                                                                                                                .str(
                                                                                                                                                                                        "xx"))),
                                                                                                                                                new TestMb
                                                                                                                                                        .RecordPair(
                                                                                                                                                        "type",
                                                                                                                                                        mb
                                                                                                                                                                .scopeUse(
                                                                                                                                                                        "outer")))))))))),
                                                                new TestMb.RecordPair("fork", mb.scopeUse("outer")),
                                                                new TestMb.RecordPair(
                                                                        "merge",
                                                                        mb.call(
                                                                                mb.field(
                                                                                        mb.call(
                                                                                                mb.typesEresult(),
                                                                                                mb.evalBinding()),
                                                                                        mb.str("ok")),
                                                                                mb.scopeUse("outer"))))))))),
                        mb.call(
                                mb.field(mb.scopeUse("u"), mb.str("impl_interface")),
                                mb.recordOf(
                                        TList.of(
                                                new TestMb.RecordPair("iface_ref", mb.evalAspectBind()),
                                                new TestMb.RecordPair(
                                                        "record",
                                                        ifaceMethods(
                                                                mb,
                                                                TList.of(
                                                                        new TestMb.RecordPair(
                                                                                "bind",
                                                                                mb.call(
                                                                                        mb.field(
                                                                                                mb.evalAspectBind(),
                                                                                                mb.str("bind")),
                                                                                        mb.tupleOf(
                                                                                                mb.field(
                                                                                                        mb.call(
                                                                                                                mb
                                                                                                                        .evalAspectLookup(),
                                                                                                                mb
                                                                                                                        .tupleOf(
                                                                                                                                mb
                                                                                                                                        .typesString(),
                                                                                                                                mb
                                                                                                                                        .evalAspectBind(),
                                                                                                                                mb
                                                                                                                                        .field(
                                                                                                                                                mb
                                                                                                                                                        .scopeUse(
                                                                                                                                                                "arg"),
                                                                                                                                                mb
                                                                                                                                                        .str(
                                                                                                                                                                "xx")))),
                                                                                                        mb.str(
                                                                                                                "value")),
                                                                                                mb.recordOf(
                                                                                                        TList.of(
                                                                                                                new TestMb
                                                                                                                        .RecordPair(
                                                                                                                        "xx",
                                                                                                                        mb
                                                                                                                                .field(
                                                                                                                                        mb
                                                                                                                                                .scopeUse(
                                                                                                                                                        "arg"),
                                                                                                                                        mb
                                                                                                                                                .str(
                                                                                                                                                        "xx"))),
                                                                                                                new TestMb
                                                                                                                        .RecordPair(
                                                                                                                        "loc",
                                                                                                                        mb
                                                                                                                                .field(
                                                                                                                                        mb
                                                                                                                                                .scopeUse(
                                                                                                                                                        "arg"),
                                                                                                                                        mb
                                                                                                                                                .str(
                                                                                                                                                        "loc"))),
                                                                                                                new TestMb
                                                                                                                        .RecordPair(
                                                                                                                        "cv",
                                                                                                                        mb
                                                                                                                                .field(
                                                                                                                                        mb
                                                                                                                                                .scopeUse(
                                                                                                                                                        "arg"),
                                                                                                                                        mb
                                                                                                                                                .str(
                                                                                                                                                        "cv")))))))))))))),
                        mb.call(
                                mb.field(mb.scopeUse("u"), mb.str("impl_interface")),
                                mb.recordOf(
                                        TList.of(
                                                new TestMb.RecordPair("iface_ref", mb.evalAspectBindDynamic()),
                                                new TestMb.RecordPair(
                                                        "record",
                                                        ifaceMethods(
                                                                mb,
                                                                TList.of(
                                                                        new TestMb.RecordPair(
                                                                                "bind_dynamic",
                                                                                mb.call(
                                                                                        mb.field(
                                                                                                mb
                                                                                                        .evalAspectBindDynamic(),
                                                                                                mb.str("bind_dynamic")),
                                                                                        mb.tupleOf(
                                                                                                mb.field(
                                                                                                        mb.call(
                                                                                                                mb
                                                                                                                        .evalAspectLookup(),
                                                                                                                mb
                                                                                                                        .tupleOf(
                                                                                                                                mb
                                                                                                                                        .typesString(),
                                                                                                                                mb
                                                                                                                                        .evalAspectBindDynamic(),
                                                                                                                                mb
                                                                                                                                        .field(
                                                                                                                                                mb
                                                                                                                                                        .scopeUse(
                                                                                                                                                                "arg"),
                                                                                                                                                mb
                                                                                                                                                        .str(
                                                                                                                                                                "xx")))),
                                                                                                        mb.str(
                                                                                                                "value")),
                                                                                                mb.recordOf(
                                                                                                        TList.of(
                                                                                                                new TestMb
                                                                                                                        .RecordPair(
                                                                                                                        "xx",
                                                                                                                        mb
                                                                                                                                .field(
                                                                                                                                        mb
                                                                                                                                                .scopeUse(
                                                                                                                                                        "arg"),
                                                                                                                                        mb
                                                                                                                                                .str(
                                                                                                                                                        "xx"))),
                                                                                                                new TestMb
                                                                                                                        .RecordPair(
                                                                                                                        "loc",
                                                                                                                        mb
                                                                                                                                .field(
                                                                                                                                        mb
                                                                                                                                                .scopeUse(
                                                                                                                                                        "arg"),
                                                                                                                                        mb
                                                                                                                                                .str(
                                                                                                                                                        "loc"))),
                                                                                                                new TestMb
                                                                                                                        .RecordPair(
                                                                                                                        "cv",
                                                                                                                        mb
                                                                                                                                .field(
                                                                                                                                        mb
                                                                                                                                                .scopeUse(
                                                                                                                                                        "arg"),
                                                                                                                                        mb
                                                                                                                                                .str(
                                                                                                                                                        "cv")))))))))))))),
                        mb.call(
                                mb.field(mb.scopeUse("u"), mb.str("impl_interface")),
                                mb.recordOf(TList.of(
                                        new TestMb.RecordPair("iface_ref", mb.evalAspectConsumeDynamic()),
                                        new TestMb.RecordPair(
                                                "record",
                                                ifaceMethods(
                                                        mb,
                                                        TList.of(
                                                                new TestMb.RecordPair(
                                                                        "to_dynamic",
                                                                        mb.call(
                                                                                mb.field(
                                                                                        mb.evalAspectConsumeDynamic(),
                                                                                        mb.str("to_dynamic")),
                                                                                mb.tupleOf(
                                                                                        mb.field(
                                                                                                mb.call(
                                                                                                        mb
                                                                                                                .evalAspectLookup(),
                                                                                                        mb.tupleOf(
                                                                                                                mb
                                                                                                                        .typesNull(),
                                                                                                                mb
                                                                                                                        .evalAspectConsumeDynamic(),
                                                                                                                mb
                                                                                                                        .field(
                                                                                                                                mb
                                                                                                                                        .scopeUse(
                                                                                                                                                "arg"),
                                                                                                                                mb
                                                                                                                                        .str(
                                                                                                                                                "xx")))),
                                                                                                mb.str("value")),
                                                                                        mb.recordOf(
                                                                                                TList.of(
                                                                                                        new TestMb
                                                                                                                .RecordPair(
                                                                                                                "xx",
                                                                                                                mb
                                                                                                                        .field(
                                                                                                                                mb
                                                                                                                                        .scopeUse(
                                                                                                                                                "arg"),
                                                                                                                                mb
                                                                                                                                        .str(
                                                                                                                                                "xx"))),
                                                                                                        new TestMb
                                                                                                                .RecordPair(
                                                                                                                "loc",
                                                                                                                mb
                                                                                                                        .field(
                                                                                                                                mb
                                                                                                                                        .scopeUse(
                                                                                                                                                "arg"),
                                                                                                                                mb
                                                                                                                                        .str(
                                                                                                                                                "loc"))),
                                                                                                        new TestMb
                                                                                                                .RecordPair(
                                                                                                                "cv",
                                                                                                                mb
                                                                                                                        .field(
                                                                                                                                mb
                                                                                                                                        .scopeUse(
                                                                                                                                                "arg"),
                                                                                                                                mb
                                                                                                                                        .str(
                                                                                                                                                "cv")))))))),
                                                                new TestMb.RecordPair(
                                                                        "jvm_class",
                                                                        mb.call(
                                                                                mb.field(
                                                                                        mb.evalAspectConsumeDynamic(),
                                                                                        mb.str("jvm_class")),
                                                                                mb.tupleOf(
                                                                                        mb.field(
                                                                                                mb.call(
                                                                                                        mb
                                                                                                                .evalAspectLookup(),
                                                                                                        mb.tupleOf(
                                                                                                                mb
                                                                                                                        .typesNull(),
                                                                                                                mb
                                                                                                                        .evalAspectConsumeDynamic(),
                                                                                                                mb
                                                                                                                        .scopeUse(
                                                                                                                                "arg"))),
                                                                                                mb.str("value")),
                                                                                        mb.scopeUse("arg")))))))))),
                        mb.call(mb.field(mb.scopeUse("d"), mb.str("finish")), mb.tupleOf()),
                        mb.return_(mb.field(mb.scopeUse("u"), mb.str("new"))))),
                mb.literalNull());
    }

    static Compile.CompileResult compileOk(TMap<String, SourceModule> modules) {
        var gx = Helpers.newGx(modules);
        var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
        Result.Ok<Compile.CompileResult, TList<ExprErr>> okRes;
        switch (res) {
            case Result.Ok<Compile.CompileResult, TList<ExprErr>> r -> okRes = r;
            case Result.Err<Compile.CompileResult, TList<ExprErr>> err -> {
                assertTrue(false, () -> "compile failed: " + res);
                return null;
            }
        }
        return okRes.value();
    }

    static TList<String> messages(Compile.CompileResult res) {
        var out = new TList<String>(res.logs().size());
        for (var l : res.logs()) out.add(l.message());
        return out;
    }

    @Test
    public void test01300ThrowInsideSeqKeepsOuterValue() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(mb.seqOf(
                        mb.tryCatchFinally(
                                mb.throw_(mb.str("boom")),
                                Opt.some(new TestMb.Catch(
                                        "e", mb.builtin("types", "string"), mb.call(mb.log(), mb.scopeUse("e")))),
                                Opt.none()),
                        mb.call(mb.log(), mb.str("after")))));
        var res = compileOk(modules);
        assertEquals(TList.of("boom", "after"), messages(res));
    }

    @Test
    public void test01310TryFinallyRunsFinally() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(mb.tryCatchFinally(
                        mb.seqOf(mb.call(mb.log(), mb.str("body")), mb.tupleOf()),
                        Opt.none(),
                        Opt.some(mb.seqOf(mb.call(mb.log(), mb.str("finally")), mb.tupleOf())))));
        var res = compileOk(modules);
        assertEquals(TList.of("body", "finally"), messages(res));
    }

    @Test
    public void test01320TryCatchHandlesThrow() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(mb.seqOf(
                        mb.tryCatchFinally(
                                mb.throw_(mb.str("boom")),
                                Opt.some(new TestMb.Catch(
                                        "e", mb.builtin("types", "string"), mb.call(mb.log(), mb.scopeUse("e")))),
                                Opt.none()),
                        mb.call(mb.log(), mb.str("done")))));
        var res = compileOk(modules);
        assertEquals(TList.of("boom", "done"), messages(res));
    }

    @Test
    public void test01330TryRequiresCatchOrFinally() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(mb.tryCatchFinally(mb.call(mb.log(), mb.str("body")), Opt.none(), Opt.none())));
        var gx = Helpers.newGx(modules);
        var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
        assertTrue(
                switch (res) {
                    case Result.Err<?, ?> ignored -> true;
                    case Result.Ok<?, ?> ignored -> false;
                },
                () -> "Try with no catch and no finally should fail compile");
    }

    @Test
    public void test01335FinallyThrowRejected() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(mb.tryCatchFinally(
                        mb.call(mb.log(), mb.str("body")), Opt.none(), Opt.some(mb.throw_(mb.str("boom"))))));
        var gx = Helpers.newGx(modules);
        var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
        assertTrue(
                switch (res) {
                    case Result.Err<?, ?> ignored -> true;
                    case Result.Ok<?, ?> ignored -> false;
                },
                () -> "throw in a finally should fail compile: " + res);
    }

    @Test
    public void test01337FinallyReturnRejected() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(mb.labeled(
                        "outer",
                        mb.tryCatchFinally(
                                mb.call(mb.log(), mb.str("body")), Opt.none(), Opt.some(mb.returnLabel("outer"))))));
        var gx = Helpers.newGx(modules);
        var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
        assertTrue(
                switch (res) {
                    case Result.Err<?, ?> ignored -> true;
                    case Result.Ok<?, ?> ignored -> false;
                },
                () -> "return in a finally should fail compile: " + res);
    }

    @Test
    public void test01340ThrowCaughtLogsMessage() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(mb.tryCatchFinally(
                        mb.throw_(mb.str("boom")),
                        Opt.some(new TestMb.Catch(
                                "e", mb.builtin("types", "string"), mb.call(mb.log(), mb.scopeUse("e")))),
                        Opt.none())));
        var res = compileOk(modules);
        assertEquals(TList.of("boom"), messages(res));
    }

    @Test
    public void test01350ThrowWithNoCatchInFunctionNoExceptionTypeRejects() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(mb.seqOf(
                        mb.bind(
                                "my_func",
                                mb.lower(mb.seqOf(
                                        mb.bind("d", mb.call(mb.define(), mb.tupleOf())),
                                        mb.bind(
                                                "nfr",
                                                mb.call(mb.field(mb.scopeUse("d"), mb.str("func")), mb.str("f"))),
                                        mb.bind("f", mb.field(mb.scopeUse("nfr"), mb.str("func"))),
                                        mb.call(
                                                mb.field(mb.scopeUse("nfr"), mb.str("impl")),
                                                mb.recordOf(
                                                        TList.of(
                                                                new TestMb.RecordPair("arg", mb.tupleOf()),
                                                                new TestMb.RecordPair(
                                                                        "ret", mb.optSomeType(mb.typesNull())),
                                                                new TestMb.RecordPair(
                                                                        "impl_body",
                                                                        mb.stage(mb.throw_(mb.str("x"))))))),
                                        mb.call(mb.field(mb.scopeUse("d"), mb.str("finish")), mb.tupleOf()),
                                        mb.return_(mb.scopeUse("f"))))),
                        mb.call(mb.scopeUse("my_func"), mb.tupleOf()))));
        var gx = Helpers.newGx(modules);
        var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
        assertTrue(
                switch (res) {
                    case Result.Err<?, ?> ignored -> true;
                    case Result.Ok<?, ?> ignored -> false;
                },
                () -> "throw in function with no declared exception type should be rejected: " + res);
    }

    @Test
    public void test01360SetExceptionTypeAllowsThrow() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(mb.seqOf(
                        mb.bind(
                                "my_func",
                                mb.lower(mb.seqOf(
                                        mb.bind("d", mb.call(mb.define(), mb.tupleOf())),
                                        mb.bind(
                                                "nfr",
                                                mb.call(mb.field(mb.scopeUse("d"), mb.str("func")), mb.str("f"))),
                                        mb.bind("f", mb.field(mb.scopeUse("nfr"), mb.str("func"))),
                                        mb.call(
                                                mb.field(mb.scopeUse("nfr"), mb.str("set_exception_type")),
                                                mb.typesString()),
                                        mb.call(
                                                mb.field(mb.scopeUse("nfr"), mb.str("impl")),
                                                mb.recordOf(
                                                        TList.of(
                                                                new TestMb.RecordPair("arg", mb.tupleOf()),
                                                                new TestMb.RecordPair(
                                                                        "ret", mb.optSomeType(mb.typesNull())),
                                                                new TestMb.RecordPair(
                                                                        "impl_body",
                                                                        mb.stage(mb.throw_(mb.str("x"))))))),
                                        mb.call(mb.field(mb.scopeUse("d"), mb.str("finish")), mb.tupleOf()),
                                        mb.return_(mb.scopeUse("f"))))),
                        mb.tryCatchFinally(
                                mb.seqOf(mb.call(mb.scopeUse("my_func"), mb.tupleOf()), mb.tupleOf()),
                                Opt.some(new TestMb.Catch(
                                        "e",
                                        mb.builtin("types", "string"),
                                        mb.seqOf(mb.call(mb.log(), mb.scopeUse("e")), mb.tupleOf()))),
                                Opt.none()))));
        var res = compileOk(modules);
        assertEquals(TList.of("x"), messages(res));
    }

    @Test
    public void test01370TryCatchFinallyCombined() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(mb.tryCatchFinally(
                        mb.throw_(mb.str("boom")),
                        Opt.some(new TestMb.Catch(
                                "e",
                                mb.builtin("types", "string"),
                                mb.seqOf(mb.call(mb.log(), mb.str("caught")), mb.tupleOf()))),
                        Opt.some(mb.seqOf(mb.call(mb.log(), mb.str("always")), mb.tupleOf())))));
        var res = compileOk(modules);
        assertEquals(TList.of("caught", "always"), messages(res));
    }

    @Test
    public void test01380ThrowInCatchStillRunsFinally() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        var inner = mb.tryCatchFinally(
                mb.throw_(mb.str("boom")),
                Opt.some(new TestMb.Catch(
                        "e",
                        mb.builtin("types", "string"),
                        mb.seqOf(
                                mb.seqOf(mb.call(mb.log(), mb.str("caught")), mb.tupleOf()),
                                mb.throw_(mb.str("rethrown"))))),
                Opt.some(mb.seqOf(mb.call(mb.log(), mb.str("always")), mb.tupleOf())));
        modules.put(
                "main",
                new SourceModule.V1(mb.tryExpr(
                        inner,
                        Opt.some(new SourceExpr.TryCatch(
                                mb.str("e2"),
                                mb.builtin("types", "string"),
                                mb.seqOf(mb.call(mb.log(), mb.str("outer")), mb.tupleOf()))),
                        Opt.none())));
        var res = compileOk(modules);
        assertEquals(TList.of("caught", "always", "outer"), messages(res));
    }

    @Test
    public void test01400EndOfScopeDropRuntime() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(mb.seqOf(
                        mb.bind("canary", mb.lower(makeCanaryInstance(mb, "Canary", "dropped"))),
                        mb.bind("cnry", mb.as(mb.as(mb.str("X"), mb.typesString()), mb.scopeUse("canary"))),
                        mb.call(mb.log(), mb.str("after")))));
        var res = compileOk(modules);
        var messages = messages(res);
        assertEquals(
                TList.of("after", "dropped"),
                messages,
                () -> "end-of-scope drop should run after body; got logs=" + messages);
    }

    @Test
    public void test01410DiscardedSeqEntryRuntime() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(mb.seqOf(
                        mb.bind("canary", mb.lower(makeCanaryInstance(mb, "Canary", "dropped"))),
                        mb.as(mb.as(mb.str("Y"), mb.typesString()), mb.scopeUse("canary")),
                        mb.call(mb.log(), mb.str("after")))));
        var res = compileOk(modules);
        var messages = messages(res);
        assertEquals(
                TList.of("dropped", "after"),
                messages,
                () -> "discarded seq entry should be dropped before next entry; got logs=" + messages);
    }

    @Test
    public void test01420FieldSiblingDropRuntime() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(mb.seqOf(
                        mb.bind("canary_a", mb.lower(makeCanaryInstance(mb, "CanaryA", "dropped_a"))),
                        mb.bind("canary_b", mb.lower(makeCanaryInstance(mb, "CanaryB", "dropped_b"))),
                        mb.bind(
                                "rec",
                                mb.recordOf(TList.of(
                                        new TestMb.RecordPair(
                                                "a",
                                                mb.as(mb.as(mb.str("A"), mb.typesString()), mb.scopeUse("canary_a"))),
                                        new TestMb.RecordPair(
                                                "b",
                                                mb.as(
                                                        mb.as(mb.str("B"), mb.typesString()),
                                                        mb.scopeUse("canary_b")))))),
                        mb.field(mb.scopeUse("rec"), mb.str("a")),
                        mb.call(mb.log(), mb.str("end")))));
        var res = compileOk(modules);
        var messages = messages(res);
        assertTrue(
                messages.contains("dropped_b"),
                () -> "sibling 'b' should be dropped when field 'a' is accessed; got logs=" + messages);
        assertTrue(
                messages.contains("dropped_a"),
                () -> "field access copies the value, so extracted 'a' and rec's original slot are"
                        + " both dropped; got logs=" + messages);
    }

    @Test
    public void test01430DropOnThrowRuntime() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(mb.tryCatchFinally(
                        mb.seqOf(
                                mb.bind("canary", mb.lower(makeCanaryInstance(mb, "Canary", "dropped"))),
                                mb.bind("cnry", mb.as(mb.as(mb.str("Z"), mb.typesString()), mb.scopeUse("canary"))),
                                mb.throw_(mb.str("boom"))),
                        Opt.some(new TestMb.Catch(
                                "e", mb.builtin("types", "string"), mb.call(mb.log(), mb.str("caught")))),
                        Opt.none())));
        var res = compileOk(modules);
        var messages = messages(res);
        int dropIdx = messages.indexOf("dropped");
        int caughtIdx = messages.indexOf("caught");
        assertTrue(dropIdx >= 0, () -> "expected 'dropped' log, got " + messages);
        assertTrue(caughtIdx >= 0, () -> "expected 'caught' log, got " + messages);
        assertTrue(dropIdx < caughtIdx, () -> "drop should run before catch handler; got logs=" + messages);
    }

    static SourceExpr dynTrue(TestMb mb) {
        return mb.operatorEq(mb.as(mb.num(1.0), mb.typesLong()), mb.as(mb.num(1.0), mb.typesLong()));
    }

    @Test
    public void test01432IfArmDropRuntime() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(mb.seqOf(
                        mb.bind("canary", mb.lower(makeCanaryInstance(mb, "Canary", "arm_dropped"))),
                        mb.ifOf(
                                TList.of(new TestMb.Branch(
                                        dynTrue(mb),
                                        mb.seqOf(
                                                mb.bind(
                                                        "cnry",
                                                        mb.as(
                                                                mb.as(mb.str("Z"), mb.typesString()),
                                                                mb.scopeUse("canary"))),
                                                mb.call(mb.log(), mb.str("then")),
                                                mb.tupleOf()))),
                                Opt.none()),
                        mb.call(mb.log(), mb.str("end")))));
        var res = compileOk(modules);
        var messages = messages(res);
        int dropIdx = messages.indexOf("arm_dropped");
        int endIdx = messages.indexOf("end");
        assertTrue(dropIdx >= 0, () -> "expected arm-local drop 'arm_dropped', got " + messages);
        assertTrue(dropIdx < endIdx, () -> "arm-local value should drop at arm end, before 'end'; got " + messages);
    }

    @Test
    public void test01434IfArmDropOnThrowRuntime() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(mb.tryCatchFinally(
                        mb.seqOf(
                                mb.bind("canary", mb.lower(makeCanaryInstance(mb, "Canary", "dropped"))),
                                mb.bind("cnry", mb.as(mb.as(mb.str("Z"), mb.typesString()), mb.scopeUse("canary"))),
                                mb.ifOf(
                                        TList.of(new TestMb.Branch(dynTrue(mb), mb.throw_(mb.str("boom")))),
                                        Opt.some(mb.tupleOf()))),
                        Opt.some(new TestMb.Catch(
                                "e",
                                mb.builtin("types", "string"),
                                mb.seqOf(mb.call(mb.log(), mb.str("caught")), mb.tupleOf()))),
                        Opt.none())));
        var res = compileOk(modules);
        var messages = messages(res);
        int dropIdx = messages.indexOf("dropped");
        int caughtIdx = messages.indexOf("caught");
        assertTrue(dropIdx >= 0, () -> "expected 'dropped' log, got " + messages);
        assertTrue(caughtIdx >= 0, () -> "expected 'caught' log, got " + messages);
        assertTrue(
                dropIdx < caughtIdx,
                () -> "live value should drop before the catch when an if-arm throws; got " + messages);
    }

    @Test
    public void test01440TryCatchUnifiesBranchTypes() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(mb.seqOf(
                        mb.tryCatchFinally(
                                mb.seqOf(mb.call(mb.log(), mb.str("body")), mb.tupleOf()),
                                Opt.some(new TestMb.Catch(
                                        "e",
                                        mb.builtin("types", "string"),
                                        mb.seqOf(mb.call(mb.log(), mb.str("caught")), mb.tupleOf()))),
                                Opt.none()),
                        mb.seqOf(mb.call(mb.log(), mb.str("after")), mb.tupleOf()))));
        var res = compileOk(modules);
        assertEquals(TList.of("body", "after"), messages(res));
    }

    @Test
    public void test01450TryCatchBranchTypeMismatchErrors() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(mb.tryCatchFinally(
                        mb.seqOf(mb.call(mb.log(), mb.str("body")), mb.tupleOf()),
                        Opt.some(new TestMb.Catch("e", mb.builtin("types", "string"), mb.scopeUse("e"))),
                        Opt.none())));
        var gx = Helpers.newGx(modules);
        var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
        assertTrue(
                switch (res) {
                    case Result.Err<?, ?> ignored -> true;
                    case Result.Ok<?, ?> ignored -> false;
                },
                () -> "Try with incompatible body/catch result types should fail compile: " + res);
    }

    @Test
    public void test01460FinallyDropsResult() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(mb.seqOf(
                        mb.bind("canary", mb.lower(makeCanaryInstance(mb, "Canary", "dropped"))),
                        mb.tryCatchFinally(
                                mb.call(mb.log(), mb.str("body")),
                                Opt.none(),
                                Opt.some(mb.as(mb.as(mb.str("V"), mb.typesString()), mb.scopeUse("canary")))),
                        mb.call(mb.log(), mb.str("after")))));
        var res = compileOk(modules);
        var messages = messages(res);
        int dropIdx = messages.indexOf("dropped");
        int afterIdx = messages.indexOf("after");
        assertTrue(dropIdx >= 0, () -> "expected 'dropped' log, got " + messages);
        assertTrue(afterIdx >= 0, () -> "expected 'after' log, got " + messages);
        assertTrue(dropIdx < afterIdx, () -> "finally result drop should run before 'after'; got logs=" + messages);
    }

    @Test
    public void test01470BindOfThrowErrors() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(mb.seqOf(
                        mb.bind(
                                "my_func",
                                mb.lower(mb.seqOf(
                                        mb.bind("d", mb.call(mb.define(), mb.tupleOf())),
                                        mb.bind(
                                                "nfr",
                                                mb.call(mb.field(mb.scopeUse("d"), mb.str("func")), mb.str("f"))),
                                        mb.bind("f", mb.field(mb.scopeUse("nfr"), mb.str("func"))),
                                        mb.call(
                                                mb.field(mb.scopeUse("nfr"), mb.str("set_exception_type")),
                                                mb.typesString()),
                                        mb.call(
                                                mb.field(mb.scopeUse("nfr"), mb.str("impl")),
                                                mb.recordOf(
                                                        TList.of(
                                                                new TestMb.RecordPair("arg", mb.tupleOf()),
                                                                new TestMb.RecordPair(
                                                                        "ret", mb.optSomeType(mb.typesNull())),
                                                                new TestMb.RecordPair(
                                                                        "impl_body",
                                                                        mb.stage(
                                                                                mb.seqOf(
                                                                                        mb.bind(
                                                                                                "x",
                                                                                                mb.throw_(
                                                                                                        mb.str(
                                                                                                                "boom"))),
                                                                                        mb.return_(
                                                                                                mb.literalNull()))))))),
                                        mb.call(mb.field(mb.scopeUse("d"), mb.str("finish")), mb.tupleOf()),
                                        mb.return_(mb.scopeUse("f"))))),
                        mb.tryCatchFinally(
                                mb.call(mb.scopeUse("my_func"), mb.tupleOf()),
                                Opt.some(new TestMb.Catch(
                                        "e", mb.builtin("types", "string"), mb.call(mb.log(), mb.scopeUse("e")))),
                                Opt.none()))));
        var gx = Helpers.newGx(modules);
        var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
        assertTrue(
                switch (res) {
                    case Result.Err<?, ?> ignored -> true;
                    case Result.Ok<?, ?> ignored -> false;
                },
                () -> "expected compile error binding result of throw");
        var msg = res.toString();
        assertTrue(
                msg.contains("Cannot bind result of expression that ends control flow"),
                () -> "expected bind-of-throw error message, got: " + msg);
    }

    @Test
    public void test01480LogAfterThrowInSeqErrors() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(mb.seqOf(
                        mb.bind(
                                "my_func",
                                mb.lower(mb.seqOf(
                                        mb.bind("d", mb.call(mb.define(), mb.tupleOf())),
                                        mb.bind(
                                                "nfr",
                                                mb.call(mb.field(mb.scopeUse("d"), mb.str("func")), mb.str("f"))),
                                        mb.bind("f", mb.field(mb.scopeUse("nfr"), mb.str("func"))),
                                        mb.call(
                                                mb.field(mb.scopeUse("nfr"), mb.str("set_exception_type")),
                                                mb.typesString()),
                                        mb.call(
                                                mb.field(mb.scopeUse("nfr"), mb.str("impl")),
                                                mb.recordOf(
                                                        TList.of(
                                                                new TestMb.RecordPair("arg", mb.tupleOf()),
                                                                new TestMb.RecordPair(
                                                                        "ret", mb.optSomeType(mb.typesNull())),
                                                                new TestMb.RecordPair(
                                                                        "impl_body",
                                                                        mb.stage(
                                                                                mb.seqOf(
                                                                                        mb.throw_(mb.str("boom")),
                                                                                        mb.call(
                                                                                                mb.log(),
                                                                                                mb.str(
                                                                                                        "unreachable")))))))),
                                        mb.call(mb.field(mb.scopeUse("d"), mb.str("finish")), mb.tupleOf()),
                                        mb.return_(mb.scopeUse("f"))))),
                        mb.tryCatchFinally(
                                mb.call(mb.scopeUse("my_func"), mb.tupleOf()),
                                Opt.some(new TestMb.Catch(
                                        "e", mb.builtin("types", "string"), mb.call(mb.log(), mb.scopeUse("e")))),
                                Opt.none()))));
        var gx = Helpers.newGx(modules);
        var res = Compile.compile(gx, new SourceSpecAbsolute.Virtual("main"));
        assertTrue(
                switch (res) {
                    case Result.Err<?, ?> ignored -> true;
                    case Result.Ok<?, ?> ignored -> false;
                },
                () -> "expected compile error for code after throw");
        var msg = res.toString();
        assertTrue(msg.contains("Unreachable"), () -> "expected Unreachable error message, got: " + msg);
    }

    static SourceExpr canaryInstance(TestMb mb, String canaryScope, String textLiteral) {
        return mb.as(mb.as(mb.str(textLiteral), mb.typesString()), mb.scopeUse(canaryScope));
    }

    @Test
    public void test01490TupleSourcelessDropReverseOrder() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(mb.seqOf(
                        mb.bind("cA", mb.lower(makeCanaryInstance(mb, "CanaryA", "drop_A"))),
                        mb.bind("cC", mb.lower(makeCanaryInstance(mb, "CanaryC", "drop_C"))),
                        mb.tupleOf(canaryInstance(mb, "cA", "a"), mb.str("const_b"), canaryInstance(mb, "cC", "c")),
                        mb.call(mb.log(), mb.str("after")))));
        var res = compileOk(modules);
        assertEquals(
                TList.of("drop_C", "drop_A", "after"),
                messages(res),
                () -> "sourceless tuple drop should reverse construction order; got " + messages(res));
    }

    @Test
    public void test01500TupleSourcedDropReverseOrder() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(mb.seqOf(
                        mb.bind("cA", mb.lower(makeCanaryInstance(mb, "CanaryA", "drop_A"))),
                        mb.bind("cC", mb.lower(makeCanaryInstance(mb, "CanaryC", "drop_C"))),
                        mb.bind(
                                "t",
                                mb.tupleOf(
                                        canaryInstance(mb, "cA", "a"),
                                        mb.str("const_b"),
                                        canaryInstance(mb, "cC", "c"))),
                        mb.call(mb.log(), mb.str("after")))));
        var res = compileOk(modules);
        assertEquals(
                TList.of("after", "drop_C", "drop_A"),
                messages(res),
                () -> "sourced tuple drop should reverse construction order at end of scope; got " + messages(res));
    }

    @Test
    public void test01510RecordSourcelessDropReverseOrder() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(mb.seqOf(
                        mb.bind("cA", mb.lower(makeCanaryInstance(mb, "CanaryA", "drop_A"))),
                        mb.bind("cC", mb.lower(makeCanaryInstance(mb, "CanaryC", "drop_C"))),
                        mb.recordOf(TList.of(
                                new TestMb.RecordPair("a", canaryInstance(mb, "cA", "a")),
                                new TestMb.RecordPair("b", mb.str("const_b")),
                                new TestMb.RecordPair("c", canaryInstance(mb, "cC", "c")))),
                        mb.call(mb.log(), mb.str("after")))));
        var res = compileOk(modules);
        assertEquals(
                TList.of("drop_C", "drop_A", "after"),
                messages(res),
                () -> "sourceless record drop should reverse construction order (canonical: a,b,c); got "
                        + messages(res));
    }

    @Test
    public void test01520RecordSourcedDropReverseOrder() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(mb.seqOf(
                        mb.bind("cA", mb.lower(makeCanaryInstance(mb, "CanaryA", "drop_A"))),
                        mb.bind("cC", mb.lower(makeCanaryInstance(mb, "CanaryC", "drop_C"))),
                        mb.bind(
                                "r",
                                mb.recordOf(TList.of(
                                        new TestMb.RecordPair("a", canaryInstance(mb, "cA", "a")),
                                        new TestMb.RecordPair("b", mb.str("const_b")),
                                        new TestMb.RecordPair("c", canaryInstance(mb, "cC", "c"))))),
                        mb.call(mb.log(), mb.str("after")))));
        var res = compileOk(modules);
        assertEquals(
                TList.of("after", "drop_C", "drop_A"),
                messages(res),
                () -> "sourced record drop should reverse construction order at end of scope; got " + messages(res));
    }

    @Test
    public void test01522RecordCastReorderDropSourceless() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        var sourceRecord = mb.recordOf(TList.of(
                new TestMb.RecordPair("a", canaryInstance(mb, "cA", "a")),
                new TestMb.RecordPair("b", canaryInstance(mb, "cB", "b"))));
        var reorderedType = mb.recordOf(TList.of(
                new TestMb.RecordPair("b", canaryInstance(mb, "cB", "")),
                new TestMb.RecordPair("a", canaryInstance(mb, "cA", ""))));
        modules.put(
                "main",
                new SourceModule.V1(mb.seqOf(
                        mb.bind("cA", mb.lower(makeCanaryInstance(mb, "CanaryA", "drop_A"))),
                        mb.bind("cB", mb.lower(makeCanaryInstance(mb, "CanaryB", "drop_B"))),
                        mb.as(sourceRecord, reorderedType),
                        mb.call(mb.log(), mb.str("after")))));
        var res = compileOk(modules);
        assertEquals(
                TList.of("drop_A", "drop_B", "after"),
                messages(res),
                () -> "sourceless record cast should reorder to target (b, a); reverse-iterated drop"
                        + " -> drop_A then drop_B, before 'after'. Got: " + messages(res));
    }

    @Test
    public void test01524RecordCastReorderDropSourced() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        var sourceRecord = mb.recordOf(TList.of(
                new TestMb.RecordPair("a", canaryInstance(mb, "cA", "a")),
                new TestMb.RecordPair("b", canaryInstance(mb, "cB", "b"))));
        var reorderedType = mb.recordOf(TList.of(
                new TestMb.RecordPair("b", canaryInstance(mb, "cB", "")),
                new TestMb.RecordPair("a", canaryInstance(mb, "cA", ""))));
        modules.put(
                "main",
                new SourceModule.V1(mb.seqOf(
                        mb.bind("cA", mb.lower(makeCanaryInstance(mb, "CanaryA", "drop_A"))),
                        mb.bind("cB", mb.lower(makeCanaryInstance(mb, "CanaryB", "drop_B"))),
                        mb.bind("src", sourceRecord),
                        mb.bind("r", mb.as(mb.scopeUse("src"), reorderedType)),
                        mb.call(mb.log(), mb.str("after")))));
        var res = compileOk(modules);
        assertEquals(
                TList.of("after", "drop_A", "drop_B", "drop_B", "drop_A"),
                messages(res),
                () -> "record cast should reorder r's field layout to match target (b, a); reverse-drop"
                        + " -> drop_A, drop_B; then src drops in its own (a, b) reversed"
                        + " -> drop_B, drop_A. Got: " + messages(res));
    }

    @Test
    public void test01530RecordSourcelessFieldAccessFirstRuntime() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(mb.seqOf(
                        mb.bind("cA", mb.lower(makeCanaryInstance(mb, "CanaryA", "drop_A"))),
                        mb.bind("cB", mb.lower(makeCanaryInstance(mb, "CanaryB", "drop_B"))),
                        mb.bind("cC", mb.lower(makeCanaryInstance(mb, "CanaryC", "drop_C"))),
                        mb.field(
                                mb.recordOf(TList.of(
                                        new TestMb.RecordPair("a", canaryInstance(mb, "cA", "a")),
                                        new TestMb.RecordPair("b", canaryInstance(mb, "cB", "b")),
                                        new TestMb.RecordPair("c", canaryInstance(mb, "cC", "c")))),
                                mb.str("a")),
                        mb.call(mb.log(), mb.str("end")))));
        var res = compileOk(modules);
        var messages = messages(res);
        assertTrue(messages.contains("drop_B"), () -> "sourceless field-drop: sibling 'b' must drop; got " + messages);
        assertTrue(messages.contains("drop_C"), () -> "sourceless field-drop: sibling 'c' must drop; got " + messages);
        assertTrue(
                messages.contains("drop_A"),
                () -> "sourceless field-drop: extracted 'a' copy must drop; got " + messages);
        assertTrue(messages.contains("end"), () -> "expected 'end' log after all drops; got " + messages);
        int endIdx = messages.indexOf("end");
        int aIdx = messages.indexOf("drop_A");
        int bIdx = messages.indexOf("drop_B");
        int cIdx = messages.indexOf("drop_C");
        assertTrue(
                aIdx < endIdx && bIdx < endIdx && cIdx < endIdx,
                () -> "all record-field drops must precede 'end' (stack expr-drop, no bind); got " + messages);
    }

    @Test
    public void test01540RecordSourcelessFieldAccessLastRuntime() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(mb.seqOf(
                        mb.bind("cA", mb.lower(makeCanaryInstance(mb, "CanaryA", "drop_A"))),
                        mb.bind("cB", mb.lower(makeCanaryInstance(mb, "CanaryB", "drop_B"))),
                        mb.bind("cC", mb.lower(makeCanaryInstance(mb, "CanaryC", "drop_C"))),
                        mb.field(
                                mb.recordOf(TList.of(
                                        new TestMb.RecordPair("a", canaryInstance(mb, "cA", "a")),
                                        new TestMb.RecordPair("b", canaryInstance(mb, "cB", "b")),
                                        new TestMb.RecordPair("c", canaryInstance(mb, "cC", "c")))),
                                mb.str("c")),
                        mb.call(mb.log(), mb.str("end")))));
        var res = compileOk(modules);
        var messages = messages(res);
        assertTrue(
                messages.contains("drop_A"),
                () -> "sourceless field-drop (last): sibling 'a' must drop; got " + messages);
        assertTrue(
                messages.contains("drop_B"),
                () -> "sourceless field-drop (last): sibling 'b' must drop; got " + messages);
        assertTrue(
                messages.contains("drop_C"),
                () -> "sourceless field-drop (last): extracted 'c' copy must drop; got " + messages);
        assertTrue(messages.contains("end"), () -> "expected 'end' log after all drops; got " + messages);
    }

    @Test
    public void test01550TupleSourcelessIndexAccessFirstRuntime() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(mb.seqOf(
                        mb.bind("cA", mb.lower(makeCanaryInstance(mb, "CanaryA", "drop_A"))),
                        mb.bind("cB", mb.lower(makeCanaryInstance(mb, "CanaryB", "drop_B"))),
                        mb.bind("cC", mb.lower(makeCanaryInstance(mb, "CanaryC", "drop_C"))),
                        mb.field(
                                mb.tupleOf(
                                        canaryInstance(mb, "cA", "a"),
                                        canaryInstance(mb, "cB", "b"),
                                        canaryInstance(mb, "cC", "c")),
                                mb.num(0)),
                        mb.call(mb.log(), mb.str("end")))));
        var res = compileOk(modules);
        var messages = messages(res);
        assertTrue(
                messages.contains("drop_A"),
                () -> "sourceless tuple index-drop: extracted [0] copy must drop; got " + messages);
        assertTrue(
                messages.contains("drop_B"),
                () -> "sourceless tuple index-drop: sibling [1] must drop; got " + messages);
        assertTrue(
                messages.contains("drop_C"),
                () -> "sourceless tuple index-drop: sibling [2] must drop; got " + messages);
        assertTrue(messages.contains("end"), () -> "expected 'end' log after all drops; got " + messages);
    }

    @Test
    public void test01560TupleSourcelessIndexAccessLastRuntime() {
        var mb = new TestMb();
        var modules = new TMap<String, SourceModule>();
        modules.put(
                "main",
                new SourceModule.V1(mb.seqOf(
                        mb.bind("cA", mb.lower(makeCanaryInstance(mb, "CanaryA", "drop_A"))),
                        mb.bind("cB", mb.lower(makeCanaryInstance(mb, "CanaryB", "drop_B"))),
                        mb.bind("cC", mb.lower(makeCanaryInstance(mb, "CanaryC", "drop_C"))),
                        mb.field(
                                mb.tupleOf(
                                        canaryInstance(mb, "cA", "a"),
                                        canaryInstance(mb, "cB", "b"),
                                        canaryInstance(mb, "cC", "c")),
                                mb.num(2)),
                        mb.call(mb.log(), mb.str("end")))));
        var res = compileOk(modules);
        var messages = messages(res);
        assertTrue(
                messages.contains("drop_A"),
                () -> "sourceless tuple index-drop (last): sibling [0] must drop; got " + messages);
        assertTrue(
                messages.contains("drop_B"),
                () -> "sourceless tuple index-drop (last): sibling [1] must drop; got " + messages);
        assertTrue(
                messages.contains("drop_C"),
                () -> "sourceless tuple index-drop (last): extracted [2] copy must drop; got " + messages);
        assertTrue(messages.contains("end"), () -> "expected 'end' log after all drops; got " + messages);
    }
}
