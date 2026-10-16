package codechicken.multipart.handler;

/** Static facade for the optional MCPC placement hook. */
public final class MCPCCompatModule {

    private MCPCCompatModule() {}

    public static void load() {
        MCPCCompatModule$.MODULE$.load();
    }
}
