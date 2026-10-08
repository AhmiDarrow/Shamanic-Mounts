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
import net.minecraft.world.item.Items;
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
import tk.darrow.shamanicmounts.genome.Marks;
import tk.darrow.shamanicmounts.genome.GenomeIO;
import tk.darrow.shamanicmounts.genome.Meiosis;
import tk.darrow.shamanicmounts.genome.MountSize;
import tk.darrow.shamanicmounts.genome.Phenotype;
import tk.darrow.shamanicmounts.item.MountItems;
import tk.darrow.shamanicmounts.ride.BreedingRules;
import tk.darrow.shamanicmounts.ride.FollowRules;
import tk.darrow.shamanicmounts.ride.GiftRules;
import tk.darrow.shamanicmounts.ride.MountNames;
import tk.darrow.shamanicmounts.ride.MountStats;
import tk.darrow.shamanicmounts.trade.MountPosts;
import tk.darrow.shamanicmounts.world.MountCall;
import tk.darrow.shamanicmounts.tack.SaddleRules;
import tk.darrow.shamanicmounts.tame.ReinTrial;

/**
 * One shamanic mount. Wild adults attack until a golden apple calms them. The saddle goes on only then, and the tame
 * is a thirty-second rein trial once the rider is seated: the mount steps the opposite way from the asked direction.
 * Foals are born wild and take that trial once grown. Riding needs the shamanic saddle. A Diamond Apple breeds two
 * tames the same player owns.
 */
public class ShamanicMount extends TamableAnimal implements PlayerRideableJumping, HasCustomInventoryScreen {
	private static final EntityDataAccessor<CompoundTag> DATA_GENOME = SynchedEntityData.defineId(ShamanicMount.class,
			EntityDataSerializers.COMPOUND_TAG);
	private static final EntityDataAccessor<Boolean> DATA_SADDLED = SynchedEntityData.defineId(ShamanicMount.class,
			EntityDataSerializers.BOOLEAN);
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
	/** Saddle bags are strapped on. */
	private static final EntityDataAccessor<Boolean> DATA_BAGS = SynchedEntityData.defineId(ShamanicMount.class,
			EntityDataSerializers.BOOLEAN);
	/** Follow, stay, or wander, as {@link MountMode} ordinals. */
	private static final EntityDataAccessor<Byte> DATA_MODE = SynchedEntityData.defineId(ShamanicMount.class,
			EntityDataSerializers.BYTE);
	/** A golden apple is still keeping this wild adult from attacking. The timer itself stays on the server. */
	private static final EntityDataAccessor<Boolean> DATA_CALM = SynchedEntityData.defineId(ShamanicMount.class,
			EntityDataSerializers.BOOLEAN);
	/** The rein prompt: 0 none, 1 left, 2 right, 3 forward, 4 back. Both sides move from this. */
	private static final EntityDataAccessor<Byte> DATA_CUE = SynchedEntityData.defineId(ShamanicMount.class,
			EntityDataSerializers.BYTE);
	/** Bred body stats, 1 to 100. The default is the unsaved middle, a constant, not an instance field. */
	private static final EntityDataAccessor<Integer> DATA_STAT_HEALTH = SynchedEntityData.defineId(ShamanicMount.class,
			EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_STAT_SPEED = SynchedEntityData.defineId(ShamanicMount.class,
			EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_STAT_JUMP = SynchedEntityData.defineId(ShamanicMount.class,
			EntityDataSerializers.INT);
	private static final EntityDataAccessor<Integer> DATA_STAT_STAMINA = SynchedEntityData.defineId(ShamanicMount.class,
			EntityDataSerializers.INT);
	/** Gallop is on for this tick. The server decides; the rider's client reads it in travel. */
	private static final EntityDataAccessor<Boolean> DATA_GALLOP = SynchedEntityData.defineId(ShamanicMount.class,
			EntityDataSerializers.BOOLEAN);
	/** How far a foal has grown, 0 to GROWTH_STEPS. The server decides; the client's age is not synced. */
	private static final EntityDataAccessor<Integer> DATA_GROWTH = SynchedEntityData.defineId(ShamanicMount.class,
			EntityDataSerializers.INT);

	/**
	 * What a mount is before its own genome is set: built once and shared, since every spawn attempt and every
	 * mount loaded from disk constructs one and replaces it straight away. Genomes and phenotypes are never changed.
	 */
	private static final Genome DEFAULT_GENOME = Founders.eightfold();
	private static final Phenotype DEFAULT_PHENOTYPE = Expression.express(DEFAULT_GENOME);
	private static final CompoundTag DEFAULT_GENOME_TAG = GenomeIO.write(DEFAULT_GENOME);

	private Genome genome = DEFAULT_GENOME;
	private Phenotype phenotype = DEFAULT_PHENOTYPE;
	private boolean genomeLocked;
	private boolean male = true;
	private final SimpleContainer chest = new SimpleContainer(GiftRules.CHEST_SLOTS);
	/** The saddle and the saddle bags, as items, so the mount screen can take them on and off. */
	private final SimpleContainer tack = new SimpleContainer(3);
	private ReinTrial.Trial trial;
	private int refuseTicks;
	private int restTicks;
	private int offerTicks;
	private UUID offerOwner;
	/** An operator's offer stays open until the pair is made. */
	private boolean offerSticky;
	private boolean jumpHeld;
	private boolean hopUsed;
	private boolean climbSpent;
	/** The climb bar. Only the server reads it, so it is not synced to clients. */
	private int stamina = GiftRules.CLIMB_TICKS;
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
	/** The last player to step off, and the game tick they did, so a teleport that unseated them can seat them again. */
	private UUID lastRider;
	private long lastRiderTick = -1;
	private boolean trialTookSaddle;
	/** A paid saddle the save caught mid rein-trial: handed back on the first tick, as a failed try would. */
	private ItemStack pendingTrialSaddle = ItemStack.EMPTY;
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
	/** Ticks of calm left. Clients only hear {@link #DATA_CALM}. */
	private int calmTicks;
	/** Direction keys the rider is holding, as a {@link ReinTrial} mask. */
	private int reinMask;
	/** Set once a wild roll, a foal, or a load has written the four bred stats. */
	private boolean statsRolled;
	/** True after attributes have been applied, so a later apply does not heal a wound. */
	private boolean bodyApplied;
	/** Current gallop stamina. Not the bred stat. The bred stat is the capacity. */
	private int gallopStamina = MountStats.capacityTicks(MountStats.MISSING);
	private int gallopRegen;
	/** Empty bar: the bonus stays off until stamina regens to 20% of capacity. */
	private boolean gallopLocked;
	/** Gallop is the sprint key, reported with sneak and jump. */
	private boolean riderSprint;
	/** The prompt last seen, and the tick it changed, so the buck starts with the new direction. */
	private byte cueCodeSeen;
	private int cueSeenTick;

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

	/** Which of the line's three coats this mount wears, 0 to 2. The pelt gene decides it. */
	public int pelt() {
		return phenotype.pelt;
	}


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
		builder.define(DATA_GENOME, DEFAULT_GENOME_TAG);
		builder.define(DATA_SADDLED, false);
		builder.define(DATA_WING, (byte) 0);
		builder.define(DATA_CLIMBING, false);
		builder.define(DATA_MODE, (byte) FollowRules.DEFAULT.ordinal());
		builder.define(DATA_BAGS, false);
		builder.define(DATA_ARMOR, (byte) 0);
		builder.define(DATA_CALM, false);
		builder.define(DATA_CUE, (byte) 0);
		builder.define(DATA_STAT_HEALTH, MountStats.MISSING);
		builder.define(DATA_STAT_SPEED, MountStats.MISSING);
		builder.define(DATA_STAT_JUMP, MountStats.MISSING);
		builder.define(DATA_STAT_STAMINA, MountStats.MISSING);
		builder.define(DATA_GALLOP, false);
		builder.define(DATA_GROWTH, BreedingRules.GROWTH_STEPS);
	}

	/** Every age change passes here, ticking up included, so the growth step follows it on the server. */
	@Override
	public void setAge(int age) {
		super.setAge(age);
		if (!this.level().isClientSide()) {
			this.entityData.set(DATA_GROWTH, BreedingRules.growthStep(age));
		}
	}

	/** A foal is born small and grows to its full size in steps. */
	@Override
	public float getAgeScale() {
		return BreedingRules.growthScale(this.entityData.get(DATA_GROWTH));
	}

	@Override
	protected void registerGoals() {
		this.goalSelector.addGoal(0, new FloatGoal(this));
		this.goalSelector.addGoal(1, new MeleeAttackGoal(this, 1.2, true));
		this.goalSelector.addGoal(2, new SitWhenOrderedToGoal(this));
		// Vanilla's follow already stops for a sit or a leash, and teleports only onto walkable ground the
		// whole body fits on, never into water, lava, or leaves. A ridden mount goes where its rider says.
		this.goalSelector.addGoal(3, new FollowOwnerGoal(this, 1.1, 8.0f, 2.0f) {
			@Override
			public boolean canUse() {
				return following() && super.canUse();
			}

			@Override
			public boolean canContinueToUse() {
				return following() && super.canContinueToUse();
			}
		});
		this.goalSelector.addGoal(4, new PanicGoal(this, 1.3) {
			@Override
			public boolean canUse() {
				return !huntsPlayers() && super.canUse();
			}
		});
		// A wild foal keeps to the grown mounts of its kind until it is grown itself.
		this.goalSelector.addGoal(5, new net.minecraft.world.entity.ai.goal.FollowParentGoal(this, 1.1));
		this.goalSelector.addGoal(5, new WaterAvoidingRandomStrollGoal(this, 1.0) {
			@Override
			public boolean canUse() {
				return (trial == null || trial.finished()) && (!isTame() || mode() == MountMode.WANDER) && !tended()
						&& super.canUse();
			}

			@Override
			public boolean canContinueToUse() {
				return (trial == null || trial.finished()) && !tended() && super.canContinueToUse();
			}
		});
		this.goalSelector.addGoal(6, new LookAtPlayerGoal(this, Player.class, 8.0f));
		this.goalSelector.addGoal(7, new RandomLookAroundGoal(this));
		this.targetSelector.addGoal(1, new OwnerHurtByTargetGoal(this));
		this.targetSelector.addGoal(2, new HurtByTargetGoal(this));
		this.targetSelector.addGoal(3, new PlayerHuntGoal(this));
	}

	/** A wild adult with nothing calming it, and nobody in the middle of taming it. Foals stay peaceful. */
	public boolean huntsPlayers() {
		return SaddleRules.hostile(this.isTame(), this.isBaby(), this.calmTicks, this.trial != null && !this.trial.finished());
	}

	/**
	 * A calmed wild mount, a foal, and a mount in the rein trial do not bite a player. A tame still may:
	 * it defends its owner.
	 */
	@Override
	public void setTarget(@Nullable LivingEntity target) {
		if (target instanceof Player && !this.isTame() && !this.huntsPlayers()) {
			target = null;
		}
		if (this.herdmate(target)) {
			target = null;
		}
		super.setTarget(target);
	}

	/** Another tame mount, when this one is tame too: never a target, never hurt by this one. */
	public boolean herdmate(@Nullable Entity other) {
		return other != this && other instanceof ShamanicMount mount
				&& SaddleRules.herdmates(this.isTame(), mount.isTame());
	}

	@Override
	public boolean canAttack(LivingEntity target) {
		return !this.herdmate(target) && super.canAttack(target);
	}

	/** Direction keys from the rider's client. Scoring stays here. */
	public void reinKeys(Player player, int mask) {
		if (player != this.getControllingPassenger()) {
			return;
		}
		this.reinMask = mask & 15;
	}

	/** The prompt both sides are showing. Null when the trial is not asking for anything. */
	public ReinTrial.Dir shownCue() {
		return ReinTrial.Dir.fromCode(this.entityData.get(DATA_CUE));
	}

	/**
	 * The rider's box, if the mount were at {@code x,y,z}. Riding turns the player's own collision off, and the
	 * seat puts their head above the hitbox, so this is the box that has to stay out of the tree.
	 */
	private boolean riderClear(Player player, double x, double y, double z) {
		float[] seat = MountSize.seat(this.phenotype, 0);
		double yaw = Math.toRadians(this.yBodyRot);
		double dx = Math.sin(yaw) * seat[0];
		double dz = -Math.cos(yaw) * seat[0];
		Vec3 sit = player.getVehicleAttachmentPoint(this);
		AABB box = player.getDimensions(player.getPose()).makeBoundingBox(x + dx - sit.x, y + seat[1] - sit.y, z + dz - sit.z)
				.deflate(0.05, 0.0, 0.05);
		return this.level().noCollision(player, box);
	}

	/** Camera-relative steer turned into world XZ. Positive strafe is left, positive forward is forward. */
	private static Vec3 steerWorld(float strafe, float forward, float yawDeg) {
		double yaw = Math.toRadians(yawDeg);
		double sin = Math.sin(yaw);
		double cos = Math.cos(yaw);
		return new Vec3(strafe * cos - forward * sin, 0.0, forward * cos + strafe * sin);
	}

	/** Room for the rider a short way along this steer input, raised by {@code lift}. */
	private boolean riderAhead(Player player, float strafe, float forward, float yaw, double lift) {
		Vec3 world = steerWorld(strafe, forward, yaw);
		double scale = Math.sqrt(world.x * world.x + world.z * world.z);
		if (scale < 1.0e-4) {
			return this.riderClear(player, this.getX(), this.getY() + lift, this.getZ());
		}
		double dx = world.x / scale;
		double dz = world.z / scale;
		for (double dist = 0.4; dist <= 0.81; dist += 0.4) {
			if (!this.riderClear(player, this.getX() + dx * dist, this.getY() + lift, this.getZ() + dz * dist)) {
				return false;
			}
		}
		return true;
	}

	/** Ticks since this prompt appeared. Each side counts from when it first saw the synced cue. */
	private int cueAge() {
		byte code = this.entityData.get(DATA_CUE);
		if (code != this.cueCodeSeen) {
			this.cueCodeSeen = code;
			this.cueSeenTick = this.tickCount;
		}
		return this.tickCount - this.cueSeenTick;
	}

	/** The owner as last found in this level; see {@link #getOwner()}. */
	@Nullable
	private LivingEntity ownerSeen;

	/**
	 * The owner, if they are in this level. Vanilla finds them by walking the level's player list on every call, and the
	 * follow goal, the sit and target goals, and the rider checks each ask several times a tick. The player found last
	 * is handed back while it is still that player and still in this level, which is exactly when the walk would find
	 * it again; anything else (gone, dead and respawned, another dimension, a new owner) walks the list as before.
	 */
	@Override
	@Nullable
	public LivingEntity getOwner() {
		UUID id = this.getOwnerUUID();
		LivingEntity seen = ownerSeen;
		if (id != null && seen != null && !seen.isRemoved() && seen.level() == this.level() && id.equals(seen.getUUID())) {
			return seen;
		}
		LivingEntity found = super.getOwner();
		ownerSeen = found;
		return found;
	}

	/** How many players have this mount's screen open. While any do, it stands still for them. */
	private int tending;

	public void startTending() {
		tending++;
		this.getNavigation().stop();
	}

	public void stopTending() {
		tending = Math.max(0, tending - 1);
	}

	public boolean tended() {
		return tending > 0;
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
		return stamina;
	}

	public SimpleContainer chest() {
		return chest;
	}

	public MountMode mode() {
		return MountMode.of(this.entityData.get(DATA_MODE));
	}

	/** On follow and free to: not ridden, not sent away, and its screen not open. */
	private boolean following() {
		return mode() == MountMode.FOLLOW && !tended() && !this.isVehicle() && !isAway();
	}

	/**
	 * Whether this mount goes along when {@code owner} leaves the dimension from where they stand now:
	 * a following tame of theirs, near, not leashed, and with nobody else on it.
	 */
	public boolean crossesWith(Player owner) {
		boolean otherRider = false;
		for (Entity passenger : this.getPassengers()) {
			otherRider |= passenger != owner;
		}
		return this.isAlive() && !this.isRemoved() && this.trial == null
				&& FollowRules.crosses(this.isTame(), owner.getUUID().equals(this.getOwnerUUID()), mode(),
						this.isOrderedToSit(), this.isLeashed(), this.isPassenger(), otherRider, isAway(),
						this.distanceToSqr(owner));
	}

	/** Whether {@code player} was in the saddle when they left this level at {@code leftTick}. */
	public boolean riddenBy(Player player, long leftTick) {
		return this.hasPassenger(player)
				|| player.getUUID().equals(lastRider) && FollowRules.wasRiding(lastRiderTick, leftTick);
	}

	/** Smoke and a soft teleport where a following mount comes out beside its owner in another dimension. */
	public void arrivedBeside() {
		MountEffects.slip(this);
	}

	/** Follow, stay, or wander. Stay is the sit; the other two stand it back up. */
	public void setMode(MountMode mode) {
		this.entityData.set(DATA_MODE, (byte) mode.ordinal());
		if (!this.level().isClientSide()) {
			this.setOrderedToSit(mode == MountMode.STAY);
			// The flute reads the order from the herd data, so a parked mount is not loaded just to be left.
			noteWhere();
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
		if (this.bodyApplied) {
			// Build and size set the body's health, so a new genome moves it.
			applyBody(false);
		}
		this.refreshDimensions();
	}

	/** Build sets the base; size within the line moves it a little, an XL about a seventh more. */
	private double bodyHealth() {
		return Math.round(switch (phenotype.scale) {
			case SLIGHT -> 22.0;
			case NORMAL -> 26.0;
			case LARGE -> 32.0;
			case GREATER -> 40.0;
		} * phenotype.sizeFactor);
	}

	public MountStats bodyStats() {
		return new MountStats(this.entityData.get(DATA_STAT_HEALTH), this.entityData.get(DATA_STAT_SPEED),
				this.entityData.get(DATA_STAT_JUMP), this.entityData.get(DATA_STAT_STAMINA));
	}

	/** The one place bred stats are written and pushed onto attributes. */
	public void assignStats(MountStats stats, boolean fillGallop) {
		this.statsRolled = true;
		this.entityData.set(DATA_STAT_HEALTH, stats.health());
		this.entityData.set(DATA_STAT_SPEED, stats.speed());
		this.entityData.set(DATA_STAT_JUMP, stats.jump());
		this.entityData.set(DATA_STAT_STAMINA, stats.stamina());
		if (fillGallop) {
			this.gallopStamina = MountStats.capacityTicks(stats.stamina());
			this.gallopLocked = false;
			this.gallopRegen = 0;
		}
		applyBody(!this.bodyApplied);
	}

	/**
	 * Health and speed, once. A full bar or the first apply snaps to the new max.
	 * A wounded mount keeps its health unless it is above the new max.
	 */
	private void applyBody(boolean firstApply) {
		double newMax = MountStats.maxHealth(bodyHealth(), this.entityData.get(DATA_STAT_HEALTH));
		float oldMax = this.getMaxHealth();
		float oldHealth = this.getHealth();
		var health = this.getAttribute(Attributes.MAX_HEALTH);
		if (health != null) {
			health.setBaseValue(newMax);
		}
		if (firstApply || oldHealth >= oldMax - 0.05f) {
			this.setHealth((float) newMax);
		} else if (oldHealth > newMax) {
			this.setHealth((float) newMax);
		}
		var speed = this.getAttribute(Attributes.MOVEMENT_SPEED);
		if (speed != null) {
			speed.setBaseValue(MountStats.moveSpeed(this.entityData.get(DATA_STAT_SPEED)));
		}
		this.bodyApplied = true;
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
		if (DATA_GROWTH.equals(key)) {
			this.refreshDimensions();
		}
	}

	/**
	 * The box the camera tests before drawing: the hitbox grown by the neck, the tail, and the spread
	 * wings, so a vast-winged roc or a long-necked crane never pops in at the edge of the screen.
	 */
	@Override
	public net.minecraft.world.phys.AABB getBoundingBoxForCulling() {
		float scale = phenotype.uniformScale;
		boolean winged = phenotype.chimera || phenotype.wings != Phenotype.WingShow.NONE;
		double side = (winged ? 1.4 * Math.max(1.0f, phenotype.wingScale) : 0.6) * scale;
		return this.getBoundingBox().inflate(side + 1.0 * scale, 1.2 * scale, side + 1.0 * scale);
	}

	@Override
	protected EntityDimensions getDefaultDimensions(Pose pose) {
		float grown = this.getAgeScale();
		return EntityDimensions.scalable(MountSize.width(phenotype) * grown, MountSize.height(phenotype) * grown);
	}

	@Override
	public SpawnGroupData finalizeSpawn(ServerLevelAccessor level, net.minecraft.world.DifficultyInstance difficulty,
			MobSpawnType reason, SpawnGroupData data) {
		if (!genomeLocked) {
			// A line that lives in this biome, in the pelt at home here, and a size the climate leans toward.
			var biome = level.getBiome(this.blockPosition());
			var line = tk.darrow.shamanicmounts.world.MountBiomes.pick(biome, this.random);
			setGenome(tk.darrow.shamanicmounts.world.MountBiomes.wild(line.founder(), line, biome, this.random), false);
			this.male = this.random.nextBoolean();
		}
		if (!this.statsRolled) {
			assignStats(MountStats.wildRoll(new java.util.Random(this.random.nextLong())), true);
		}
		return super.finalizeSpawn(level, difficulty, reason, data);
	}

	@Override
	public void tick() {
		if (!this.level().isClientSide()) {
			tickGallop();
		}
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
		if (!pendingTrialSaddle.isEmpty()) {
			this.spawnAtLocation(pendingTrialSaddle);
			pendingTrialSaddle = ItemStack.EMPTY;
		}
		if (calmTicks > 0 && --calmTicks == 0) {
			this.entityData.set(DATA_CALM, false);
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
		syncWing();
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
		this.getNavigation().stop();
		boolean riding = this.getControllingPassenger() instanceof Player;
		ReinTrial.tick(trial, ReinTrial.Press.of(reinMask), !riding);
		if (trial.caughtBeat()) {
			MountEffects.caught(this);
		} else if (trial.missedBeat() && !trial.failed()) {
			MountEffects.missed(this);
		}
		if (trial.failed()) {
			failTrial();
			return;
		}
		if (trial.done()) {
			finishTame();
			return;
		}
		showCue(trial.cue());
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

	/** The rider's keys, from the client, on both sides. A fresh jump press also allows one more hop. Gallop is the sprint key. */
	public void riderKeys(Player player, boolean sneak, boolean jump, boolean sprint) {
		if (player != this.getControllingPassenger()) {
			return;
		}
		riderSneak = sneak;
		riderSprint = sprint;
		if (jump && !jumpHeld) {
			hopUsed = false;
		}
		jumpHeld = jump;
	}

	/** A rein trial is still running, so this mount cannot be traded or galloped. */
	public boolean inReinTrial() {
		return this.trial != null && !this.trial.finished();
	}

	/**
	 * Gallop is the sprint key. Drain only while sprinting, not in a rein trial, and not on an empty bar.
	 * Regen is one point per two ticks. At empty the bonus stays off until 20% is back.
	 */
	private void tickGallop() {
		boolean trial = this.trial != null && !this.trial.finished();
		int staminaStat = this.entityData.get(DATA_STAT_STAMINA);
		int capacity = MountStats.capacityTicks(staminaStat);
		if (this.gallopStamina > capacity) {
			this.gallopStamina = capacity;
		}
		if (this.gallopLocked && this.gallopStamina >= MountStats.gallopFloor(staminaStat)) {
			this.gallopLocked = false;
		}
		boolean ridden = this.getControllingPassenger() instanceof Player;
		boolean gallop = ridden && this.riderSprint && !trial && this.gallopStamina > 0 && !this.gallopLocked;
		if (gallop) {
			this.gallopStamina--;
			this.gallopRegen = 0;
			if (this.gallopStamina <= 0) {
				this.gallopStamina = 0;
				this.gallopLocked = true;
			}
		} else if (!trial && this.gallopStamina < capacity) {
			this.gallopRegen++;
			if (this.gallopRegen >= 2) {
				this.gallopRegen = 0;
				this.gallopStamina++;
			}
		}
		if (this.entityData.get(DATA_GALLOP) != gallop) {
			this.entityData.set(DATA_GALLOP, gallop);
		}
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
			riderSprint = false;
			jumpHeld = false;
			reinMask = 0;
			return;
		}
		// A wild mount under a rein trial gives nothing: no drum, no sight, no strike.
		if (!this.isTame() || this.trial != null) {
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
			// Night vision with 10 seconds or less left dims and pulses, so the ride keeps it above that.
			keep(player, net.minecraft.world.effect.MobEffects.NIGHT_VISION, NIGHT_VISION_TICKS, NIGHT_VISION_TICKS - 40);
		}
		if (GiftRules.pinion(phenotype)) {
			keep(player, net.minecraft.world.effect.MobEffects.SLOW_FALLING, 10, 5);
			keep(this, net.minecraft.world.effect.MobEffects.SLOW_FALLING, 10, 5);
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
			keep(player, net.minecraft.world.effect.MobEffects.WATER_BREATHING, 40, 20);
		}
		if (GiftRules.thickHide(genome)) {
			keep(player, net.minecraft.world.effect.MobEffects.DAMAGE_RESISTANCE, 40, 20);
		}
		if (GiftRules.shadowSpeed(phenotype) && this.level().getMaxLocalRawBrightness(this.blockPosition()) < 7) {
			keep(player, net.minecraft.world.effect.MobEffects.MOVEMENT_SPEED, 40, 20);
		}
		boolean hide = GiftRules.sneakHide(phenotype) && riderSneak && revealTicks <= 0;
		if (hide) {
			// While hidden, no mob near the rider can take up the rider or the mount as a target (see hidesFrom), so
			// the sweep is needed only when the hide starts and for a mob that walks in already set on them.
			if (!hidden || this.tickCount % QUIET_EVERY == 0) {
				quietNearby(player);
			}
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
					shine(mob);
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

	/** How long the omen's glow lasts on a monster. The scan runs every ten ticks. */
	private static final int GLOW_TICKS = 40;
	/** A glow of the omen's own with more than this left is not renewed: it outlasts the next two scans. */
	private static final int GLOW_RENEW_AT = 20;
	/** Ticks between sweeps for mobs set on a hidden rider. The target event covers the ticks between. */
	private static final int QUIET_EVERY = 4;

	/**
	 * Lights a monster for the omen. Renewing every scan would fire the effect events and rework the monster's effects
	 * each time for nothing, so the omen's own glow is topped up only once it is down to {@link #GLOW_RENEW_AT}; it never
	 * runs out while the monster stays in range. Any other glow (another source's, a stronger or endless one) is met
	 * exactly as before.
	 */
	private static void shine(Mob mob) {
		net.minecraft.world.effect.MobEffectInstance now = mob.getEffect(net.minecraft.world.effect.MobEffects.GLOWING);
		if (now != null && now.getAmplifier() == 0 && now.isAmbient() && !now.isVisible() && !now.showIcon()
				&& !now.isInfiniteDuration() && now.getDuration() > GLOW_RENEW_AT) {
			return;
		}
		mob.addEffect(new net.minecraft.world.effect.MobEffectInstance(
				net.minecraft.world.effect.MobEffects.GLOWING, GLOW_TICKS, 0, true, false, false));
	}

	/**
	 * Whether {@code seeker} must not take up {@code target} because this mount's veil is closed: the target is the
	 * steering rider or the mount, the hide holds right now, and the seeker is a mob within the reach of the sweep.
	 */
	public boolean hidesFrom(LivingEntity seeker, LivingEntity target) {
		if (!hidden || !riderSneak || revealTicks > 0 || this.level().isClientSide() || !(seeker instanceof Mob)
				|| !(this.getControllingPassenger() instanceof Player rider) || (target != rider && target != this)) {
			return false;
		}
		return seeker.getBoundingBox().intersects(rider.getBoundingBox().inflate(QUIET_REACH));
	}

	/** How long the drum's night vision runs. Over ten seconds, or the screen dims and pulses. */
	private static final int NIGHT_VISION_TICKS = 260;

	/**
	 * Keeps a riding effect on without renewing it every tick: each renewal sends the rider an effect
	 * packet and fires the effect events. It is topped up once {@code renewAt} ticks or fewer are left.
	 * A stronger or endless copy the rider already has is left alone.
	 */
	private static void keep(LivingEntity who, net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect,
			int ticks, int renewAt) {
		net.minecraft.world.effect.MobEffectInstance now = who.getEffect(effect);
		if (now == null || (now.getAmplifier() == 0 && !now.isInfiniteDuration() && now.getDuration() <= renewAt)) {
			who.addEffect(new net.minecraft.world.effect.MobEffectInstance(effect, ticks, 0, true, false, true));
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
			this.stamina = next;
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
				this.stamina = stamina + 1;
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

	/** How far around a hidden rider mobs lose them. */
	private static final double QUIET_REACH = 24.0;

	private void quietNearby(Player player) {
		AABB box = player.getBoundingBox().inflate(QUIET_REACH);
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
		if (this.level().isClientSide() || player != this.getControllingPassenger() || !this.isTame() || this.trial != null) {
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
		// The whole mount, and its rider above it, must fit at the far end.
		net.minecraft.world.phys.AABB there = this.getBoundingBox().move(dest.subtract(this.position()))
				.expandTowards(0.0, player.getBbHeight(), 0.0);
		if (!this.level().noCollision(this, there) || this.level().containsAnyLiquid(there)) {
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
				other -> other != this && other != player && other.isAlive() && !herdmate(other))) {
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
				living -> living != this && living != player && living.isAlive() && !herdmate(living));
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
				living -> living != this && living != player && living.isAlive() && !herdmate(living));
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
		boolean wildAdult = !this.isTame() && !this.isBaby();
		boolean calm = this.entityData.get(DATA_CALM);
		boolean trialNow = this.trialShown();
		if (SaddleRules.canCalm(wildAdult, stack.is(Items.GOLDEN_APPLE), trialNow)) {
			if (!this.level().isClientSide()) {
				this.setCalm(SaddleRules.CALM_TICKS);
				this.setTarget(null);
				if (!player.getAbilities().instabuild) {
					stack.shrink(1);
				}
				this.playSound(SoundEvents.HORSE_EAT, 0.7f, 1.0f);
				MountEffects.calmed(this);
			}
			return InteractionResult.sidedSuccess(this.level().isClientSide());
		}
		boolean saddle = stack.is(MountItems.SHAMANIC_SADDLE.get());
		if (SaddleRules.canPlaceSaddle(wildAdult, calm, saddle, this.saddled(), trialNow)) {
			if (!this.level().isClientSide()) {
				this.trialTookSaddle = !player.getAbilities().instabuild;
				if (this.trialTookSaddle) {
					stack.shrink(1);
				}
				this.setSaddled(true);
				this.playSound(SoundEvents.HORSE_SADDLE, 0.6f, 1.0f);
			}
			return InteractionResult.sidedSuccess(this.level().isClientSide());
		}
		if (!player.isShiftKeyDown() && SaddleRules.canMount(wildAdult, calm, this.saddled(),
				this.level().isClientSide() ? 0 : this.refuseTicks, trialNow, this.isVehicle())) {
			if (!this.level().isClientSide()) {
				beginTrial(player);
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
		float meal = tk.darrow.shamanicmounts.tack.FeedRules.mealHeal(this.isTame(), meat(stack), this.getHealth(),
				this.getMaxHealth(), nutrition(stack));
		if (meal > 0.0f) {
			if (!this.level().isClientSide()) {
				this.heal(meal);
				if (!player.getAbilities().instabuild) {
					stack.shrink(1);
				}
				this.playSound(SoundEvents.HORSE_EAT, 0.7f, 0.9f);
				MountEffects.fed(this);
			}
			return InteractionResult.sidedSuccess(this.level().isClientSide());
		}
		if (this.isTame() && this.isOwnedBy(player) && player.isShiftKeyDown()) {
			openCustomInventoryScreen(player);
			return InteractionResult.sidedSuccess(this.level().isClientSide());
		}
		if (canBeRiddenNow() && !player.isShiftKeyDown() && this.getPassengers().size() < seatCount()
				&& (this.isOwnedBy(player) || this.getControllingPassenger() != null)) {
			if (!this.level().isClientSide()) {
				player.startRiding(this);
			}
			return InteractionResult.sidedSuccess(this.level().isClientSide());
		}
		return super.mobInteract(player, hand);
	}

	/** Any meat, vanilla or tagged by another mod, raw or cooked. Rotten flesh is not a meal. */
	private static boolean meat(ItemStack stack) {
		if (stack.isEmpty() || stack.is(Items.ROTTEN_FLESH)) {
			return false;
		}
		return stack.is(net.minecraft.tags.ItemTags.MEAT)
				|| stack.is(net.neoforged.neoforge.common.Tags.Items.FOODS_RAW_MEAT)
				|| stack.is(net.neoforged.neoforge.common.Tags.Items.FOODS_COOKED_MEAT);
	}

	private static int nutrition(ItemStack stack) {
		var food = stack.get(net.minecraft.core.component.DataComponents.FOOD);
		return food == null ? 0 : food.nutrition();
	}

	private boolean trialShown() {
		if (this.trial != null && !this.trial.finished()) {
			return true;
		}
		return this.level().isClientSide() && this.entityData.get(DATA_CUE) != 0;
	}

	private void setCalm(int ticks) {
		this.calmTicks = Math.max(0, ticks);
		boolean calm = this.calmTicks > 0;
		if (this.entityData.get(DATA_CALM) != calm) {
			this.entityData.set(DATA_CALM, calm);
		}
	}

	private void showCue(ReinTrial.Dir dir) {
		byte code = dir == null ? 0 : dir.code();
		if (this.entityData.get(DATA_CUE) == code) {
			return;
		}
		this.entityData.set(DATA_CUE, code);
		if (code != 0) {
			MountEffects.cue(this);
		}
	}

	private void clearCue() {
		if (this.entityData.get(DATA_CUE) != 0) {
			this.entityData.set(DATA_CUE, (byte) 0);
		}
	}

	private void beginTrial(Player player) {
		this.getNavigation().stop();
		this.setTarget(null);
		this.reinMask = 0;
		this.trial = ReinTrial.start(this.random.nextLong());
		this.showCue(this.trial.cue());
		this.setOrderedToSit(false);
		player.startRiding(this);
	}

	private void failTrial() {
		if (this.trial == null) {
			return;
		}
		this.trial = null;
		this.reinMask = 0;
		this.clearCue();
		this.refuseTicks = ReinTrial.REFUSE_TICKS;
		// A dead mount already dropped its tack, saddle included; a live one hands back the saddle it wore.
		ItemStack worn = this.isAlive() ? tack.removeItemNoUpdate(MountChestMenu.SADDLE_SLOT) : ItemStack.EMPTY;
		this.setSaddled(false);
		this.ejectPassengers();
		if (this.trialTookSaddle && !worn.isEmpty()) {
			this.spawnAtLocation(worn);
		}
		this.trialTookSaddle = false;
		this.playSound(SoundEvents.HORSE_ANGRY, 0.8f, 0.9f);
	}

	private void finishTame() {
		this.trial = null;
		this.reinMask = 0;
		this.clearCue();
		Player rider = this.getControllingPassenger() instanceof Player player ? player : null;
		if (rider != null) {
			this.tame(rider);
			// A new tame follows. Stay and wander are orders its owner gives on the mount screen.
			this.setMode(FollowRules.DEFAULT);
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
		// The female of a mixed pair is the dam; a same-sex pair keeps the order they were fed in.
		ShamanicMount damMount = this.male && !other.male ? other : this;
		ShamanicMount sireMount = damMount == this ? other : this;
		Genome child = Meiosis.child(damMount.genome, sireMount.genome, new java.util.Random(this.random.nextLong()));
		ShamanicMount foal = MountEntities.MOUNT.get().create(server);
		if (foal == null) {
			return;
		}
		foal.setGenome(child, true);
		foal.assignStats(MountStats.child(this.bodyStats(), other.bodyStats(), new java.util.Random(this.random.nextLong())), true);
		foal.male = this.random.nextBoolean();
		foal.moveTo(this.getX(), this.getY(), this.getZ(), this.getYRot(), 0.0f);
		foal.setAge(BreedingRules.FOAL_AGE);
		// A foal is born wild. Once grown it takes the saddle trial like any mount, and its lineage
		// goes into the herd book when it is tamed.
		foal.dam = damMount.getUUID();
		foal.sire = sireMount.getUUID();
		foal.setPersistenceRequired();
		server.addFreshEntity(foal);
		if (child.chimera) {
			// The chimera joins the herd book for whoever bred it.
			MountHerdData herd = MountHerdData.get(server);
			herd.bredChimera(player.getUUID());
			herd.bredChimera(damMount.getOwnerUUID());
			herd.bredChimera(sireMount.getOwnerUUID());
		}
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
		MountStats stats = bodyStats();
		herd.adopt(this.getUUID(), player.getUUID(), name, male, genome, parentDam, parentSire, pelt(), stats.health(),
				stats.speed(), stats.jump(), stats.stamina());
		this.setCustomName(Component.literal(name));
		this.setCustomNameVisible(true);
		noteWhere();
	}

	/** Where the flute last heard this tame. The herd data lives with the overworld. */
	private void noteWhere() {
		if (!this.isTame() || this.getOwnerUUID() == null || !(this.level() instanceof ServerLevel server)) {
			return;
		}
		MountHerdData.get(server).note(this.getUUID(), server.dimension().location().toString(), this.getX(), this.getY(),
				this.getZ(), this.mode());
	}

	private void hearts() {
		if (this.level() instanceof ServerLevel server) {
			server.sendParticles(ParticleTypes.HEART, this.getX(), this.getY() + this.getBbHeight(), this.getZ(), 7,
					0.4, 0.4, 0.4, 0.0);
		}
	}

	/** Hand this tame to another player. The pedigree stays. The new owner is not sat. */
	public void transferTo(ServerPlayer to) {
		this.setOwnerUUID(to.getUUID());
		this.setTame(true, false);
		this.setOrderedToSit(false);
		this.setMode(MountMode.FOLLOW);
		if (!(this.level() instanceof ServerLevel server)) {
			return;
		}
		MountHerdData herd = MountHerdData.get(server);
		if (herd.give(this.getUUID(), to.getUUID())) {
			return;
		}
		String name = this.getCustomName() == null ? MountNames.of(phenotype, this.isBaby())
				: this.getCustomName().getString();
		MountStats stats = bodyStats();
		herd.adopt(this.getUUID(), to.getUUID(), name, male, genome, dam, sire, pelt(), stats.health(), stats.speed(),
				stats.jump(), stats.stamina());
	}

	public void releaseIntoWorld(Player player) {
		this.setTame(false, false);
		this.setOwnerUUID(null);
		this.setMode(MountMode.WANDER);
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

	/** The rider who steers: the owner in the front seat, or the rider of a rein trial. A friend behind never steers. */
	@Override
	public LivingEntity getControllingPassenger() {
		if (!(this.getFirstPassenger() instanceof LivingEntity living)) {
			return null;
		}
		if (this.trial != null || !this.isTame() || this.isOwnedBy(living)) {
			return living;
		}
		return null;
	}

	@Override
	public boolean canJump() {
		return this.isVehicle() && canBeRiddenNow();
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
		// The cue is synced, because the trial object lives on the server and the rider's client owns the motion.
		// Asked left steps right, asked forward steps back. The step kicks, then eases, and the mount hops.
		// The rider sticks up past the hitbox, so a kick toward a tree or a wall is held in place instead.
		ReinTrial.Dir cue = this.shownCue();
		if (cue != null && !this.isTame()) {
			if (this.getControllingPassenger() instanceof Player player) {
				float yaw = player.getYRot();
				int into = this.cueAge();
				float fight = (float) Math.sin(into * 0.85) * 9.0f;
				float toss = 0.0f;
				if (into < 8) {
					toss = -24.0f * (1.0f - into / 8.0f);
				} else if (into < 16) {
					toss = 12.0f * (1.0f - (into - 8) / 8.0f);
				}
				this.setYRot(yaw);
				this.yRotO = yaw;
				this.setYBodyRot(yaw + fight);
				this.setYHeadRot(yaw - fight * 0.6f);
				this.setXRot(toss);
				float surge = ReinTrial.buck(into);
				float strafe = cue.strafe() * surge;
				float forward = cue.forward() * surge;
				boolean ahead = this.riderAhead(player, strafe, forward, yaw, 0.0);
				boolean up = this.riderClear(player, this.getX(), this.getY() + ReinTrial.BUCK_HOP + 0.2, this.getZ());
				boolean aheadUp = this.riderAhead(player, strafe, forward, yaw, ReinTrial.BUCK_HOP + 0.2);
				boolean hop = this.isControlledByLocalInstance() && ReinTrial.hops(into) && this.onGround() && up
						&& (aheadUp || !ahead);
				if (hop) {
					Vec3 motion = this.getDeltaMovement();
					boolean carry = ahead && aheadUp;
					this.setDeltaMovement(carry ? motion.x : 0.0, ReinTrial.BUCK_HOP, carry ? motion.z : 0.0);
					this.hasImpulse = true;
				}
				this.setSpeed((float) this.getAttributeValue(Attributes.MOVEMENT_SPEED));
				if (ahead && (!hop || aheadUp)) {
					super.travel(new Vec3(strafe, 0.0, forward));
				} else {
					Vec3 motion = this.getDeltaMovement();
					this.setDeltaMovement(0.0, motion.y, 0.0);
					super.travel(Vec3.ZERO);
				}
			} else {
				super.travel(Vec3.ZERO);
			}
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
			double walked = this.getAttributeValue(Attributes.MOVEMENT_SPEED);
			float speed = this.entityData.get(DATA_GALLOP) ? (float) MountStats.gallopSpeed(walked) : (float) walked;
			this.setSpeed(speed);
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
			this.setDeltaMovement(this.getDeltaMovement().add(0.0,
					MountStats.jumpImpulse(this.entityData.get(DATA_STAT_JUMP)), 0.0));
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
		if (!this.level().isClientSide() && passenger instanceof Player rider && GiftRules.drum(phenotype)) {
			net.minecraft.world.effect.MobEffectInstance sight = rider.getEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION);
			if (sight != null && sight.isAmbient() && !sight.isInfiniteDuration() && sight.getDuration() <= NIGHT_VISION_TICKS) {
				rider.removeEffect(net.minecraft.world.effect.MobEffects.NIGHT_VISION);
			}
		}
		if (!this.level().isClientSide() && passenger instanceof Player rider) {
			lastRider = rider.getUUID();
			lastRiderTick = this.level().getGameTime();
			if (rider instanceof ServerPlayer server && MountPosts.dismounted(server, this.getUUID())) {
				server.displayClientMessage(Component.translatable("shamanicmounts.trade.cancelled"), true);
			}
		}
		// Stepping off leaves the order as it was: a following mount keeps following.
		if (this.trial != null && !this.trial.finished()) {
			failTrial();
		}
	}

	@Override
	public boolean hurt(DamageSource source, float amount) {
		// A herdmate's maul, ram, coil, or bite lands on nothing, so neither one turns on the other.
		if (this.herdmate(source.getEntity())) {
			return false;
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

	/**
	 * Wild mounts stay where they were born, like other animals. Most are born with the land, out at the
	 * edge of sight; a mount that despawned once no one stood near would be gone long before a rider got
	 * there, and the animals that never leave keep the creature cap too full for more to be born later.
	 */
	@Override
	public boolean removeWhenFarAway(double distance) {
		return false;
	}

	@Override
	protected void dropEquipment() {
		super.dropEquipment();
		if (this.trial != null && !this.trialTookSaddle) {
			tack.removeItemNoUpdate(MountChestMenu.SADDLE_SLOT);
		}
		if (!pendingTrialSaddle.isEmpty()) {
			this.spawnAtLocation(pendingTrialSaddle);
			pendingTrialSaddle = ItemStack.EMPTY;
		}
		Containers.dropContents(this.level(), this, this.chest);
		Containers.dropContents(this.level(), this, this.tack);
		this.chest.clearContent();
		this.tack.clearContent();
	}

	@Override
	public void die(DamageSource source) {
		if (!this.level().isClientSide() && this.level() instanceof ServerLevel server && this.isTame()) {
			MountHerdData.get(server).release(this.getOwnerUUID(), this.getUUID());
		}
		super.die(source);
	}

	@Override
	public void onAddedToLevel() {
		super.onAddedToLevel();
		if (!this.level().isClientSide()) {
			MountCall.added(this);
		}
	}

	@Override
	public void addAdditionalSaveData(CompoundTag tag) {
		super.addAdditionalSaveData(tag);
		tag.put("Genome", GenomeIO.write(genome));
		tag.putBoolean("GenomeLocked", genomeLocked);
		tag.putBoolean("Male", male);
		tag.putBoolean("Saddled", this.saddled());
		tag.putInt("Refuse", refuseTicks);
		tag.putInt("Calm", calmTicks);
		tag.putBoolean("SaddlePaid", trialTookSaddle);
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
		tag.putInt("ModeVersion", FollowRules.ORDERS_VERSION);
		tag.putByte("RiderKeys", (byte) ((riderSneak ? 1 : 0) | (jumpHeld ? 2 : 0)));
		tag.putInt("SneakHeld", sneakHeld);
		net.minecraft.nbt.ListTag chestList = new net.minecraft.nbt.ListTag();
		for (int slot = 0; slot < this.chest.getContainerSize(); slot++) {
			ItemStack piece = this.chest.getItem(slot);
			if (!piece.isEmpty()) {
				CompoundTag one = new CompoundTag();
				one.putByte("Slot", (byte) slot);
				chestList.add(piece.save(this.registryAccess(), one));
			}
		}
		tag.put("Chest", chestList);
		net.minecraft.nbt.ListTag tackList = new net.minecraft.nbt.ListTag();
		for (int slot = 0; slot < this.tack.getContainerSize(); slot++) {
			ItemStack piece = this.tack.getItem(slot);
			// The saddle of a rein trial is saved with a flag: the trial cannot resume without its rider, so on load
			// the mount hands the saddle back as a failed try would, instead of keeping it wild-saddled or losing it.
			if (slot == MountChestMenu.SADDLE_SLOT && this.trial != null && !piece.isEmpty()) {
				tag.putBoolean("TrialPending", true);
			}
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
		tag.putInt("StatHealth", this.entityData.get(DATA_STAT_HEALTH));
		tag.putInt("StatSpeed", this.entityData.get(DATA_STAT_SPEED));
		tag.putInt("StatJump", this.entityData.get(DATA_STAT_JUMP));
		tag.putInt("StatStamina", this.entityData.get(DATA_STAT_STAMINA));
		tag.putInt("Gallop", this.gallopStamina);
		tag.putBoolean("GallopLocked", this.gallopLocked);
		noteWhere();
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
		this.calmTicks = tag.getInt("Calm");
		if (this.calmTicks > 0) {
			this.entityData.set(DATA_CALM, true);
		}
		this.trialTookSaddle = tag.getBoolean("SaddlePaid");
		this.restTicks = tag.getInt("Rest");
		this.stamina = tag.contains("Stamina") ? tag.getInt("Stamina") : GiftRules.CLIMB_TICKS;
		this.climbSpent = tag.getBoolean("ClimbSpent");
		this.drumCooldown = tag.getInt("Drum");
		this.ramCooldown = tag.getInt("Ram");
		this.maulCooldown = tag.getInt("Maul");
		this.coilCooldown = tag.getInt("Coil");
		this.awayTicks = tag.getInt("Away");
		this.awayCooldown = tag.getInt("AwayCd");
		this.blinkCooldown = tag.getInt("Blink");
		this.revealTicks = tag.getInt("Reveal");
		setMode(FollowRules.loaded(tag.contains("Mode") ? tag.getString("Mode") : null, tag.getInt("ModeVersion"),
				this.isOrderedToSit(), GiftRules.guard(phenotype)));
		this.chest.clearContent();
		for (net.minecraft.nbt.Tag raw : tag.getList("Chest", net.minecraft.nbt.Tag.TAG_COMPOUND)) {
			CompoundTag one = (CompoundTag) raw;
			int slot = one.getByte("Slot") & 255;
			if (slot < this.chest.getContainerSize()) {
				this.chest.setItem(slot, ItemStack.parse(this.registryAccess(), one).orElse(ItemStack.EMPTY));
			}
		}
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
		if (tag.getBoolean("TrialPending")) {
			// Saved mid-trial: no rider is seated to finish it, so the try fails. A paid saddle is dropped on the
			// first tick, once the mount stands in its level; a creative one simply goes.
			ItemStack worn = this.tack.removeItemNoUpdate(MountChestMenu.SADDLE_SLOT);
			this.pendingTrialSaddle = this.trialTookSaddle && !worn.isEmpty() ? worn : ItemStack.EMPTY;
			this.setSaddled(false);
			this.refuseTicks = ReinTrial.REFUSE_TICKS;
		}
		syncTack();
		if (this.tack.getItem(MountChestMenu.SADDLE_SLOT).isEmpty()) {
			this.trialTookSaddle = false;
		}
		this.dam = tag.hasUUID("Dam") ? tag.getUUID("Dam") : null;
		this.sire = tag.hasUUID("Sire") ? tag.getUUID("Sire") : null;
		boolean hadStats = tag.contains("StatHealth");
		MountStats loaded = new MountStats(
				MountStats.read(hadStats, tag.getInt("StatHealth")),
				MountStats.read(tag.contains("StatSpeed"), tag.getInt("StatSpeed")),
				MountStats.read(tag.contains("StatJump"), tag.getInt("StatJump")),
				MountStats.read(tag.contains("StatStamina"), tag.getInt("StatStamina")));
		boolean fillGallop = !tag.contains("Gallop");
		this.bodyApplied = true;
		assignStats(loaded, fillGallop);
		if (!fillGallop) {
			int capacity = MountStats.capacityTicks(loaded.stamina());
			this.gallopStamina = Math.max(0, Math.min(capacity, tag.getInt("Gallop")));
			this.gallopLocked = tag.getBoolean("GallopLocked") || this.gallopStamina <= 0;
		}
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
