package compilerxyz.eval;

import compilerxyz.util.TList;

public sealed interface BuiltinTree {
  record Branch(TList<Child> children) implements BuiltinTree {}

  record Leaf(BuiltinLeaf leaf) implements BuiltinTree {}

  record Child(String name, BuiltinTree node) {}

  sealed interface BuiltinLeaf {
    record AnyConst(Type type) implements BuiltinLeaf {}

    record AspectId(BindingId dynamicKey, Unique unique, External dynamicValue)
        implements BuiltinLeaf {}

    record Constructor(BindingId dynamicKey, Type type, External dynamicValue)
        implements BuiltinLeaf {}

    record Function(BindingId dynamicKey, Type type, External dynamicValue)
        implements BuiltinLeaf {}

    record InitialBind(BindingId dynamicKey, Type type) implements BuiltinLeaf {}

    record Interface(BindingId dynamicKey, Type type, External dynamicValue)
        implements BuiltinLeaf {}
  }
}
