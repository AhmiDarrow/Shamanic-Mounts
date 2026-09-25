package tk.darrow.shamanicmounts.entity;

import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.SpawnPlacementTypes;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.level.levelgen.Heightmap;
import net.neoforged.neoforge.event.entity.EntityAttributeCreationEvent;
import net.neoforged.neoforge.event.entity.RegisterSpawnPlacementsEvent;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.DeferredRegister;

import tk.darrow.shamanicmounts.ShamanicMounts;

public final class MountEntities {
	public static final DeferredRegister<EntityType<?>> ENTITIES = DeferredRegister.create(Registries.ENTITY_TYPE,
			ShamanicMounts.MOD_ID);

	public static final DeferredHolder<EntityType<?>, EntityType<ShamanicMount>> MOUNT = ENTITIES.register("mount",
			() -> EntityType.Builder.of(ShamanicMount::new, MobCategory.CREATURE).sized(1.4f, 1.6f).eyeHeight(1.4f)
					.clientTrackingRange(10).build("shamanicmounts:mount"));

	private MountEntities() {
	}

	public static void attributes(EntityAttributeCreationEvent event) {
		event.put(MOUNT.get(), ShamanicMount.createAttributes().build());
	}

	public static void placements(RegisterSpawnPlacementsEvent event) {
		event.register(MOUNT.get(), SpawnPlacementTypes.ON_GROUND, Heightmap.Types.MOTION_BLOCKING_NO_LEAVES,
				Animal::checkAnimalSpawnRules, RegisterSpawnPlacementsEvent.Operation.REPLACE);
	}
}
