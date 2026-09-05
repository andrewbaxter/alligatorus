package compilerxyz.main.eval.value.object;

import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.value.source_interface.InterfaceCell;
import compilerxyz.main.gen.AspectAspect;

public record AspectAspectObjectConst(TypeObjectConst type) implements AspectAspect {

    @Override
    public InterfaceCell aspectAspectGetIfaceCell(InterfaceCell value) {
        var aspSrcOpt3 = type.aspectSource();
        if (aspSrcOpt3.optIsNone()) {
            throw new UnsupportedOperationException(type.typeDesc() + "::getIfaceData: no aspect source");
        }
        return switch (aspSrcOpt3.optUnwrap()) {
            case ObjectAspectSource.Static ignored ->
                throw new UnsupportedOperationException(
                        type.typeDesc() + "::getIfaceCell: static aspect has no interface cell");
            case ObjectAspectSource.FromInterfaceData ignored -> value;
        };
    }

    @Override
    public Unique aspectAspectGetUnique(InterfaceCell value) {
        var aspSrcOpt2 = type.aspectSource();
        if (aspSrcOpt2.optIsNone()) {
            throw new IllegalStateException(type.typeDesc() + "::getUnique: no aspect source");
        }
        return switch (aspSrcOpt2.optUnwrap()) {
            case ObjectAspectSource.Static staticSrc -> staticSrc.id();
            case ObjectAspectSource.FromInterfaceData ignored -> value.identity;
        };
    }
}
