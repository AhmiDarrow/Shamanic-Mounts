package tk.darrow.shamanicmounts.book;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import net.neoforged.fml.loading.FMLPaths;

/**
 * Whether this player wants the breeding chapter. Stored on this computer, one line per player,
 * including an explicit off so a later open still knows spoilers are hidden.
 */
public final class SpoilerPref {
	private final Path file;
	private final Map<UUID, Boolean> shown = new LinkedHashMap<>();

	public SpoilerPref(Path file) {
		this.file = file;
		load();
	}

	public static SpoilerPref local() {
		return new SpoilerPref(FMLPaths.CONFIGDIR.get().resolve("shamanicmounts").resolve("spoilers.txt"));
	}

	/** Spoilers are off until this player turns them on. */
	public boolean shown(UUID player) {
		return Boolean.TRUE.equals(shown.get(player));
	}

	public void set(UUID player, boolean spoilersOn) {
		shown.put(player, spoilersOn);
		save();
	}

	private void load() {
		shown.clear();
		if (!Files.isRegularFile(file)) {
			return;
		}
		try {
			for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
				String trimmed = line.strip();
				if (trimmed.isEmpty() || trimmed.startsWith("#")) {
					continue;
				}
				int space = trimmed.indexOf(' ');
				if (space <= 0) {
					continue;
				}
				shown.put(UUID.fromString(trimmed.substring(0, space)),
						Boolean.parseBoolean(trimmed.substring(space + 1).strip()));
			}
		} catch (IOException | IllegalArgumentException ignored) {
			shown.clear();
		}
	}

	private void save() {
		StringBuilder text = new StringBuilder();
		text.append("# player-uuid true|false\n");
		for (Map.Entry<UUID, Boolean> entry : shown.entrySet()) {
			text.append(entry.getKey()).append(' ').append(entry.getValue()).append('\n');
		}
		try {
			Files.createDirectories(file.getParent());
			Files.writeString(file, text.toString(), StandardCharsets.UTF_8);
		} catch (IOException failure) {
			throw new IllegalStateException("could not save spoiler preference", failure);
		}
	}
}
