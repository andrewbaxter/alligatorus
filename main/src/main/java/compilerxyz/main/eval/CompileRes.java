package compilerxyz.main.eval;

import compilerxyz.prelude.util.TList;

public record CompileRes(long startMs, long endMs, TList<LogEntry> logs, CompileResResult result) {}
