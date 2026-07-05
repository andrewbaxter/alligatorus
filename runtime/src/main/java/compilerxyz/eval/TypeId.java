package compilerxyz.eval;

import compilerxyz.util.TList;
import java.util.Comparator;

public sealed interface TypeId {
  record Builtin(long id) implements TypeId {}

  record DefsetMember(DefSetId defSetId, long entryId) implements TypeId {}

  record Composite(TypeId base, TList<TypeId> children) implements TypeId {}

  Comparator<TypeId> CANONICAL_ORDER =
      (a, b) -> {
        int ra = kindRank(a);
        int rb = kindRank(b);
        if (ra != rb) return Integer.compare(ra, rb);
        return switch (a) {
          case Builtin ba -> Long.compare(ba.id(), ((Builtin) b).id());
          case DefsetMember da -> {
            var db = (DefsetMember) b;
            int c = compareDefSetId(da.defSetId(), db.defSetId());
            if (c != 0) yield c;
            yield Long.compare(da.entryId(), db.entryId());
          }
          case Composite ca -> {
            var cb = (Composite) b;
            int c = TypeId.CANONICAL_ORDER.compare(ca.base(), cb.base());
            if (c != 0) yield c;
            yield compareLists(ca.children(), cb.children());
          }
        };
      };

  private static int kindRank(TypeId t) {
    return switch (t) {
      case Builtin ignored -> 0;
      case DefsetMember ignored -> 1;
      case Composite ignored -> 2;
    };
  }

  private static int compareDefSetId(DefSetId a, DefSetId b) {
    int c = compareCompileId(a.compileId(), b.compileId());
    if (c != 0) return c;
    return Long.compare(a.index(), b.index());
  }

  private static int compareCompileId(CompileId a, CompileId b) {
    int c = Long.compare(a.importState().value(), b.importState().value());
    if (c != 0) return c;
    return Long.compare(a.source().value(), b.source().value());
  }

  private static int compareLists(TList<TypeId> a, TList<TypeId> b) {
    int n = Math.min(a.size(), b.size());
    for (int i = 0; i < n; i++) {
      int c = CANONICAL_ORDER.compare(a.get(i), b.get(i));
      if (c != 0) return c;
    }
    return Integer.compare(a.size(), b.size());
  }
}
