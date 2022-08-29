import me.monmcgt.code.onstance.server.OnstanceServer;

public class TestMain {
    public static void main(String[] args) {
        OnstanceServer onstanceServer;
        if (args.length == 0) {
            onstanceServer = new OnstanceServer();
        } else {
            onstanceServer = new OnstanceServer(Integer.parseInt(args[0]));
        }
        onstanceServer.start();
    }
}
