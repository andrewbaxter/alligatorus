package compilerxyz.main.langhelp;

import compilerxyz.main.eval.ExprErr;
import compilerxyz.main.langhelp.cache.CacheReal;
import compilerxyz.main.syntax.SourceSpecAbsolute;
import compilerxyz.prelude.util.Result;
import java.nio.file.Path;

public final class Main {
    private Main() {}

    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("usage: source <path.at>");
            System.exit(1);
        }
        var path = Path.of(args[0]).toAbsolutePath().normalize().toString();

        try (var cache = new CacheReal()) {
            var gx = GlobalContext.newContext(cache, cache, cache);

            var result = Compile.compile(gx, new SourceSpecAbsolute.Local(path));
            switch (result) {
                case Result.Ok<Compile.CompileResult, ?> ok -> {
                    var out = ok.value();
                    for (var log : out.logs()) {
                        System.out.println(log.message());
                    }
                }
                case Result.Err<?, ?> err -> {
                    @SuppressWarnings("unchecked")
                    var errs = (Iterable<ExprErr>) err.error();
                    for (var e : errs) {
                        System.err.println("error at " + e.loc() + ": " + e.errors());
                    }
                    System.exit(1);
                }
            }
        }
    }
}
