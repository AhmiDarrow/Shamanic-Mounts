package tk.darrow.shamanicmounts.client;

import java.util.UUID;

import net.minecraft.client.Minecraft;

import tk.darrow.shamanicmounts.book.HerdBook;
import tk.darrow.shamanicmounts.book.SpoilerPref;

public final class ClientBook {
	private ClientBook() {
	}

	public static void open(UUID player, HerdBook book) {
		Minecraft.getInstance().setScreen(new HerdBookScreen(player, book, SpoilerPref.local()));
	}

	/** Rename is 0, release is 1. The screen already updated the copy the player is looking at. */
	public static void tellServer(int action, UUID id, String name) {
		net.neoforged.neoforge.network.PacketDistributor.sendToServer(
				new tk.darrow.shamanicmounts.net.MountPayloads.HerdEdit(action, id.toString(), name == null ? "" : name));
	}
}
