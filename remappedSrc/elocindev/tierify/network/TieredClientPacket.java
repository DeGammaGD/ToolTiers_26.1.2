package elocindev.tierify.network;

import elocindev.tierify.screen.client.ReforgeScreen;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

@Environment(EnvType.CLIENT)
public class TieredClientPacket {

    public static void init() {
        ClientPlayNetworking.registerGlobalReceiver(TieredServerPacket.REFORGE_READY_ID, (payload, context) -> {
            context.client().execute(() -> {
                if (context.client().screen instanceof ReforgeScreen)
                    ((ReforgeScreen) context.client().screen).reforgeButton.setDisabled(payload.disableButton());
            });
        });
        ClientPlayNetworking.registerGlobalReceiver(TieredServerPacket.HEALTH_ID, (payload, context) -> {
            context.client().execute(() -> {
                if (context.client().player != null) {
                    context.client().player.setHealth(payload.health());
                }
            });
        });
    }

    public static void writeC2SScreenPacket(int mouseX, int mouseY, boolean reforgingScreen) {
        ClientPlayNetworking.send(new TieredServerPacket.SetScreenPayload(mouseX, mouseY, reforgingScreen));
    }

    public static void writeC2SReforgePacket() {
        ClientPlayNetworking.send(new TieredServerPacket.ReforgePayload());
    }

}
