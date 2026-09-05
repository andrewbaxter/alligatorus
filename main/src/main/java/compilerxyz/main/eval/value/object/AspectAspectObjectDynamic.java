package compilerxyz.main.eval.value.object;

import compilerxyz.main.eval.Unique;
import compilerxyz.main.eval.value.source_interface.InterfaceCell;
import compilerxyz.main.gen.AspectAspect;

public record AspectAspectObjectDynamic(TypeObjectDynamic type) implements AspectAspect {

    @Override
    public InterfaceCell aspectAspectGetIfaceCell(InterfaceCell value) {
        var aspSrcOpt4 = type.aspectSource();
        if (aspSrcOpt4.optIsNone()) {
            throw new UnsupportedOperationException(
                    type.typeDesc() + "::getIfaceData: TypeObjectDynamic has no aspect source");
        }
        return switch (aspSrcOpt4.optUnwrap()) {
            case ObjectAspectSource.Static ignored ->
                throw new UnsupportedOperationException(
                        type.typeDesc() + "::getIfaceCell: static aspect has no interface cell");
            case ObjectAspectSource.FromInterfaceData ignored -> value;
        };
    }

    @Override
    public Unique aspectAspectGetUnique(InterfaceCell value) {
        var aspSrcOpt3 = type.aspectSource();
        if (aspSrcOpt3.optIsNone()) {
            throw new IllegalStateException(
                    type.typeDesc() + "::getUnique: TypeObjectDynamic does not represent an aspect");
        }
        return switch (aspSrcOpt3.optUnwrap()) {
            case ObjectAspectSource.Static staticSrc -> staticSrc.id();
            case ObjectAspectSource.FromInterfaceData ignored -> value.identity;
        };
    }
}
