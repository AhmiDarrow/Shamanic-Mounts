package tk.darrow.shamanicmounts.sound;

import java.util.List;

import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import tk.darrow.shamanicmounts.ShamanicMounts;

public final class MountSounds {
	public static final DeferredRegister<SoundEvent> SOUNDS = DeferredRegister.create(Registries.SOUND_EVENT, ShamanicMounts.MOD_ID);

	/** Mount Flute phrases (tools/synth_flute.py); the flute plays them in turn. */
	public static final List<DeferredHolder<SoundEvent, SoundEvent>> FLUTE_CALLS = List.of(
			sound("item.flute.call_1"), sound("item.flute.call_2"),
			sound("item.flute.call_3"), sound("item.flute.call_4"));

	private static DeferredHolder<SoundEvent, SoundEvent> sound(String path) {
		return SOUNDS.register(path, () -> SoundEvent.createVariableRangeEvent(
				ResourceLocation.fromNamespaceAndPath(ShamanicMounts.MOD_ID, path)));
	}

	private MountSounds() {
	}
}
