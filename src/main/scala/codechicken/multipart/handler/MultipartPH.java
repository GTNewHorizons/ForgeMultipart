package codechicken.multipart.handler;

public class MultipartPH {

    private final MultipartMod$ channel = MultipartMod$.MODULE$;
    private final String registryChannel = "ForgeMultipart";

    public MultipartMod$ channel() {
        return channel;
    }

    public String registryChannel() {
        return registryChannel;
    }
}
