package tk.darrow.shamanicmounts.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

/**
 * Particles and sounds for every gift, the brace, and the chimera's aura. Vanilla particles only.
 * Every recipe is a burst or a rate-limited trickle, spawned on the body rather than at the feet,
 * and none of them changes a rule.
 */
final class MountEffects {
	/** Ticks the drum ring keeps widening after the call. */
	static final int DRUM_RING_TICKS = 10;

	private MountEffects() {
	}

	private static ServerLevel server(ShamanicMount mount) {
		return mount.level() instanceof ServerLevel level ? level : null;
	}

	/** A point on the body: {@code along} in look direction and {@code across} to the right, both in widths. */
	private static Vec3 body(ShamanicMount mount, double along, double across, double height) {
		Vec3 look = mount.getLookAngle();
		Vec3 flat = new Vec3(look.x, 0.0, look.z);
		if (flat.lengthSqr() < 1.0e-6) {
			flat = new Vec3(0.0, 0.0, 1.0);
		}
		flat = flat.normalize();
		Vec3 right = new Vec3(-flat.z, 0.0, flat.x);
		double width = mount.getBbWidth();
		return mount.position()
				.add(flat.scale(along * width))
				.add(right.scale(across * width))
				.add(0.0, mount.getBbHeight() * height, 0.0);
	}

	private static void burst(ServerLevel level, ParticleOptions particle, Vec3 at, int count, double spread, double speed) {
		level.sendParticles(particle, at.x, at.y, at.z, count, spread, spread, spread, speed);
	}

	/** A flat ring of {@code count} particles at {@code radius} around the body's middle. */
	private static void ring(ServerLevel level, ParticleOptions particle, ShamanicMount mount, double radius, double height,
			int count) {
		Vec3 middle = body(mount, 0.0, 0.0, height);
		for (int i = 0; i < count; i++) {
			double angle = (Math.PI * 2.0 * i) / count;
			double x = middle.x + Math.cos(angle) * radius;
			double z = middle.z + Math.sin(angle) * radius;
			level.sendParticles(particle, x, middle.y, z, 1, 0.0, 0.05, 0.0, 0.0);
		}
	}

	/** Splash under a mount walking on water. Call every tick; it spawns on every fourth. */
	static void waterWalk(ShamanicMount mount) {
		ServerLevel level = server(mount);
		if (level == null || mount.tickCount % 4 != 0) {
			return;
		}
		Vec3 moved = mount.getDeltaMovement();
		if (moved.horizontalDistanceSqr() < 0.0025) {
			return;
		}
		Vec3 feet = body(mount, -0.2, 0.0, 0.0);
		level.sendParticles(ParticleTypes.SPLASH, feet.x, feet.y + 0.05, feet.z, 3, mount.getBbWidth() * 0.35, 0.02,
				mount.getBbWidth() * 0.35, 0.0);
	}

	/** Two low drum hits and the first ring. The ring keeps widening through {@link #drumRing}. */
	static void drum(ShamanicMount mount, Iterable<Player> healed) {
		ServerLevel level = server(mount);
		if (level == null) {
			return;
		}
		level.playSound(null, mount.getX(), mount.getY(), mount.getZ(), SoundEvents.NOTE_BLOCK_BASEDRUM.value(),
				SoundSource.NEUTRAL, 1.2f, 0.55f);
		level.playSound(null, mount.getX(), mount.getY(), mount.getZ(), SoundEvents.NOTE_BLOCK_BASEDRUM.value(),
				SoundSource.NEUTRAL, 0.8f, 0.7f);
		for (Player player : healed) {
			level.sendParticles(ParticleTypes.HAPPY_VILLAGER, player.getX(), player.getY() + player.getBbHeight() * 0.6,
					player.getZ(), 5, 0.3, 0.4, 0.3, 0.0);
		}
	}

	/** One step of the drum ring: eight notes at a radius that grows with {@code tick}, 1 to {@link #DRUM_RING_TICKS}. */
	static void drumRing(ShamanicMount mount, int tick) {
		ServerLevel level = server(mount);
		if (level == null) {
			return;
		}
		double radius = 0.6 + tick * 0.75;
		ring(level, ParticleTypes.NOTE, mount, radius, 0.55, 8);
	}

	/** The ram connected: sweep in front, crits on each target, dust off the ground. */
	static void ram(ShamanicMount mount, Iterable<LivingEntity> targets) {
		ServerLevel level = server(mount);
		if (level == null) {
			return;
		}
		Vec3 front = body(mount, 0.7, 0.0, 0.55);
		level.sendParticles(ParticleTypes.SWEEP_ATTACK, front.x, front.y, front.z, 1, 0.0, 0.0, 0.0, 0.0);
		for (LivingEntity target : targets) {
			level.sendParticles(ParticleTypes.CRIT, target.getX(), target.getY() + target.getBbHeight() * 0.6, target.getZ(),
					6, 0.25, 0.3, 0.25, 0.15);
		}
		BlockPos under = mount.blockPosition().below();
		BlockState state = level.getBlockState(under);
		if (!state.isAir()) {
			Vec3 feet = body(mount, 0.0, 0.0, 0.0);
			level.sendParticles(new BlockParticleOption(ParticleTypes.BLOCK, state), feet.x, feet.y + 0.1, feet.z, 10,
					mount.getBbWidth() * 0.4, 0.1, mount.getBbWidth() * 0.4, 0.1);
		}
	}

	/** A wisp off each wing tip. Call every glide tick; it spawns on every third. */
	static void glide(ShamanicMount mount) {
		ServerLevel level = server(mount);
		if (level == null || mount.tickCount % 3 != 0) {
			return;
		}
		for (int side = -1; side <= 1; side += 2) {
			Vec3 tip = body(mount, -0.1, side * 0.95, 0.7);
			level.sendParticles(ParticleTypes.CLOUD, tip.x, tip.y, tip.z, 1, 0.02, 0.02, 0.02, 0.0);
		}
	}

	/** Smoke and portal as the nagual slips away or comes back, with a low, soft teleport. */
	static void slip(ShamanicMount mount) {
		ServerLevel level = server(mount);
		if (level == null) {
			return;
		}
		Vec3 middle = body(mount, 0.0, 0.0, 0.5);
		burst(level, ParticleTypes.SMOKE, middle, 10, 0.35, 0.02);
		burst(level, ParticleTypes.PORTAL, middle, 12, 0.4, 0.4);
		level.playSound(null, middle.x, middle.y, middle.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.NEUTRAL, 0.5f, 0.6f);
	}

	/** A soul now and then on the rider whose mount is away. Call every away tick. */
	static void awayRider(ShamanicMount mount, Player rider) {
		ServerLevel level = server(mount);
		if (level == null || mount.tickCount % 20 != 0) {
			return;
		}
		level.sendParticles(ParticleTypes.SOUL, rider.getX(), rider.getY() + rider.getBbHeight() * 0.5, rider.getZ(), 1,
				0.2, 0.3, 0.2, 0.01);
	}

	/** The scent opens: a ring of souls around the hound. */
	static void scentStart(ShamanicMount mount) {
		ServerLevel level = server(mount);
		if (level == null) {
			return;
		}
		ring(level, ParticleTypes.SOUL, mount, mount.getBbWidth() * 0.9, 0.45, 10);
	}

	/** Breath from the nostrils on a night ride. Call every tick; it spawns on every twentieth. */
	static void nightBreath(ShamanicMount mount) {
		ServerLevel level = server(mount);
		if (level == null || mount.tickCount % 20 != 0 || !level.isNight()) {
			return;
		}
		Vec3 nose = body(mount, 0.65, 0.0, 0.8);
		level.sendParticles(ParticleTypes.SMOKE, nose.x, nose.y, nose.z, 2, 0.05, 0.03, 0.05, 0.005);
	}

	/** Astral sparks off the wing roots while climbing. Call every climb tick; it spawns on every fourth. */
	static void climb(ShamanicMount mount) {
		ServerLevel level = server(mount);
		if (level == null || mount.tickCount % 4 != 0) {
			return;
		}
		for (int side = -1; side <= 1; side += 2) {
			Vec3 root = body(mount, 0.0, side * 0.45, 0.75);
			level.sendParticles(ParticleTypes.END_ROD, root.x, root.y, root.z, 1, 0.05, 0.05, 0.05, 0.01);
		}
	}

	/** The climb bar ran dry. */
	static void spent(ShamanicMount mount) {
		ServerLevel level = server(mount);
		if (level == null) {
			return;
		}
		burst(level, ParticleTypes.POOF, body(mount, 0.0, 0.0, 0.7), 5, 0.3, 0.02);
	}

	/** A ring of dust as a flier that feels no fall sets down. */
	static void land(ShamanicMount mount) {
		ServerLevel level = server(mount);
		if (level == null) {
			return;
		}
		BlockState state = level.getBlockState(mount.blockPosition().below());
		ParticleOptions dust = state.isAir() ? ParticleTypes.POOF : new BlockParticleOption(ParticleTypes.BLOCK, state);
		ring(level, dust, mount, mount.getBbWidth() * 0.7, 0.02, 12);
	}

	/** The veil closes: a dark puff. */
	static void veilOn(ShamanicMount mount) {
		ServerLevel level = server(mount);
		if (level == null) {
			return;
		}
		Vec3 middle = body(mount, 0.0, 0.0, 0.5);
		burst(level, ParticleTypes.LARGE_SMOKE, middle, 8, 0.4, 0.01);
		burst(level, ParticleTypes.ASH, middle, 4, 0.5, 0.0);
	}

	/** The veil tears: a brief reverse portal. */
	static void veilOff(ShamanicMount mount) {
		ServerLevel level = server(mount);
		if (level == null) {
			return;
		}
		burst(level, ParticleTypes.REVERSE_PORTAL, body(mount, 0.0, 0.0, 0.5), 10, 0.35, 0.3);
	}

	/** Portal at one end of a blink. Called at the origin before the move and at the landing after it. */
	static void blink(ShamanicMount mount, boolean landing) {
		ServerLevel level = server(mount);
		if (level == null) {
			return;
		}
		Vec3 middle = body(mount, 0.0, 0.0, 0.5);
		burst(level, ParticleTypes.PORTAL, middle, 14, 0.4, 0.5);
		if (landing) {
			level.playSound(null, middle.x, middle.y, middle.z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.NEUTRAL, 0.4f, 1.6f);
		}
	}

	/** One spark near the plates. Call every tick; it spawns on every tenth. */
	static void aura(ShamanicMount mount) {
		ServerLevel level = server(mount);
		if (level == null || mount.tickCount % 10 != 0) {
			return;
		}
		Vec3 back = body(mount, -0.2, (mount.getRandom().nextDouble() - 0.5) * 0.5, 0.85);
		level.sendParticles(ParticleTypes.END_ROD, back.x, back.y, back.z, 1, 0.1, 0.05, 0.1, 0.0);
	}

	/** One braced jolt held: a snort from the nose. */
	static void jolt(ShamanicMount mount) {
		ServerLevel level = server(mount);
		if (level == null) {
			return;
		}
		Vec3 nose = body(mount, 0.65, 0.0, 0.8);
		level.sendParticles(ParticleTypes.SMOKE, nose.x, nose.y, nose.z, 4, 0.1, 0.05, 0.1, 0.02);
		level.playSound(null, nose.x, nose.y, nose.z, SoundEvents.HORSE_BREATHE, SoundSource.NEUTRAL, 0.8f, 0.9f);
	}

	/** The fourth jolt held: a quiet ring under the hearts. */
	static void tamed(ShamanicMount mount) {
		ServerLevel level = server(mount);
		if (level == null) {
			return;
		}
		ring(level, ParticleTypes.HAPPY_VILLAGER, mount, mount.getBbWidth() * 0.8, 0.4, 12);
	}
}
