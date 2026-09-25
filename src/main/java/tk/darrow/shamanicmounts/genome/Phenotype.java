package tk.darrow.shamanicmounts.genome;

import java.util.EnumSet;

/** What the rig shows. Alleles that did not win are still on the genome, for the book. */
public final class Phenotype {
	public enum LegShow { NONE, TWO, HITCH, FOUR, SPARE, EIGHT }
	public enum WingShow { NONE, PINION, FULL, ASTRAL, ASTRAL_FULL }
	public enum RackShow { NONE, BUDS, FULL, CROWN }
	public enum ScaleShow { SLIGHT, NORMAL, LARGE, GREATER }
	public enum CoatShow {
		SOLID, ROSETTE, STAR, DUSK, BONE, SPECKLED, DUSK_WASH, DUSK_ROSETTE, DUSK_STAR
	}
	public enum GaitShow { LAND, SEA, CINDER, MIST }
	public enum PhaseShow { SOLID, VEIL, DEEP_VEIL, GHOST }
	public enum BondShow { SADDLE, DRUM, NAGUAL }
	public enum SenseShow { EYE, SCENT }
	public enum FootShow { HOOF, PAW, TALON }
	public enum TailShow { NONE, STUB, PLUME, FLAG, LASH, FAN }

	public final Marks.Torso torsoMaternal;
	public final Marks.Torso torsoPaternal;
	public final Proportions proportions;
	public final Marks.Head head;
	public final Marks.Head carriedHead;
	public final FootShow foot;
	public final LegShow legs;
	public final RackShow rack;
	public final boolean crownHeavy;
	public final WingShow wings;
	/** Wing size against the founder crane's: 0.6 small to 3.0 vast. */
	public final float wingScale;
	public final TailShow tail;
	public final ScaleShow scale;
	public final float uniformScale;
	public final CoatShow coat;
	public final boolean coatMasked;
	public final GaitShow gait;
	public final EnumSet<Marks.Realm> realms;
	public final boolean realmPotent;
	public final PhaseShow phase;
	public final SenseShow sense;
	public final BondShow bond;
	public final EnumSet<Marks.Ward> wards;
	public final EnumSet<Marks.Trail> trails;
	public final EnumSet<Marks.Gift> gifts;
	/** Every gift that is showing is on both strands. A lone dream is only a glimpse. */
	public final boolean giftWhole;
	public final boolean thin;
	/** The ninth form. Body genes underneath are the cross; the animal showing is the chimera. */
	public final boolean chimera;

	public Phenotype(Marks.Torso torsoMaternal, Marks.Torso torsoPaternal, Proportions proportions, Marks.Head head,
			Marks.Head carriedHead, FootShow foot, LegShow legs, RackShow rack, boolean crownHeavy, WingShow wings,
			float wingScale, TailShow tail, ScaleShow scale, float uniformScale, CoatShow coat, boolean coatMasked, GaitShow gait,
			EnumSet<Marks.Realm> realms, boolean realmPotent, PhaseShow phase, SenseShow sense, BondShow bond,
			EnumSet<Marks.Ward> wards, EnumSet<Marks.Trail> trails, EnumSet<Marks.Gift> gifts, boolean giftWhole,
			boolean thin, boolean chimera) {
		this.torsoMaternal = torsoMaternal;
		this.torsoPaternal = torsoPaternal;
		this.proportions = proportions;
		this.head = head;
		this.carriedHead = carriedHead;
		this.foot = foot;
		this.legs = legs;
		this.rack = rack;
		this.crownHeavy = crownHeavy;
		this.wings = wings;
		this.wingScale = wingScale;
		this.tail = tail;
		this.scale = scale;
		this.uniformScale = uniformScale;
		this.coat = coat;
		this.coatMasked = coatMasked;
		this.gait = gait;
		this.realms = realms;
		this.realmPotent = realmPotent;
		this.phase = phase;
		this.sense = sense;
		this.bond = bond;
		this.wards = wards;
		this.trails = trails;
		this.gifts = gifts;
		this.giftWhole = giftWhole;
		this.thin = thin;
		this.chimera = chimera;
	}
}
