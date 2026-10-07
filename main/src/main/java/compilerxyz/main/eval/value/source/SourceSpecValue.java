package compilerxyz.main.eval.value.source;

import compilerxyz.main.syntax.RemoteOrigin;

public sealed interface SourceSpecValue {
    record Virtual(String name) implements SourceSpecValue {}

    record Local(String path) implements SourceSpecValue {}

    record Remote(RemoteOrigin origin) implements SourceSpecValue {}
}
