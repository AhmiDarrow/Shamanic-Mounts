package tk.darrow.shamanicmounts.book;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import tk.darrow.shamanicmounts.genome.Locus;

/**
 * Every shipped locale matches {@code en_us}: the same keys, valid JSON, and the same placeholders.
 * English stays the source the book methods return when the game language is not loaded.
 */
public class LangTest {
	private static final List<String> LOCALES = List.of(
			"en_us", "es_es", "es_mx", "fr_fr", "de_de", "pt_br", "it_it", "ru_ru", "pl_pl", "ja_jp", "ko_kr", "zh_cn");
	private static final List<String> CJK = List.of("ja_jp", "ko_kr", "zh_cn");
	private static final Pattern SPEC = Pattern.compile("%(?:(\\d+)\\$)?([sd])");

	@Test
	void everyLocaleMatchesEnglish() throws Exception {
		Map<String, String> english = load("en_us");
		assertTrue(english.size() > 68, "expected the book copy to be added to the original 68 keys");
		assertEquals("Shamanic Mounts", english.get("itemGroup.shamanicmounts"));
		for (String locale : LOCALES) {
			Map<String, String> file = load(locale);
			assertEquals(english.keySet(), file.keySet(), locale + " key set");
			for (Map.Entry<String, String> entry : file.entrySet()) {
				assertFalse(entry.getValue().isBlank(), locale + " " + entry.getKey());
				assertEquals(specs(english.get(entry.getKey())), specs(entry.getValue()),
						locale + " placeholders " + entry.getKey());
			}
			assertEquals("Shamanic Mounts", file.get("itemGroup.shamanicmounts"), locale);
		}
		assertEquals(load("es_es"), load("es_mx"));
	}

	@Test
	void shortLabelsFitTheirButtons() throws Exception {
		for (String locale : LOCALES) {
			if ("en_us".equals(locale)) {
				continue;
			}
			Map<String, String> file = load(locale);
			for (String key : List.of("mount.shamanicmounts.follow", "mount.shamanicmounts.stay", "mount.shamanicmounts.wander")) {
				assertTrue(widthBudget(locale, file.get(key)) <= 8, locale + " " + key + " = " + file.get(key));
			}
			for (String key : List.of("book.shamanicmounts.basics", "book.shamanicmounts.lines", "book.shamanicmounts.tames",
					"book.shamanicmounts.key", "book.shamanicmounts.breeding", "book.shamanicmounts.release",
					"book.shamanicmounts.release_sure", "book.shamanicmounts.back")) {
				assertTrue(widthBudget(locale, file.get(key)) <= 14, locale + " " + key + " = " + file.get(key));
			}
		}
	}

	@Test
	void englishBookCopyResolvesAndThereIsNoBundledFont() {
		assertNull(LangTest.class.getResource("/assets/shamanicmounts/font/default.json"));
		for (Codex.Section section : Codex.basics()) {
			assertResolved(section.heading());
			section.paragraphs().forEach(LangTest::assertResolved);
		}
		for (Codex.Section section : Codex.breeding()) {
			assertResolved(section.heading());
			section.paragraphs().forEach(LangTest::assertResolved);
		}
		for (Codex.Line line : Codex.lines()) {
			assertResolved(line.blurb());
			assertTrue(Copy.has(line.nameKey()), line.name());
		}
		Codex.keyIntro().forEach(LangTest::assertResolved);
		assertTrue(String.join(" ", Codex.keyIntro()).contains("capital letter shows"));
		for (Locus locus : Locus.values()) {
			assertResolved(Genotype.name(locus));
			assertResolved(Genotype.rule(locus));
		}
		assertEquals("body", Genotype.name(Locus.TORSO));
		assertTrue(Genotype.rule(Locus.TORSO).startsWith("Dominant: U > S > H > D > C > B > N"));
		assertEquals("Tame", Copy.en("book.shamanicmounts.default_name"));
	}

	private static void assertResolved(String text) {
		assertFalse(text == null || text.isBlank() || text.startsWith("book.shamanicmounts."), text);
	}

	/** Rough fit: CJK cells are wider, so count code points; Latin and Cyrillic count characters. */
	private static int widthBudget(String locale, String text) {
		int points = text.codePointCount(0, text.length());
		return CJK.contains(locale) ? points * 2 : points;
	}

	private static List<String> specs(String text) throws Exception {
		ArrayList<String> found = new ArrayList<>();
		int auto = 0;
		for (int i = 0; i < text.length(); i++) {
			if (text.charAt(i) != '%') {
				continue;
			}
			if (i + 1 < text.length() && text.charAt(i + 1) == '%') {
				i++;
				continue;
			}
			Matcher matcher = SPEC.matcher(text.substring(i));
			if (!matcher.lookingAt()) {
				throw new AssertionError("bad placeholder in: " + text);
			}
			String index = matcher.group(1);
			if (index == null) {
				auto++;
				index = Integer.toString(auto);
			}
			found.add(index + matcher.group(2));
			i += matcher.end() - 1;
		}
		found.sort(String::compareTo);
		return found;
	}

	private static Map<String, String> load(String locale) throws Exception {
		String path = "/assets/shamanicmounts/lang/" + locale + ".json";
		try (InputStream in = LangTest.class.getResourceAsStream(path)) {
			assertTrue(in != null, "missing " + path);
			JsonObject obj = JsonParser.parseString(new String(in.readAllBytes(), StandardCharsets.UTF_8)).getAsJsonObject();
			TreeMap<String, String> out = new TreeMap<>();
			for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
				assertTrue(entry.getValue().isJsonPrimitive(), locale + " " + entry.getKey());
				out.put(entry.getKey(), entry.getValue().getAsString());
			}
			return out;
		}
	}
}
