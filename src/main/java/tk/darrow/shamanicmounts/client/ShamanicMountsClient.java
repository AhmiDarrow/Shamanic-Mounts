package tk.darrow.shamanicmounts.client;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;

import tk.darrow.shamanicmounts.ShamanicMounts;

/** Client hooks. The live harness starts only when {@code -Dshamanicmounts.harness=true}. */
@Mod(value = ShamanicMounts.MOD_ID, dist = Dist.CLIENT)
public final class ShamanicMountsClient {
	public ShamanicMountsClient(IEventBus modBus) {
		MountClient.install(modBus);
		if (Boolean.getBoolean("shamanicmounts.harness")) {
			// The harness driver is not in the released jar; it exists only in a development run.
			try {
				Class.forName("tk.darrow.shamanicmounts.client.HarnessVerification").getMethod("install").invoke(null);
				SolidDraw.stats = true;
			} catch (ReflectiveOperationException missing) {
				ShamanicMounts.LOGGER.warn("Harness requested but its driver is not in this build");
			}
		}
	}
}
