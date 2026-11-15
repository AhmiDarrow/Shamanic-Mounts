package tk.darrow.shamanicmounts.book;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * English copy loaded from {@code en_us.json}. Book methods that tests and the harness read return
 * this text. The screen asks the game language for the same keys, and a missing key comes back as
 * the key itself rather than a blank or a crash.
 */
public final class Copy {
	private static volatile Map<String, String> en;

	private Copy() {
	}

	public static String en(String key) {
		if (key == null || key.isEmpty()) {
			return "";
		}
		String value = map().get(key);
		return value == null || value.isEmpty() ? key : value;
	}

	public static boolean has(String key) {
		String value = map().get(key);
		return value != null && !value.isEmpty();
	}

	/** Lang key for one allele, note, or shown word. Spaces and pluses become underscores. */
	public static String wordKey(String token) {
		String slug = token.toLowerCase(Locale.ROOT).replace(' ', '_').replace('+', '_');
		return "book.shamanicmounts.word." + slug;
	}

	/** The English word, or the token itself when the lang file has no entry. */
	public static String enWord(String token) {
		if (token == null || token.isEmpty()) {
			return "";
		}
		String value = map().get(wordKey(token));
		return value == null || value.isEmpty() ? token : value;
	}

	private static Map<String, String> map() {
		Map<String, String> loaded = en;
		if (loaded != null) {
			return loaded;
		}
		synchronized (Copy.class) {
			if (en == null) {
				en = load();
			}
			return en;
		}
	}

	private static Map<String, String> load() {
		HashMap<String, String> out = new HashMap<>();
		try (InputStream in = Copy.class.getResourceAsStream("/assets/shamanicmounts/lang/en_us.json")) {
			if (in == null) {
				return Map.copyOf(out);
			}
			String text = new String(in.readAllBytes(), StandardCharsets.UTF_8);
			JsonObject obj = JsonParser.parseString(text).getAsJsonObject();
			for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
				if (entry.getValue().isJsonPrimitive()) {
					out.put(entry.getKey(), entry.getValue().getAsString());
				}
			}
		} catch (Exception ignored) {
			// A missing or unreadable file leaves the map empty. Callers then show the key.
		}
		return Map.copyOf(out);
	}
}
