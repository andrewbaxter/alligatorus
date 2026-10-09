package compilerxyz.main.langhelp.serve;

import compilerxyz.main.langserver.ClientMessage;
import compilerxyz.main.langserver.ClientMessageCodec;
import compilerxyz.main.langserver.ServerMessage;
import compilerxyz.main.langserver.ServerMessageCodec;
import compilerxyz.main.safejson.Json;
import compilerxyz.main.safejson.JsonValues;
import compilerxyz.prelude.util.Opt;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

public final class ServeProtocol {
    private final BufferedReader in;
    private final PrintWriter out;

    public ServeProtocol(InputStream in, OutputStream out) {
        this.in = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
        this.out = new PrintWriter(out, false, StandardCharsets.UTF_8);
    }

    public Opt<ClientMessage> protocolRead() {
        while (true) {
            String line;
            try {
                line = in.readLine();
            } catch (IOException e) {
                throw new RuntimeException("ServeProtocol: reading stdin", e);
            }
            if (line == null) return Opt.none();
            if (line.isBlank()) continue;
            try {
                return Opt.some(ClientMessageCodec.fromValue(JsonValues.parse(line), Json.FieldPath.root()));
            } catch (IOException | Json.DeserializationException e) {
                System.err.println("serve: ignoring unreadable message (" + e.getMessage() + "): " + line);
            }
        }
    }

    public synchronized void protocolWrite(ServerMessage message) {
        out.println(ServerMessageCodec.serialize(message));
        out.flush();
    }
}
