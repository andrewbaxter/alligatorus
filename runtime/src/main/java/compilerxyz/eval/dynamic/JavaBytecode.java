package compilerxyz.eval.dynamic;

import compilerxyz.eval.BindingId;
import compilerxyz.util.Opt;
import compilerxyz.util.TList;

public interface JavaBytecode {
  <T> T dispatch(Dispatcher<T> dispatcher);

  record BindingUse(BindingId id) implements JavaBytecode {
    @Override
    public <T> T dispatch(Dispatcher<T> dispatcher) {
      throw new AssertionError(
          "JavaBytecode.BindingUse must be expanded by TargetDynamic before dispatch");
    }
  }

  record BindingSet(BindingId base, CodeDynamic value) implements JavaBytecode {
    @Override
    public <T> T dispatch(Dispatcher<T> dispatcher) {
      throw new AssertionError(
          "JavaBytecode.BindingSet must be expanded by TargetDynamic before dispatch");
    }
  }

  record Block(TList<BlockEntry> entries) implements JavaBytecode {
    @Override
    public <T> T dispatch(Dispatcher<T> dispatcher) {
      throw new AssertionError(
          "JavaBytecode.Block must be expanded by TargetDynamic before dispatch");
    }
  }

  sealed interface BlockEntry {
    record Code(CodeDynamic code) implements BlockEntry {}

    record Bind(BindingId base, CodeDynamic value) implements BlockEntry {}
  }

  record Foreach(CodeDynamic base, BindingId elementKey, CodeDynamic body) implements JavaBytecode {
    @Override
    public <T> T dispatch(Dispatcher<T> dispatcher) {
      throw new AssertionError(
          "JavaBytecode.Foreach must be expanded by TargetDynamic before dispatch");
    }
  }

  record ForeachMap(CodeDynamic base, BindingId elementKey, CodeDynamic body)
      implements JavaBytecode {
    @Override
    public <T> T dispatch(Dispatcher<T> dispatcher) {
      throw new AssertionError(
          "JavaBytecode.ForeachMap must be expanded by TargetDynamic before dispatch");
    }
  }

  record IfBranch(CodeDynamic condition, CodeDynamic body) {}

  record If(TList<IfBranch> conditions, Opt<CodeDynamic> elseBody) implements JavaBytecode {
    @Override
    public <T> T dispatch(Dispatcher<T> dispatcher) {
      throw new AssertionError(
          "JavaBytecode.If must be expanded by TargetDynamic before dispatch");
    }
  }

  record MatchBranch(CodeDynamic body) {}

  record Match(
      CodeDynamic base,
      BindingId elementScopeId,
      TList<MatchBranch> cases,
      Opt<CodeDynamic> fallback)
      implements JavaBytecode {
    @Override
    public <T> T dispatch(Dispatcher<T> dispatcher) {
      throw new AssertionError(
          "JavaBytecode.Match must be expanded by TargetDynamic before dispatch");
    }
  }

  record Catch(BindingId binding, CodeDynamic body) {}

  record Try(CodeDynamic body, Opt<Catch> catchClause, Opt<CodeDynamic> finallyBlock)
      implements JavaBytecode {
    @Override
    public <T> T dispatch(Dispatcher<T> dispatcher) {
      throw new AssertionError(
          "JavaBytecode.Try must be expanded by TargetDynamic before dispatch");
    }
  }

  interface Dispatcher<T> {
    T handleInstruction(JavaBytecodeInstruction n);

    T handleSequence(JavaBytecodeSequence n);

    T handleStoreLoad(JavaBytecodeStoreLoad n);

    T handleCatch(JavaBytecodeCatch n);

    T handleCatchStart(JavaBytecodeCatchStart n);

    T handleLand(JavaBytecodeLand n);

    T handleJump(JavaBytecodeJump n);
  }

  interface DefaultDispatcher<T> extends Dispatcher<T> {
    default T handleDefault(JavaBytecode n) {
      return null;
    }

    @Override
    default T handleJump(JavaBytecodeJump n) {
      return handleDefault(n);
    }

    @Override
    default T handleLand(JavaBytecodeLand n) {
      return handleDefault(n);
    }

    @Override
    default T handleCatch(JavaBytecodeCatch n) {
      return handleDefault(n);
    }

    @Override
    default T handleCatchStart(JavaBytecodeCatchStart n) {
      return handleDefault(n);
    }

    @Override
    default T handleInstruction(JavaBytecodeInstruction n) {
      return handleDefault(n);
    }

    @Override
    default T handleStoreLoad(JavaBytecodeStoreLoad n) {
      return handleDefault(n);
    }

    @Override
    default T handleSequence(JavaBytecodeSequence n) {
      return handleDefault(n);
    }
  }
}
