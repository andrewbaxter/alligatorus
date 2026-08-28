package compilerxyz.eval.value.source;

import compilerxyz.syntax.HashValue;

public sealed interface SourceSpecValue {
    record Virtual(String name) implements SourceSpecValue {}

    record Local(String path) implements SourceSpecValue {}

    record Remote(SourceSpecRemote remote) implements SourceSpecValue {}

    record SourceSpecRemote(String url, HashValue hash) {}
}
