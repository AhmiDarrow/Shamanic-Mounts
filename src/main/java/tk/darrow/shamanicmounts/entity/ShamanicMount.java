package tk.darrow.shamanicmounts.entity;

import java.util.UUID;

import org.jetbrains.annotations.Nullable;

import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.Container;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.AgeableMob;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HasCustomInventoryScreen;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.PlayerRideableJumping;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.TamableAnimal;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.ai.goal.FloatGoal;
import net.minecraft.world.entity.ai.goal.FollowOwnerGoal;
import net.minecraft.world.entity.ai.goal.LookAtPlayerGoal;
import net.minecraft.world.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.world.entity.ai.goal.PanicGoal;
import net.minecraft.world.entity.ai.goal.RandomLookAroundGoal;
import net.minecraft.world.entity.ai.goal.SitWhenOrderedToGoal;
import net.minecraft.world.entity.ai.goal.WaterAvoidingRandomStrollGoal;
import net.minecraft.world.entity.ai.goal.target.HurtByTargetGoal;
import net.minecraft.world.entity.ai.goal.target.OwnerHurtByTargetGoal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import tk.darrow.shamanicmounts.book.HerdBook;
import tk.darrow.shamanicmounts.genome.Expression;
import tk.darrow.shamanicmounts.genome.Founders;
import tk.darrow.shamanicmounts.genome.Genome;
import tk.darrow.shamanicmounts.genome.GenomeIO;
import tk.darrow.shamanicmounts.genome.Meiosis;
import tk.darrow.shamanicmounts.genome.MountSize;
import tk.darrow.shamanicmounts.genome.Phenotype;
import tk.darrow.shamanicmounts.item.MountItems;
import tk.darrow.shamanicmounts.ride.BreedingRules;
import tk.darrow.shamanicmounts.ride.GiftRules;
import tk.darrow.shamanicmounts.ride.MountNames;
import tk.darrow.shamanicmounts.tack.SaddleRules;
import tk.darrow.shamanicmounts.tame.BraceTrial;

/**
 * One shamanic mount. Wild adults are tamed by the four-jolt brace. Foals are born tame.
 * Riding needs the shamanic saddle. A Diamond Apple breeds two tames the same player owns.
 */
public class ShamanicMount extends TamableAnimal implements PlayerRideableJumping, HasCustomInventoryScreen {
	private static final EntityDataAccessor<CompoundTag> DATA_GENOME = SynchedEntityData.defineId(ShamanicMount.class,
			EntityDataSerializers.COMPOUND_TAG);
	private static final EntityDataAccessor<Boolean> DATA_SADDLED = SynchedEntityData.defineId(ShamanicMount.class,
			EntityDataSerializers.BOOLEAN);
	private static final EntityDataAccessor<Integer> DATA_STAMINA = SynchedEntityData.defineId(ShamanicMount.class,
			EntityDataSerializers.INT);
	/** 0 wings folded, 1 gliding, 2 flapping. */
	private static final EntityDataAccessor<Byte> DATA_WING = SynchedEntityData.defineId(ShamanicMount.class,
			EntityDataSerializers.BYTE);
	/**
	 * The server's word on the climb. The rider's client moves the mount, so it needs to be told when
	 * the dream is lifting; the server keeps the stamina and decides.
	 */
	private static final EntityDataAccessor<Boolean> DATA_CLIMBING = SynchedEntityData.defineId(ShamanicMount.class,
			EntityDataSerializers.BOOLEAN);
	/** Horse armor on the mount: 0 none, 1 leather, 2 iron, 3 gold, 4 diamond. */
	private static final EntityDataAccessor<Byte> DATA_ARMOR = SynchedEntityData.defineId(ShamanicMount.class,
			EntityDataSerializers.BYTE);
	private static final net.minecraft.resources.ResourceLocation ARMOR_ID = net.minecraft.resources.ResourceLocation
			.fromNamespaceAndPath(tk.darrow.shamanicmounts.ShamanicMounts.MOD_ID, "horse_armor");
	/** Which of the line's three coats this one wears, 0 to 2. */
	private static final EntityDataAccessor<Byte> DATA_PELT = SynchedEntityData.defineId(ShamanicMount.class,
			EntityDataSerializers.BYTE);
	/** Saddle bags are strapped on. */
	private static final EntityDataAccessor<Boolean> DATA_BAGS = SynchedEntityData.defineId(ShamanicMount.class,
			EntityDataSerializers.BOOLEAN);
	/** Follow, stay, or wander, as {@link MountMode} ordinals. */
	private static final EntityDataAccessor<Byte> DATA_MODE = SynchedEntityData.defineId(ShamanicMount.class,
			EntityDataSerializers.BYTE);

	private Genome genome = Founders.eightfold();
	private Phenotype phenotype = Expression.express(genome);
	private boolean genomeLocked;
	private boolean male = true;
	private final SimpleContainer chest = new SimpleContainer(GiftRules.CHEST_SLOTS);
	/** The saddle and the saddle bags, as items, so the mount screen can take them on and off. */
	private final SimpleContainer tack = new SimpleContainer(3);
	private BraceTrial.Trial trial;
	private int refuseTicks;
	private int restTicks;
	private int offerTicks;
	private UUID offerOwner;
	/** An operator's offer stays open until the pair is made. */
	private boolean offerSticky;
	private boolean jumpHeld;
	private boolean hopUsed;
	private boolean climbSpent;
	private boolean climbing;
	private int drumCooldown;
	private int ramCooldown;
	private int maulCooldown;
	private int coilCooldown;
	private int awayTicks;
	private int awayCooldown;
	private int blinkCooldown;
	private int revealTicks;
	private float lastExhaustion = -1.0f;
	private boolean trialTookSaddle;
	private boolean herdChecked;
	private UUID dam;
	private UUID sire;
	private float wingOpen;
	private float wingOpenO;
	/** Ticks left on the widening drum ring. */
	private int drumRing;
	/** The shade's veil is closed around the rider. */
	private boolean hidden;
	/** Ticks the rider has held sneak. A short press steps off; a hold hides or aims the blink. */
	private int sneakHeld;
	/** True while the mount itself throws its riders off, so a held sneak cannot keep them on. */
	private boolean ejecting;
	/** The rider's sneak key as the client reports it; vanilla drops it every tick while riding. */
	private boolean riderSneak;
	/** The scent ring was shown for this ride. */
	private boolean scentShown;
	/** Ticks a fall-proof flier has been off the ground while ridden. */
	private int airTicks;
	private int lastJolts;

	public ShamanicMount(EntityType<? extends ShamanicMount> type, Level level) {
		super(type, level);
		this.tack.addListener(container -> syncTack());
	}

	/** The tack slots decide what is on: a saddle item means saddled, bags mean bags. */
	private void syncTack() {
		if (this.level().isClientSide()) {
			return;
		}
		boolean saddled = !tack.getItem(MountChestMenu.SADDLE_SLOT).isEmpty();
		boolean bags = !tack.getItem(MountChestMenu.BAGS_SLOT).isEmpty();
		if (this.entityData.get(DATA_SADDLED) != saddled) {
			this.entityData.set(DATA_SADDLED, saddled);
			if (!saddled && this.trial == null) {
				this.ejectPassengers();
			}
		}
		if (this.entityData.get(DATA_BAGS) != bags) {
			this.entityData.set(DATA_BAGS, bags);
			if (!bags && !this.chest.isEmpty()) {
				Containers.dropContents(this.level(), this, this.chest);
				this.chest.clearContent();
			}
		}
		ItemStack armor = tack.getItem(MountChestMenu.ARMOR_SLOT);
		this.entityData.set(DATA_ARMOR, (byte) armorTier(armor));
		var attribute = this.getAttribute(Attributes.ARMOR);
		if (attribute != null) {
			if (armor.getItem() instanceof net.minecraft.world.item.ArmorItem piece) {
				attribute.addOrUpdateTransientModifier(new net.minecraft.world.entity.ai.attributes.AttributeModifier(ARMOR_ID,
						piece.getDefense(), net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation.ADD_VALUE));
			} else {
				attribute.removeModifier(ARMOR_ID);
			}
		}
	}

	/** Which vanilla horse armor is on, for the look: 0 none, 1 leather, 2 iron, 3 gold, 4 diamond. */
	private static int armorTier(ItemStack stack) {
		if (stack.is(net.minecraft.world.item.Items.DIAMOND_HORSE_ARMOR)) {
			return 4;
		}
		if (stack.is(net.minecraft.world.item.Items.GOLDEN_HORSE_ARMOR)) {
			return 3;
		}
		if (stack.is(net.minecraft.world.item.Items.IRON_HORSE_ARMOR)) {
			return 2;
		}
		return stack.isEmpty() ? 0 : 1;
	}

	public int armorTier() {
		return this.entityData.get(DATA_ARMOR);
	}

	public boolean hasBags() {
		return this.entityData.get(DATA_BAGS);
	}

	/** Which of the line's three coats this mount wears, 0 to 2. */
	public int pelt() {
		return this.entityData.get(DATA_PELT);
	}

	public void setPelt(int pelt) {
		this.entityData.set(DATA_PELT, (byte) Math.floorMod(pelt, 3));
		peltRolled = true;
	}

	private boolean peltRolled;

	public SimpleContainer tack() {
		return tack;
	}

	public static AttributeSupplier.Builder createAttributes() {
		return Mob.createMobAttributes()
				.add(Attributes.MAX_HEALTH, 26.0)
				.add(Attributes.MOVEMENT_SPEED, 0.25)
				.add(Attributes.ATTACK_DAMAGE, 3.0)
				.add(Attributes.FOLLOW_RANGE, 24.0)
				.add(Attributes.STEP_HEIGHT, 0.6);
	}

	@Override
	protected void defineSynchedData(SynchedEntityData.Builder builder) {
		super.defineSynchedData(builder);
		builder.define(DATA_GENOME, GenomeIO.write(Founders.eightfold()));
		builder.define(DATA_SADDLED, false);
		builder.define(DATA_STAMINA, GiftRules.CLIMB_TICKS);
		builder.define(DATA_WING, (byte) 0);
		builder.define(DATA_CLIMBING, false);
		builder.define(DATA_MODE, (byte) MountMode.WANDER.ordinal());
		builder.define(DATA_BAGS, false);
		builder.define(DATA_PELT, (byte) 0);
		builder.define(DATA_ARMOR, (byte) 0);
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2, true));
		this.goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
		this.goalSelector.addGoal(3, new FollowOwnerGoal(this, 1.1, 8.0f, 2.0f) {
			@Override
			public boolean canUse() {
				return mode() == MountMode.FOLLOW && super.canUse();
			}

			@Override
			public boolean canContinueToUse() {
				return mode() == MountMode.FOLLOW && super.canContinueToUse();
			}
		});
		this.goalSelector.addGoal(4, new PanicGoal(this, 1.3));
		// A wild foal keeps to the grown mounts of its kind until it is grown itself.
		this.goalSelector.addGoal(5, new net.minecraft.world.entity.ai.goal.FollowParentGoal(this, 1.1));
		this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0) {
			@Override
			public boolean canUse() {
				return (!isTame() || mode() == MountMode.WANDER) && super.canUse();
			}
		});
		this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0f));
		this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
		this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new HurtByTargetGoal(this));
	}

	public Genome genome() {
		return genome;
	}

	public Phenotype phenotype() {
		return phenotype;
	}

	public int wingPose() {
		return this.entityData.get(DATA_WING) & 255;
	}

	public float wingOpen(float partial) {
		return net.minecraft.util.Mth.lerp(partial, this.wingOpenO, this.wingOpen);
	}

	public boolean saddled() {
		return this.entityData.get(DATA_SADDLED);
	}

	public boolean male() {
		return male;
	}

	/** Eggs and wild spawns roll this. Breeding rolls it on the foal. */
	public void rollSex() {
		this.male = this.random.nextBoolean();
	}

	/** A loaded mount in any dimension, or null when its chunk is not loaded. */
	public static ShamanicMount loaded(net.minecraft.server.MinecraftServer server, UUID id) {
		for (ServerLevel level : server.getAllLevels()) {
			if (level.getEntity(id) instanceof ShamanicMount mount) {
				return mount;
			}
		}
		return null;
	}

	public int stamina() {
		return this.entityData.get(DATA_STAMINA);
	}

	public SimpleContainer chest() {
		return chest;
	}

	public MountMode mode() {
		return MountMode.of(this.entityData.get(DATA_MODE));
	}

	/** Follow, stay, or wander. Stay is the sit; the other two stand it back up. */
	public void setMode(MountMode mode) {
		this.entityData.set(DATA_MODE, (byte) mode.ordinal());
		if (!this.level().isClientSide()) {
			this.setOrderedToSit(mode == MountMode.STAY);
		}
	}

	public boolean jumpHeld() {
		return jumpHeld;
	}

	public int revealTicks() {
		return revealTicks;
	}

	public void reveal() {
		revealTicks = GiftRules.REVEAL_TICKS;
		if (hidden) {
			hidden = false;
			MountEffects.veilOff(this);
		}
	}

	public boolean isAway() {
		return awayTicks > 0;
	}

	/** Wild spawn and eggs call this. A locked genome is not replaced by a random founder. */
	public void setGenome(Genome genome, boolean locked) {
		this.genome = genome;
		this.phenotype = Expression.express(genome);
		this.genomeLocked = this.genomeLocked || locked;
		if (!this.level().isClientSide()) {
			this.entityData.set(DATA_GENOME, GenomeIO.write(genome));
		}
		applyHealth();
		this.refreshDimensions();
	}

	private void applyHealth() {
		double health = switch (phenotype.scale) {
			case SLIGHT -> 22.0;
			case NORMAL -> 26.0;
			case LARGE -> 32.0;
			case GREATER -> 40.0;
		};
		var attribute = this.getAttribute(Attributes.MAX_HEALTH);
		if (attribute != null && attribute.getBaseValue() != health) {
			float ratio = this.getMaxHealth() <= 0 ? 1.0f : this.getHealth() / this.getMaxHealth();
			attribute.setBaseValue(health);
			this.setHealth(Math.max(1.0f, (float) health * ratio));
		}
	}

	@Override
	public void onSyncedDataUpdated(EntityDataAccessor<?> key) {
		super.onSyncedDataUpdated(key);
		if (DATA_GENOME.equals(key) && this.level().isClientSide()) {
			try {
				this.genome = GenomeIO.read(this.entityData.get(DATA_GENOME));
				this.phenotype = Expression.express(genome);
			} catch (RuntimeException ignored) {
				this.genome = Founders.eightfold();
				this.phenotype = Expression.express(genome);
			}
			this.refreshDimensions();
		}
	}

	@Override
	protected EntityDimensions getDefaultDimensions(Pose pose) {
		return EntityDimensions.scalable(MountSize.width(phenotype), MountSize.height(phenotype));
	}

	@Override
	public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, net.minecraft.world.DifficultyInstance difficulty,
			MobSpawnType reason, SpawnGroupData data) {
		if (!genomeLocked) {
			Genome[] founders = { Founders.eightfold(), Founders.drumHart(), Founders.elk(), Founders.crane(),
					Founders.nagual(), Founders.barghest(), Founders.roc(), Founders.shade(), Founders.bear(), Founders.serpent() };
			setGenome(founders[this.random.nextInt(founders.length)], false);
			this.male = this.random.nextBoolean();
		}
		if (!peltRolled) {
			setPelt(this.random.nextInt(3));
		}
		return super.finalizeSpawn(level, difficulty, reason, data);
	}

	@Override
	public void tick() {
		super.tick();
		if (this.level().isClientSide()) {
			this.wingOpenO = this.wingOpen;
			float target = this.wingPose() == 0 ? 0.0f : 1.0f;
			this.wingOpen += (target - this.wingOpen) * 0.35f;
			return;
		}
		if (refuseTicks > 0) {
			refuseTicks--;
		}
		if (restTicks > 0) {
			restTicks--;
		}
		if (offerTicks > 0 && !offerSticky && --offerTicks == 0) {
			offerOwner = null;
		}
		if (drumCooldown > 0) {
			drumCooldown--;
		}
		if (ramCooldown > 0) {
			ramCooldown--;
		}
		if (maulCooldown > 0) {
			maulCooldown--;
		}
		if (coilCooldown > 0) {
			coilCooldown--;
		}
		if (GiftRules.coil(phenotype) && this.getAirSupply() < this.getMaxAirSupply()) {
			this.setAirSupply(this.getMaxAirSupply());
		}
		if (awayCooldown > 0) {
			awayCooldown--;
		}
		if (blinkCooldown > 0) {
			blinkCooldown--;
		}
		if (revealTicks > 0) {
			revealTicks--;
		}
		if (drumRing > 0) {
			MountEffects.drumRing(this, MountEffects.DRUM_RING_TICKS - drumRing + 1);
			drumRing--;
		}
		if (phenotype.chimera) {
			MountEffects.aura(this);
		}
		if (!herdChecked) {
			herdChecked = true;
			reconcileHerd();
		}
		tickTrial();
		tickAway();
		tickRiding();
		tickLanding();
		if (!this.level().isClientSide()) {
			syncWing();
		}
		if (GiftRules.guard(phenotype) && this.isOrderedToSit() && this.getTarget() != null) {
			this.setOrderedToSit(false);
		} else if (mode() == MountMode.STAY && !this.isOrderedToSit() && this.getTarget() == null && !this.isVehicle()) {
			this.setOrderedToSit(true);
		}
		float step = this.isVehicle() && GiftRules.fullStep(phenotype) ? 1.0f : 0.6f;
		var stepHeight = this.getAttribute(Attributes.STEP_HEIGHT);
		if (stepHeight != null && stepHeight.getBaseValue() != step) {
			stepHeight.setBaseValue(step);
		}
	}

	private void reconcileHerd() {
		if (!(this.level() instanceof ServerLevel server)) {
			return;
		}
		HerdBook.Entry entry = MountHerdData.get(server).book().get(this.getUUID());
		if (entry != null && !entry.tame() && this.isTame()) {
			releaseIntoWorld(null);
		}
	}

	private void tickLanding() {
		boolean flier = this.isVehicle() && phenotype.gifts.contains(tk.darrow.shamanicmounts.genome.Marks.Gift.DREAM);
		if (flier && !this.onGround() && !this.isInWater()) {
			airTicks++;
			return;
		}
		if (flier && this.onGround() && airTicks > 12) {
			MountEffects.land(this);
		}
		airTicks = 0;
	}

	private void tickTrial() {
		if (trial == null || trial.finished()) {
			return;
		}
		boolean riding = this.getControllingPassenger() instanceof Player;
		BraceTrial.tick(trial, jumpHeld, !riding, false);
		if (trial.jolts() > lastJolts) {
			lastJolts = trial.jolts();
			if (!trial.done()) {
				MountEffects.jolt(this);
			}
		}
		this.setDeltaMovement(0.0, this.getDeltaMovement().y, 0.0);
		if (trial.failed()) {
			failTrial();
		} else if (trial.done()) {
			finishTame();
		}
	}

	private void tickAway() {
		if (awayTicks <= 0) {
			return;
		}
		awayTicks--;
		Player owner = ownerAnywhere();
		if (owner == null) {
			returnNow();
			return;
		}
		follow(owner);
		MountEffects.awayRider(this, owner);
		if (awayTicks <= 0) {
			returnNow();
		}
	}

	/** Sneak has a job on this mount, so a held sneak keeps the rider on and a tap steps off. */
	public boolean holdsSneak() {
		return GiftRules.sneakHide(phenotype) || GiftRules.blink(phenotype) || GiftRules.coil(phenotype);
	}

	/** Whether a held sneak may keep {@code player} on: only the controlling rider, and never while thrown. */
	public boolean sneakKeepsOn(Player player) {
		return holdsSneak() && !ejecting && this.trial == null && player == this.getControllingPassenger();
	}

	@Override
	public void ejectPassengers() {
		ejecting = true;
		try {
			super.ejectPassengers();
		} finally {
			ejecting = false;
		}
	}

	/** Ticks the current sneak has been held. */
	public int sneakHeld() {
		return sneakHeld;
	}

	public boolean riderSneak() {
		return riderSneak;
	}

	/** The rider's keys, from the client, on both sides. A fresh jump press also allows one more hop. */
	public void riderKeys(Player player, boolean sneak, boolean jump) {
		if (player != this.getControllingPassenger()) {
			return;
		}
		riderSneak = sneak;
		if (jump && !jumpHeld) {
			hopUsed = false;
		}
		jumpHeld = jump;
	}

	private void tickRiding() {
		if (this.isVehicle() && this.isOrderedToSit()) {
			this.setOrderedToSit(false);
		}
		if (!(this.getControllingPassenger() instanceof Player player) || isAway()) {
			lastExhaustion = -1.0f;
			hidden = false;
			scentShown = false;
			sneakHeld = 0;
			riderSneak = false;
			jumpHeld = false;
			return;
		}
		if (holdsSneak()) {
			if (riderSneak) {
				sneakHeld++;
			} else {
				if (sneakHeld > 0 && sneakHeld <= GiftRules.SNEAK_TAP_TICKS) {
					sneakHeld = 0;
					player.setShiftKeyDown(false);
					player.stopRiding();
					return;
				}
				sneakHeld = 0;
			}
		}
		if (GiftRules.drum(phenotype)) {
			player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
					net.minecraft.world.effect.MobEffects.NIGHT_VISION, 40, 0, true, false, true));
		}
		if (GiftRules.pinion(phenotype)) {
			player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
					net.minecraft.world.effect.MobEffects.SLOW_FALLING, 10, 0, true, false, true));
			this.addEffect(new net.minecraft.world.effect.MobEffectInstance(
					net.minecraft.world.effect.MobEffects.SLOW_FALLING, 10, 0, true, false, true));
		}
		if (GiftRules.longevity(phenotype)) {
			float now = player.getFoodData().getExhaustionLevel();
			if (lastExhaustion >= 0.0f) {
				float target = GiftRules.halvedExhaustion(lastExhaustion, now);
				if (target < now) {
					player.getFoodData().addExhaustion(target - now);
				}
				lastExhaustion = target;
			} else {
				lastExhaustion = now;
			}
		}
		if (GiftRules.deepCoil(genome)) {
			player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
					net.minecraft.world.effect.MobEffects.WATER_BREATHING, 40, 0, true, false, true));
		}
		if (GiftRules.thickHide(genome)) {
			player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
					net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 40, 0, true, false, true));
		}
		if (GiftRules.shadowSpeed(phenotype) && this.level().getMaxLocalRawBrightness(this.blockPosition()) < 7) {
			player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
					net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED, 40, 0, true, false, true));
		}
		boolean hide = GiftRules.sneakHide(phenotype) && riderSneak && revealTicks <= 0;
		if (hide) {
			quietNearby(player);
			if (!hidden) {
				MountEffects.veilOn(this);
			}
		}
		hidden = hide;
		int glow = GiftRules.glowRange(phenotype);
		if (glow > 0) {
			if (!scentShown) {
				scentShown = true;
				MountEffects.scentStart(this);
			}
			MountEffects.nightBreath(this);
		}
		if (glow > 0 && this.tickCount % 10 == 0) {
			AABB box = this.getBoundingBox().inflate(glow);
			for (Mob mob : this.level().getEntitiesOfClass(Mob.class, box, LivingEntity::isAlive)) {
				if (mob.getType().getCategory() == net.minecraft.world.entity.MobCategory.MONSTER) {
					mob.addEffect(new net.minecraft.world.effect.MobEffectInstance(
							net.minecraft.world.effect.MobEffects.GLOWING, 40, 0, true, false, false));
				}
			}
		}
		tickFlight(player);
		if (player.swinging && player.swingTime == 1) {
			reveal();
			if (GiftRules.coil(phenotype)) {
				tryCoil(player);
			} else if (GiftRules.might(phenotype)) {
				tryMaul(player);
			} else {
				tryRam(player);
			}
		}
	}

	/**
	 * The dream's climb, decided here and published for the rider's client, which does the moving.
	 * Stamina drains while climbing and refills on the ground or in a glide; an empty bar must refill
	 * all the way before the next climb.
	 */
	private void tickFlight(Player player) {
		boolean night = this.level().isNight();
		int stamina = this.stamina();
		boolean climb = GiftRules.canClimb(phenotype, genome, night) && jumpHeld && !climbSpent && stamina > 0;
		climbing = climb;
		if (climb) {
			int next = stamina - 1;
			this.entityData.set(DATA_STAMINA, next);
			MountEffects.climb(this);
			if (next <= 0) {
				climbSpent = true;
				MountEffects.spent(this);
			}
			if (GiftRules.climbFeedsHunger(genome)) {
				player.causeFoodExhaustion(0.04f);
			}
		} else {
			if (stamina < GiftRules.CLIMB_TICKS) {
				this.entityData.set(DATA_STAMINA, stamina + 1);
			} else {
				climbSpent = false;
			}
			if (GiftRules.glide(phenotype) && jumpHeld && !this.onGround() && !this.isInWater()) {
				MountEffects.glide(this);
			}
		}
		if (this.entityData.get(DATA_CLIMBING) != climb) {
			this.entityData.set(DATA_CLIMBING, climb);
		}
	}

	private void quietNearby(Player player) {
		AABB box = player.getBoundingBox().inflate(24.0);
		for (Mob mob : this.level().getEntitiesOfClass(Mob.class, box)) {
			if (mob.getTarget() == player || mob.getTarget() == this) {
				mob.setTarget(null);
			}
		}
	}

	/**
	 * The use key. A mount with one of these does that one. A mount with several:
	 * use plays the drum, then send-away, then blink. Sneak and use blinks first,
	 * so a complete mount can still reach the blink without waiting out the drum.
	 */
	public void used(Player player) {
		if (this.level().isClientSide() || player != this.getControllingPassenger()) {
			return;
		}
		boolean sneak = riderSneak || player.isShiftKeyDown();
		if (sneak && GiftRules.blink(phenotype) && blinkCooldown <= 0 && blink(player)) {
			return;
		}
		if (!sneak && GiftRules.drum(phenotype) && drumCooldown <= 0) {
			drum(player);
			return;
		}
		if (!sneak && GiftRules.nagual(phenotype) && awayCooldown <= 0 && awayTicks <= 0) {
			sendAway(player);
			return;
		}
		if (GiftRules.blink(phenotype) && blinkCooldown <= 0) {
			blink(player);
		}
	}

	private void drum(Player player) {
		drumCooldown = GiftRules.DRUM_COOLDOWN;
		var regen = new net.minecraft.world.effect.MobEffectInstance(
				net.minecraft.world.effect.MobEffects.REGENERATION, GiftRules.DRUM_DURATION, 1, false, true, true);
		player.addEffect(regen);
		this.addEffect(new net.minecraft.world.effect.MobEffectInstance(regen));
		java.util.List<Player> healed = this.level().getEntitiesOfClass(Player.class,
				this.getBoundingBox().inflate(GiftRules.DRUM_RANGE));
		for (Player near : healed) {
			near.addEffect(new net.minecraft.world.effect.MobEffectInstance(regen));
		}
		if (!healed.contains(player)) {
			healed.add(player);
		}
		drumRing = MountEffects.DRUM_RING_TICKS;
		MountEffects.drum(this, healed);
	}

	private void sendAway(Player player) {
		awayTicks = GiftRules.AWAY_DURATION;
		awayCooldown = GiftRules.AWAY_COOLDOWN;
		this.ejectPassengers();
		MountEffects.slip(this);
		this.setInvisible(true);
		this.noPhysics = true;
		player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
				net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED, GiftRules.AWAY_DURATION, 1, false, true, true));
		player.addEffect(new net.minecraft.world.effect.MobEffectInstance(
				net.minecraft.world.effect.MobEffects.JUMP, GiftRules.AWAY_DURATION, 1, false, true, true));
		player.getPersistentData().putInt("shamanicmounts_skin", GiftRules.AWAY_DURATION);
		player.getPersistentData().putUUID("shamanicmounts_away", this.getUUID());
	}

	/** The owner wherever they are, not only in this level. */
	@Nullable
	private Player ownerAnywhere() {
		if (this.getOwner() instanceof Player here) {
			return here;
		}
		UUID id = this.getOwnerUUID();
		if (id == null || !(this.level() instanceof ServerLevel server)) {
			return null;
		}
		return server.getServer().getPlayerList().getPlayer(id);
	}

	public void returnNow() {
		awayTicks = 0;
		this.setInvisible(false);
		this.noPhysics = false;
		if (ownerAnywhere() instanceof Player player) {
			if (player.level() == this.level()) {
				this.teleportTo(player.getX() + 1.0, player.getY(), player.getZ());
				MountEffects.slip(this);
			} else {
				follow(player);
			}
			player.getPersistentData().remove("shamanicmounts_skin");
			player.getPersistentData().remove("shamanicmounts_away");
		}
	}

	private void follow(Player player) {
		if (player.level() != this.level() && player.level() instanceof ServerLevel dest) {
			this.changeDimension(new net.minecraft.world.level.portal.DimensionTransition(dest,
					new Vec3(player.getX() + 1.0, player.getY(), player.getZ()), Vec3.ZERO, this.getYRot(),
					this.getXRot(), net.minecraft.world.level.portal.DimensionTransition.DO_NOTHING));
			return;
		}
		this.teleportTo(player.getX() + 1.0, player.getY(), player.getZ());
	}

	private boolean blink(Player player) {
		Vec3 look = player.getLookAngle();
		Vec3 flat = new Vec3(look.x, 0.0, look.z);
		if (flat.lengthSqr() < 1.0e-4) {
			flat = this.getLookAngle();
			flat = new Vec3(flat.x, 0.0, flat.z);
		}
		flat = flat.normalize();
		// Level unless the rider is looking well up; then the blink rises with the gaze.
		Vec3 dir = look.y > 0.5 ? look.normalize() : flat;
		Vec3 dest = this.position().add(dir.scale(GiftRules.BLINK_BLOCKS));
		BlockPos feet = BlockPos.containing(dest);
		if (!this.level().getBlockState(feet).isAir() || !this.level().getBlockState(feet.above()).isAir()) {
			return false;
		}
		MountEffects.blink(this, false);
		this.teleportTo(dest.x, dest.y, dest.z);
		if (player instanceof ServerPlayer rider) {
			net.neoforged.neoforge.network.PacketDistributor.sendToPlayer(rider,
					new tk.darrow.shamanicmounts.net.MountPayloads.MountWarp(dest.x, dest.y, dest.z));
		}
		blinkCooldown = GiftRules.BLINK_COOLDOWN;
		MountEffects.blink(this, true);
		return true;
	}

	/** The serpent's coil: the nearest creature in front is held fast and squeezed for three seconds. */
	private void tryCoil(Player player) {
		if (coilCooldown > 0) {
			return;
		}
		Vec3 look = this.getLookAngle();
		AABB box = this.getBoundingBox().expandTowards(look.scale(2.5)).inflate(0.6);
		LivingEntity nearest = null;
		double best = Double.MAX_VALUE;
		for (LivingEntity living : this.level().getEntitiesOfClass(LivingEntity.class, box,
				other -> other != this && other != player && other.isAlive())) {
			double d = living.distanceToSqr(this);
			if (d < best) {
				best = d;
				nearest = living;
			}
		}
		if (nearest == null) {
			return;
		}
		coilCooldown = GiftRules.COIL_COOLDOWN;
		nearest.hurt(this.damageSources().mobAttack(this), 4.0f);
		nearest.addEffect(new net.minecraft.world.effect.MobEffectInstance(
				net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, GiftRules.COIL_TICKS, 4, false, true, true));
		nearest.addEffect(new net.minecraft.world.effect.MobEffectInstance(
				net.minecraft.world.effect.MobEffects.WEAKNESS, GiftRules.COIL_TICKS, 1, false, true, true));
		nearest.addEffect(new net.minecraft.world.effect.MobEffectInstance(
				net.minecraft.world.effect.MobEffects.POISON, GiftRules.COIL_TICKS, 0, false, true, true));
		this.playSound(SoundEvents.SNIFFER_DIGGING, 0.8f, 0.6f);
		MountEffects.ram(this, java.util.List.of(nearest));
	}

	/** The bear's maul: a heavy swipe at everything in front, with a roar. */
	private void tryMaul(Player player) {
		if (maulCooldown > 0) {
			return;
		}
		maulCooldown = GiftRules.MAUL_COOLDOWN;
		Vec3 look = this.getLookAngle();
		AABB box = this.getBoundingBox().expandTowards(look.scale(2.4)).inflate(0.8);
		java.util.List<LivingEntity> targets = this.level().getEntitiesOfClass(LivingEntity.class, box,
				living -> living != this && living != player && living.isAlive());
		for (LivingEntity target : targets) {
			target.hurt(this.damageSources().mobAttack(this), GiftRules.MAUL_DAMAGE);
			target.knockback(1.0, -look.x, -look.z);
			target.addEffect(new net.minecraft.world.effect.MobEffectInstance(
					net.minecraft.world.effect.MobEffects.MOVEMENT_SLOWDOWN, 40, 1, false, true, true));
		}
		this.playSound(SoundEvents.POLAR_BEAR_WARNING, 0.9f, 0.85f);
		MountEffects.ram(this, targets);
	}

	private void tryRam(Player player) {
		if (!GiftRules.ram(phenotype) || ramCooldown > 0) {
			return;
		}
		ramCooldown = GiftRules.RAM_COOLDOWN;
		Vec3 look = this.getLookAngle();
		this.setDeltaMovement(look.x * 0.9, 0.15, look.z * 0.9);
		AABB box = this.getBoundingBox().expandTowards(look.scale(2.0)).inflate(0.5);
		java.util.List<LivingEntity> targets = this.level().getEntitiesOfClass(LivingEntity.class, box,
				living -> living != this && living != player && living.isAlive());
		for (LivingEntity target : targets) {
			target.hurt(this.damageSources().mobAttack(this), 3.0f);
			target.knockback(1.4, -look.x, -look.z);
		}
		this.playSound(SoundEvents.RAVAGER_ATTACK, 0.7f, 0.9f);
		MountEffects.ram(this, targets);
	}

	@Override
	public InteractionResult mobInteract(Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		boolean saddle = stack.is(MountItems.SHAMANIC_SADDLE.get());
		if (SaddleRules.canOffer(!this.isTame() && !this.isBaby(), saddle, refuseTicks)) {
			if (!this.level().isClientSide()) {
				beginTrial(player, stack);
			}
			return InteractionResult.sidedSuccess(this.level().isClientSide());
		}
		if (stack.is(MountItems.DIAMOND_APPLE.get()) && this.isTame() && this.isOwnedBy(player)) {
			boolean op = player.hasPermissions(2);
			if (!this.level().isClientSide()
					&& BreedingRules.canFeed(true, this.isBaby(), true, restTicks, true, op)
					&& offer(player)) {
				if (!player.getAbilities().instabuild) {
					stack.shrink(1);
				}
				this.playSound(SoundEvents.HORSE_EAT, 0.7f, 1.1f);
			}
			return InteractionResult.sidedSuccess(this.level().isClientSide());
		}
		if (this.isTame() && this.isOwnedBy(player) && player.isShiftKeyDown()) {
			openCustomInventoryScreen(player);
			return InteractionResult.sidedSuccess(this.level().isClientSide());
		}
		if (canBeRiddenNow() && !player.isShiftKeyDown() && this.getPassengers().size() < seatCount()) {
			if (!this.level().isClientSide()) {
				player.startRiding(this);
			}
			return InteractionResult.sidedSuccess(this.level().isClientSide());
		}
		return super.mobInteract(player, hand);
	}

	private void beginTrial(Player player, ItemStack stack) {
		this.trialTookSaddle = !player.getAbilities().instabuild;
		if (this.trialTookSaddle) {
			stack.shrink(1);
		}
		this.setSaddled(true);
		this.trial = BraceTrial.start();
		this.lastJolts = 0;
		this.setOrderedToSit(false);
		player.startRiding(this);
		this.playSound(SoundEvents.HORSE_SADDLE, 0.6f, 1.0f);
	}

	private void failTrial() {
		if (this.trial == null) {
			return;
		}
		this.trial = null;
		this.refuseTicks = BraceTrial.REFUSE_TICKS;
		this.setSaddled(false);
		this.ejectPassengers();
		if (this.trialTookSaddle) {
			this.trialTookSaddle = false;
			this.spawnAtLocation(new ItemStack(MountItems.SHAMANIC_SADDLE.get()));
		}
		this.playSound(SoundEvents.HORSE_ANGRY, 0.8f, 0.9f);
	}

	private void finishTame() {
		this.trial = null;
		Player rider = this.getControllingPassenger() instanceof Player player ? player : null;
		if (rider != null) {
			this.tame(rider);
			remember(rider, dam, sire);
		}
		this.playSound(SoundEvents.HORSE_AMBIENT, 0.8f, 1.2f);
		this.level().broadcastEntityEvent(this, (byte) 7);
		MountEffects.tamed(this);
	}

	/** @return true when this apple readied the mount or made the foal */
	private boolean offer(Player player) {
		if (offerTicks > 0 && player.getUUID().equals(offerOwner)) {
			return false;
		}
		ShamanicMount partner = findOffer(player.getUUID());
		if (partner == null) {
			offerTicks = BreedingRules.OFFER_TICKS;
			offerOwner = player.getUUID();
			offerSticky = player.hasPermissions(2);
			hearts();
			return true;
		}
		breed(player, partner);
		return true;
	}

	private ShamanicMount findOffer(UUID owner) {
		ShamanicMount nearest = null;
		double best = BreedingRules.REACH * BreedingRules.REACH;
		for (ShamanicMount other : this.level().getEntitiesOfClass(ShamanicMount.class,
				this.getBoundingBox().inflate(BreedingRules.REACH))) {
			if (other == this || other.offerTicks <= 0 || !owner.equals(other.offerOwner)) {
				continue;
			}
			double dist = this.distanceToSqr(other);
			if (BreedingRules.canPair(owner, other.offerOwner, dist) && dist < best) {
				best = dist;
				nearest = other;
			}
		}
		return nearest;
	}

	private void breed(Player player, ShamanicMount other) {
		this.offerTicks = 0;
		other.offerTicks = 0;
		this.offerOwner = null;
		other.offerOwner = null;
		this.offerSticky = false;
		other.offerSticky = false;
		this.restTicks = BreedingRules.REST_TICKS;
		other.restTicks = BreedingRules.REST_TICKS;
		if (!(this.level() instanceof ServerLevel server)) {
			return;
		}
		Genome child = Meiosis.child(this.genome, other.genome, new java.util.Random(this.random.nextLong()));
		ShamanicMount foal = MountEntities.MOUNT.get().create(server);
		if (foal == null) {
			return;
		}
		foal.setGenome(child, true);
		foal.male = this.random.nextBoolean();
		foal.setPelt(this.random.nextBoolean() ? this.pelt() : other.pelt());
		foal.moveTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), 0.0f);
		foal.setAge(-24000);
		// A foal is born wild. Once grown it takes the saddle trial like any mount, and its lineage
		// goes into the herd book when it is tamed.
		foal.dam = this.getUUID();
		foal.sire = other.getUUID();
		foal.setPersistenceRequired();
		server.addFreshEntity(foal);
		this.hearts();
		other.hearts();
		foal.hearts();
	}

	private void remember(Player player, UUID parentDam, UUID parentSire) {
		if (!(this.level() instanceof ServerLevel server)) {
			return;
		}
		MountHerdData herd = MountHerdData.get(server);
		String name = MountNames.of(phenotype, this.isBaby());
		if (this.isBaby()) {
			name = name + " " + (herd.count(player.getUUID()) + 1);
		}
		herd.adopt(this.getUUID(), player.getUUID(), name, male, genome, parentDam, parentSire, pelt());
		this.setCustomName(Component.literal(name));
		this.setCustomNameVisible(true);
	}

	private void hearts() {
		if (this.level() instanceof ServerLevel server) {
			server.sendParticles(ParticleTypes.HEART, this.getX(), this.getY() + this.getBbHeight(), this.getZ(), 7,
					0.4, 0.4, 0.4, 0.0);
		}
	}

	public void releaseIntoWorld(Player player) {
		this.setTame(false, false);
		this.setOwnerUUID(null);
		this.setOrderedToSit(false);
		this.setCustomName(null);
		this.setCustomNameVisible(false);
		for (int slot = 0; slot < tack.getContainerSize(); slot++) {
			ItemStack piece = tack.removeItemNoUpdate(slot);
			if (piece.isEmpty()) {
				continue;
			}
			if (player != null) {
				player.getInventory().add(piece);
			}
			if (!piece.isEmpty()) {
				this.spawnAtLocation(piece);
			}
		}
		Containers.dropContents(this.level(), this, this.chest);
		this.chest.clearContent();
		syncTack();
	}

	private void setSaddled(boolean saddled) {
		if (this.level().isClientSide()) {
			this.entityData.set(DATA_SADDLED, saddled);
			return;
		}
		if (saddled && tack.getItem(MountChestMenu.SADDLE_SLOT).isEmpty()) {
			tack.setItem(MountChestMenu.SADDLE_SLOT, new ItemStack(MountItems.SHAMANIC_SADDLE.get()));
		} else if (!saddled) {
			tack.setItem(MountChestMenu.SADDLE_SLOT, ItemStack.EMPTY);
		}
		syncTack();
	}

	private boolean canBeRiddenNow() {
		return SaddleRules.canRide(this.isTame(), this.saddled()) && !this.isBaby() && !this.isAway();
	}

	private int seatCount() {
		if (this.trial != null && !this.trial.finished()) {
			return 1;
		}
		return GiftRules.secondSeat(phenotype) ? 2 : 1;
	}

	@Override
	protected boolean canAddPassenger(Entity passenger) {
		if (this.trial != null && !this.trial.finished()) {
			return this.getPassengers().isEmpty();
		}
		return canBeRiddenNow() && this.getPassengers().size() < seatCount();
	}

	@Override
	protected void positionRider(Entity passenger, MoveFunction move) {
		int index = this.getPassengers().indexOf(passenger);
		float[] seat = MountSize.seat(phenotype, Math.max(0, index));
		double yaw = Math.toRadians(this.yBodyRot);
		// Body yaw, not the head: the saddle stays put when the mount looks around.
		double back = seat[0];
		double dx = -Math.sin(yaw) * -back;
		double dz = Math.cos(yaw) * -back;
		// A rider's own vehicle attachment (0.6 for a player) puts the seat under them, not under their feet.
		Vec3 sit = passenger.getVehicleAttachmentPoint(this);
		move.accept(passenger, this.getX() + dx - sit.x, this.getY() + seat[1] - sit.y, this.getZ() + dz - sit.z);
	}

	@Override
	public LivingEntity getControllingPassenger() {
		return this.getFirstPassenger() instanceof LivingEntity living ? living : null;
	}

	@Override
	public boolean canJump() {
		return this.isVehicle() && (canBeRiddenNow() || (this.trial != null && !this.trial.finished()));
	}

	/** Vanilla reports a riding jump only when the key is released; the key itself arrives by payload. */
	@Override
	public void onPlayerJump(int power) {
		hopUsed = false;
	}

	@Override
	public void handleStartJump(int power) {
		hopUsed = false;
	}

	@Override
	public void handleStopJump() {
		jumpHeld = false;
		hopUsed = false;
	}

	@Override
	public void travel(Vec3 travel) {
		if (this.trial != null && !this.trial.finished()) {
			super.travel(Vec3.ZERO);
			this.setDeltaMovement(0.0, this.getDeltaMovement().y, 0.0);
			this.climbing = false;
			syncWing();
			return;
		}
		if (this.isAlive() && this.isVehicle() && this.getControllingPassenger() instanceof Player player && canBeRiddenNow()) {
			this.setYRot(player.getYRot());
			this.yRotO = this.getYRot();
			this.setXRot(player.getXRot() * 0.5f);
			this.setRot(this.getYRot(), this.getXRot());
			float strafe = player.xxa * 0.5f;
			float forward = player.zza;
			if (forward <= 0.0f) {
				forward *= 0.25f;
			}
			air(player, forward);
			this.setSpeed((float) this.getAttributeValue(Attributes.MOVEMENT_SPEED));
			if (GiftRules.coil(phenotype) && this.isInWater()) {
				Vec3 look = this.getLookAngle();
				double rise = jumpHeld ? 0.12 : (riderSneak ? -0.08 : look.y * 0.08);
				this.setDeltaMovement(look.x * 0.28 * forward, rise, look.z * 0.28 * forward);
				this.move(net.minecraft.world.entity.MoverType.SELF, this.getDeltaMovement());
				this.resetFallDistance();
				syncWing();
				return;
			}
			super.travel(new Vec3(strafe, travel.y, forward));
			if (this.isVehicle() && GiftRules.waterWalk(phenotype) && this.isInWater()) {
				Vec3 moved = this.getDeltaMovement();
				this.setDeltaMovement(moved.x, 0.0, moved.z);
				this.setOnGround(true);
				this.resetFallDistance();
				MountEffects.waterWalk(this);
			}
			syncWing();
			return;
		}
		super.travel(travel);
		syncWing();
	}

	private void syncWing() {
		boolean winged = this.phenotype.chimera || this.phenotype.wings != Phenotype.WingShow.NONE;
		int pose = 0;
		if (winged && !this.onGround() && !this.isInWater()) {
			pose = this.entityData.get(DATA_CLIMBING) || this.getDeltaMovement().y > 0.06 ? 2 : 1;
		}
		byte packed = (byte) pose;
		if (this.entityData.get(DATA_WING) != packed) {
			this.entityData.set(DATA_WING, packed);
		}
	}

	/**
	 * Airborne motion. This runs where the mount is moved, which for a ridden mount is the rider's
	 * client, so the climb is read from the server's flag and the keys from the local copy.
	 */
	private void air(Player player, float forward) {
		boolean climb = this.entityData.get(DATA_CLIMBING);
		climbing = climb;
		if (climb) {
			Vec3 look = this.getLookAngle();
			double up = 0.35;
			this.setDeltaMovement(look.x * 0.6, up, look.z * 0.6);
			this.resetFallDistance();
			return;
		}
		if (GiftRules.glide(phenotype) && jumpHeld && !this.onGround()) {
			Vec3 look = this.getLookAngle();
			this.setDeltaMovement(look.x * 0.5, -0.04, look.z * 0.5);
			this.resetFallDistance();
			return;
		}
		if (jumpHeld && this.onGround() && !hopUsed && forward > 0.0f) {
			this.setDeltaMovement(this.getDeltaMovement().add(0.0, 0.55, 0.0));
			hopUsed = true;
		}
		if (phenotype.gifts.contains(tk.darrow.shamanicmounts.genome.Marks.Gift.DREAM) && this.isVehicle()) {
			this.resetFallDistance();
		}
	}

	@Override
	public boolean canStandOnFluid(net.minecraft.world.level.material.FluidState state) {
		return this.isVehicle() && GiftRules.waterWalk(phenotype) && state.is(net.minecraft.tags.FluidTags.WATER);
	}

	@Override
	public boolean causeFallDamage(float distance, float multiplier, DamageSource source) {
		if (this.isVehicle() && phenotype.gifts.contains(tk.darrow.shamanicmounts.genome.Marks.Gift.DREAM)) {
			return false;
		}
		return super.causeFallDamage(distance, multiplier, source);
	}

	@Override
	public void removePassenger(Entity passenger) {
		super.removePassenger(passenger);
		if (this.trial != null && !this.trial.finished()) {
			failTrial();
		} else if (!this.level().isClientSide() && GiftRules.guard(phenotype) && this.isTame()) {
			setMode(MountMode.STAY);
		}
	}

	@Override
	public boolean hurt(DamageSource source, float amount) {
		if (this.trial != null && !this.trial.finished()) {
			boolean hit = super.hurt(source, amount);
			failTrial();
			return hit;
		}
		if (this.isVehicle() && GiftRules.sneakHide(phenotype)) {
			reveal();
		}
		return super.hurt(source, amount);
	}

	/** The saddle bags, with the mount's portrait and its follow, stay, or wander choice. */
	@Override
	public void openCustomInventoryScreen(Player player) {
		if (!this.isTame() || !this.isOwnedBy(player) || !(player instanceof ServerPlayer server)) {
			return;
		}
		int rows = MountChestMenu.rowsFor(this);
		server.openMenu(new SimpleMenuProvider((id, inventory, who) -> new MountChestMenu(id, inventory, this.tack, this.chest,
				this, rows), this.getDisplayName()), buffer -> {
					buffer.writeVarInt(this.getId());
					buffer.writeVarInt(rows);
				});
	}

	@Override
	public boolean isFood(ItemStack stack) {
		return false;
	}

	@Override
	public AgeableMob getBreedOffspring(ServerLevel level, AgeableMob other) {
		return null;
	}

	@Override
	public boolean removeWhenFarAway(double distance) {
		return !this.isTame();
	}

	@Override
	protected void dropCustomDeathLoot(ServerLevel level, DamageSource source, boolean recentlyHit) {
		super.dropCustomDeathLoot(level, source, recentlyHit);
		Containers.dropContents(level, this, this.chest);
		Containers.dropContents(level, this, this.tack);
	}

	@Override
	public void die(DamageSource source) {
		if (!this.level().isClientSide() && this.level() instanceof ServerLevel server && this.isTame()) {
			MountHerdData.get(server).release(this.getOwnerUUID(), this.getUUID());
		}
		super.die(source);
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.put("Genome", GenomeIO.write(genome));
		tag.putBoolean("GenomeLocked", genomeLocked);
		tag.putBoolean("Male", male);
		tag.putBoolean("Saddled", this.saddled());
		tag.putInt("Refuse", refuseTicks);
		tag.putInt("Rest", restTicks);
		tag.putInt("Stamina", this.stamina());
		tag.putBoolean("ClimbSpent", climbSpent);
		tag.putInt("Drum", drumCooldown);
		tag.putInt("Ram", ramCooldown);
		tag.putInt("Maul", maulCooldown);
		tag.putInt("Coil", coilCooldown);
		tag.putInt("Away", awayTicks);
		tag.putInt("AwayCd", awayCooldown);
		tag.putInt("Blink", blinkCooldown);
		tag.putInt("Reveal", revealTicks);
		tag.putBoolean("Hidden", hidden);
		tag.putString("Mode", mode().name());
		tag.putInt("Pelt", pelt());
		tag.putByte("RiderKeys", (byte) ((riderSneak ? 1 : 0) | (jumpHeld ? 2 : 0)));
		tag.putInt("SneakHeld", sneakHeld);
		tag.put("Chest", this.chest.createTag(this.registryAccess()));
		net.minecraft.nbt.ListTag tackList = new net.minecraft.nbt.ListTag();
		for (int slot = 0; slot < this.tack.getContainerSize(); slot++) {
			ItemStack piece = this.tack.getItem(slot);
			if (!piece.isEmpty()) {
				CompoundTag one = new CompoundTag();
				one.putByte("Slot", (byte) slot);
				tackList.add(piece.save(this.registryAccess(), one));
			}
		}
		tag.put("Tack", tackList);
		if (dam != null) {
			tag.putUUID("Dam", dam);
		}
		if (sire != null) {
			tag.putUUID("Sire", sire);
		}
	}

	@Override
	public void readAdditionalSaveData(CompoundTag tag) {
		super.readAdditionalSaveData(tag);
		if (tag.contains("Genome")) {
			try {
				setGenome(GenomeIO.read(tag.getCompound("Genome")), tag.getBoolean("GenomeLocked"));
			} catch (RuntimeException ignored) {
				setGenome(Founders.eightfold(), false);
			}
		}
		this.male = tag.getBoolean("Male");
		if (tag.getBoolean("Saddled")) {
			this.setSaddled(true);
		}
		this.refuseTicks = tag.getInt("Refuse");
		this.restTicks = tag.getInt("Rest");
		this.entityData.set(DATA_STAMINA, tag.contains("Stamina") ? tag.getInt("Stamina") : GiftRules.CLIMB_TICKS);
		this.climbSpent = tag.getBoolean("ClimbSpent");
		this.drumCooldown = tag.getInt("Drum");
		this.ramCooldown = tag.getInt("Ram");
		this.maulCooldown = tag.getInt("Maul");
		this.coilCooldown = tag.getInt("Coil");
		this.awayTicks = tag.getInt("Away");
		this.awayCooldown = tag.getInt("AwayCd");
		this.blinkCooldown = tag.getInt("Blink");
		this.revealTicks = tag.getInt("Reveal");
		if (tag.contains("Pelt")) {
			setPelt(tag.getInt("Pelt"));
		}
		if (tag.contains("Mode")) {
			try {
				setMode(MountMode.valueOf(tag.getString("Mode")));
			} catch (IllegalArgumentException ignored) {
				setMode(MountMode.WANDER);
			}
		} else if (this.isOrderedToSit()) {
			setMode(MountMode.STAY);
		}
		this.chest.fromTag(tag.getList("Chest", net.minecraft.nbt.Tag.TAG_COMPOUND), this.registryAccess());
		if (tag.contains("Tack")) {
			this.tack.clearContent();
			for (net.minecraft.nbt.Tag raw : tag.getList("Tack", net.minecraft.nbt.Tag.TAG_COMPOUND)) {
				CompoundTag one = (CompoundTag) raw;
				int slot = one.getByte("Slot");
				if (slot >= 0 && slot < this.tack.getContainerSize()) {
					this.tack.setItem(slot, ItemStack.parse(this.registryAccess(), one).orElse(ItemStack.EMPTY));
				}
			}
		}
		syncTack();
		this.dam = tag.hasUUID("Dam") ? tag.getUUID("Dam") : null;
		this.sire = tag.hasUUID("Sire") ? tag.getUUID("Sire") : null;
		if (awayTicks > 0) {
			this.setInvisible(true);
			this.noPhysics = true;
		}
	}

	@Override
	protected net.minecraft.sounds.SoundEvent getAmbientSound() {
		return SoundEvents.HORSE_AMBIENT;
	}

	@Override
	protected net.minecraft.sounds.SoundEvent getHurtSound(DamageSource source) {
		return SoundEvents.HORSE_HURT;
	}

	@Override
	protected net.minecraft.sounds.SoundEvent getDeathSound() {
		return SoundEvents.HORSE_DEATH;
	}

}
