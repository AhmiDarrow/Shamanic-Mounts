package tk.darrow.shamanicmounts.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.StringJoiner;
import java.util.UUID;

import com.mojang.blaze3d.vertex.PoseStack;

import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.screens.PauseScreen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.ClientChatReceivedEvent;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import tk.darrow.shamanicmounts.book.HerdBook;
import tk.darrow.shamanicmounts.genome.Founders;
import tk.darrow.shamanicmounts.genome.Genome;
import tk.darrow.shamanicmounts.genome.Marks;
import tk.darrow.shamanicmounts.genome.Strand;

/**
 * Command file for the live harness ({@code -Dshamanicmounts.harness=true}).
 *
 * <p>An external driver writes {@code <gameDir>/showcase-command.txt}. This poller runs that one command on the
 * client thread and replaces the file with {@code showcase-done.txt}: line 1 is the command, line 2 is
 * {@code ok ...} or {@code error ...}. Same hand-off Tribal Power's showcase client uses.
 *
 * <ul>
 *   <li>{@code shot <label>}, {@code useitem}, {@code use <x> <y> <z> [times] [face]}, {@code useentity}, {@code hotbar <0-8>}</li>
 *   <li>{@code look <x> <y> <z>}, {@code name} — the held item's display name</li>
 *   <li>{@code buttons}, {@code press <index|text>}, {@code click <slot> [button] [type]}, {@code state}, {@code close}</li>
 *   <li>{@code messages [clear]}</li>
 *   <li>{@code seed} — open a herd book that already holds Brook and Ash, so rename and release can be driven
 *       before a world mount exists</li>
 *   <li>{@code pick <index>} — click a row on the tames page</li>
 *   <li>{@code type <text>} — fill the rename box</li>
 *   <li>{@code report} — the open herd book's page, tame names, and the lines it is drawing</li>
 * </ul>
 */
public final class HarnessVerification {
	private static final long POLL_MS = 100;
	private static long nextPoll;
	private static int captures;
	private static final ArrayDeque<String> MESSAGES = new ArrayDeque<>();
	private static String lastOverlay = "";
	private static int lastOverlayTime;

	private HarnessVerification() {
	}

	public static void install() {
		net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(HarnessVerification::tick);
		net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener(HarnessVerification::chat);
	}

	public static void chat(ClientChatReceivedEvent event) {
		synchronized (MESSAGES) {
			MESSAGES.addLast(event.getMessage().getString().replace('|', '/').replace('\n', ' '));
			while (MESSAGES.size() > 200) {
				MESSAGES.removeFirst();
			}
		}
	}

	/** Action-bar text does not arrive as chat, so it is read off the HUD. */
	private static void overlay(Minecraft mc) {
		try {
			var text = net.minecraft.client.gui.Gui.class.getDeclaredField("overlayMessageString");
			var time = net.minecraft.client.gui.Gui.class.getDeclaredField("overlayMessageTime");
			text.setAccessible(true);
			time.setAccessible(true);
			var component = (net.minecraft.network.chat.Component) text.get(mc.gui);
			int ticks = time.getInt(mc.gui);
			String shown = component == null ? "" : component.getString();
			boolean fresh = ticks > lastOverlayTime || (!shown.equals(lastOverlay) && ticks > 0);
			lastOverlayTime = ticks;
			if (fresh && !shown.isEmpty()) {
				lastOverlay = shown;
				synchronized (MESSAGES) {
					MESSAGES.addLast("[bar] " + shown.replace('|', '/').replace('\n', ' '));
					while (MESSAGES.size() > 200) {
						MESSAGES.removeFirst();
					}
				}
			}
		} catch (ReflectiveOperationException ignored) {
		}
	}

	public static void tick(ClientTickEvent.Post event) {
		Minecraft mc = Minecraft.getInstance();
		// A headless window is never focused. Without this, the pause menu opens and eats every click.
		if (mc.options.pauseOnLostFocus) {
			mc.options.pauseOnLostFocus = false;
		}
		if (mc.screen instanceof PauseScreen) {
			mc.setScreen(null);
		}
		if (mc.player != null) {
			overlay(mc);
		}
		long now = System.currentTimeMillis();
		if (now < nextPoll) {
			return;
		}
		nextPoll = now + POLL_MS;
		// A minimized harness window leaves the framebuffer at 1x1, so every screenshot is a blank pixel.
		var target = mc.getMainRenderTarget();
		if (target != null && (target.width < 100 || target.height < 100) && mc.getWindow() != null) {
			org.lwjgl.glfw.GLFW.glfwRestoreWindow(mc.getWindow().getWindow());
			org.lwjgl.glfw.GLFW.glfwShowWindow(mc.getWindow().getWindow());
			mc.getWindow().setWindowed(1280, 800);
		}
		poll(mc);
	}

	private static void poll(Minecraft mc) {
		Path command = mc.gameDirectory.toPath().resolve("showcase-command.txt");
		if (!Files.isRegularFile(command)) {
			return;
		}
		String line;
		try {
			line = Files.readString(command).strip();
			Files.deleteIfExists(command);
		} catch (IOException e) {
			return;
		}
		if (line.isEmpty()) {
			return;
		}
		String result;
		try {
			result = execute(mc, line);
		} catch (RuntimeException e) {
			result = "error " + e;
		}
		try {
			Files.writeString(mc.gameDirectory.toPath().resolve("showcase-done.txt"), line + "\n" + result + "\n");
		} catch (IOException ignored) {
		}
	}

	private static void grab(Minecraft mc, String name) {
		Screenshot.grab(mc.gameDirectory, name, mc.getMainRenderTarget(), message -> {
		});
	}

	private static String describe(ItemStack stack) {
		return stack.isEmpty() ? "empty" : BuiltInRegistries.ITEM.getKey(stack.getItem()) + "x" + stack.getCount();
	}

	private static String execute(Minecraft mc, String line) {
		String[] parts = line.split("\\s+");
		switch (parts[0]) {
			case "shot" -> {
				String label = parts.length > 1 ? parts[1].replaceAll("[^A-Za-z0-9_-]", "_") : "shot";
				grab(mc, "showcase-" + (++captures) + "-" + label + ".png");
				return "ok showcase-" + captures + "-" + label + ".png";
			}
			case "useentity" -> {
				if (mc.player == null || mc.gameMode == null || mc.level == null) {
					return "error no player";
				}
				if (mc.screen != null) {
					mc.setScreen(null);
				}
				Vec3 eye = mc.player.getEyePosition();
				Vec3 look = mc.player.getLookAngle();
				Entity target = null;
				double best = 7.0;
				for (Entity entity : mc.level.entitiesForRendering()) {
					// Dropped items and orbs are never what the harness means to use.
					if (entity == mc.player || !entity.isAlive() || entity instanceof net.minecraft.world.entity.item.ItemEntity
							|| entity instanceof net.minecraft.world.entity.ExperienceOrb) {
						continue;
					}
					Vec3 to = entity.getBoundingBox().getCenter().subtract(eye);
					double distance = to.length();
					if (distance < 0.05 || distance > 6.0) {
						continue;
					}
					if (to.normalize().dot(look) < 0.65) {
						continue;
					}
					if (distance < best) {
						best = distance;
						target = entity;
					}
				}
				if (target == null) {
					return "error no entity";
				}
				InteractionResult used = mc.gameMode.interact(mc.player, target, InteractionHand.MAIN_HAND);
				return "ok " + used + " " + target.getId();
			}
			case "useitem" -> {
				if (mc.player == null || mc.gameMode == null) {
					return "error no player";
				}
				int times = parts.length > 1 ? Integer.parseInt(parts[1]) : 1;
				if (mc.screen != null) {
					mc.setScreen(null);
				}
				InteractionResult last = InteractionResult.PASS;
				for (int i = 0; i < times; i++) {
					last = mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
				}
				return "ok " + last + " x" + times;
			}
			case "use" -> {
				if (mc.player == null || mc.gameMode == null) {
					return "error no player";
				}
				if (parts.length < 4) {
					return "error missing block position";
				}
				BlockPos pos = new BlockPos(Integer.parseInt(parts[1]), Integer.parseInt(parts[2]), Integer.parseInt(parts[3]));
				int times = parts.length > 4 ? Integer.parseInt(parts[4]) : 1;
				Direction face = parts.length > 5 ? Direction.byName(parts[5]) : Direction.UP;
				if (face == null) {
					face = Direction.UP;
				}
				if (mc.screen != null) {
					mc.setScreen(null);
				}
				mc.player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES, Vec3.atCenterOf(pos));
				var hit = new BlockHitResult(Vec3.atCenterOf(pos).add(Vec3.atLowerCornerOf(face.getNormal()).scale(0.5)), face, pos, false);
				InteractionResult last = InteractionResult.PASS;
				for (int i = 0; i < times; i++) {
					last = mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
				}
				return "ok " + last + " x" + times;
			}
			case "hotbar" -> {
				if (mc.player == null) {
					return "error no player";
				}
				int slot = Integer.parseInt(parts[1]);
				mc.player.getInventory().selected = Math.floorMod(slot, 9);
				return "ok slot " + mc.player.getInventory().selected + " " + describe(mc.player.getMainHandItem());
			}
			case "look" -> {
				if (mc.player == null) {
					return "error no player";
				}
				if (parts.length < 4) {
					return "error missing position";
				}
				mc.player.lookAt(net.minecraft.commands.arguments.EntityAnchorArgument.Anchor.EYES,
						new Vec3(Double.parseDouble(parts[1]), Double.parseDouble(parts[2]), Double.parseDouble(parts[3])));
				return "ok looking";
			}
			case "name" -> {
				if (mc.player == null) {
					return "error no player";
				}
				return "ok " + mc.player.getMainHandItem().getHoverName().getString();
			}
			case "click" -> {
				if (mc.player == null || mc.gameMode == null) {
					return "error no player";
				}
				if (!(mc.screen instanceof AbstractContainerScreen<?> screen)) {
					return "error no menu open";
				}
				int slot = Integer.parseInt(parts[1]);
				int button = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
				ClickType type = parts.length > 3 ? ClickType.valueOf(parts[3]) : ClickType.PICKUP;
				mc.gameMode.handleInventoryMouseClick(screen.getMenu().containerId, slot, button, type, mc.player);
				return "ok clicked " + slot + " carrying " + describe(screen.getMenu().getCarried());
			}
			case "buttons" -> {
				if (mc.screen == null) {
					return "error no screen open";
				}
				StringJoiner out = new StringJoiner("|");
				int i = 0;
				for (var child : mc.screen.children()) {
					if (child instanceof AbstractWidget widget) {
						out.add((i++) + ":" + widget.getClass().getSimpleName() + ":"
								+ widget.getMessage().getString().replace('|', '/'));
					}
				}
				return "ok " + out;
			}
			case "press" -> {
				if (mc.screen == null) {
					return "error no screen open";
				}
				String want = line.substring(parts[0].length()).strip();
				int i = 0;
				AbstractWidget hit = null;
				for (var child : mc.screen.children()) {
					if (!(child instanceof AbstractWidget widget)) {
						continue;
					}
					boolean match = want.matches("\\d+") ? i == Integer.parseInt(want)
							: widget.getMessage().getString().toLowerCase().contains(want.toLowerCase());
					if (match) {
						hit = widget;
						break;
					}
					i++;
				}
				if (hit == null) {
					return "error no widget " + want;
				}
				if (!hit.active) {
					return "error widget inactive " + hit.getMessage().getString();
				}
				double cx = hit.getX() + hit.getWidth() / 2.0;
				double cy = hit.getY() + hit.getHeight() / 2.0;
				hit.mouseClicked(cx, cy, 0);
				hit.mouseReleased(cx, cy, 0);
				return "ok pressed " + hit.getMessage().getString().replace('|', '/');
			}
			case "state" -> {
				StringJoiner out = new StringJoiner(" ");
				out.add("ok");
				if (mc.player == null) {
					out.add("pos=");
					out.add("held=empty");
				} else {
					out.add("pos=" + mc.player.blockPosition().toShortString().replace(" ", ""));
					out.add("held=" + describe(mc.player.getMainHandItem()));
				}
				out.add("screen=" + (mc.screen == null ? "none" : mc.screen.getClass().getSimpleName()));
				if (mc.screen != null) {
					out.add("title=" + mc.screen.getTitle().getString().replace(' ', '_'));
				}
				if (mc.screen instanceof AbstractContainerScreen<?> screen) {
					StringJoiner slots = new StringJoiner(",");
					for (Slot slot : screen.getMenu().slots) {
						if (slot.hasItem()) {
							slots.add(slot.index + ":" + describe(slot.getItem()));
						}
					}
					out.add("slots=" + screen.getMenu().slots.size() + "[" + slots + "]");
				}
				return out.toString();
			}
			case "messages" -> {
				synchronized (MESSAGES) {
					String all = String.join("|", MESSAGES);
					if (parts.length > 1 && parts[1].equals("clear")) {
						MESSAGES.clear();
					}
					return "ok " + all;
				}
			}
			case "seed" -> {
				if (mc.player == null) {
					return "error no player";
				}
				UUID owner = mc.player.getUUID();
				HerdBook book = new HerdBook();
				HerdBook.Entry brook = book.keep(owner, "Brook", true, Founders.eightfold(), null, null);
				Genome carriedSkin = new Genome(
						Strand.wild(Marks.Torso.CAT, Marks.Head.CAT).with(Marks.Gift.SKIN),
						Strand.wild(Marks.Torso.CAT, Marks.Head.CAT),
						true, true, true);
				book.keep(owner, "Ash", false, carriedSkin, brook.id(), null);
				ClientBook.open(owner, book);
				return "ok seeded";
			}
			case "pick" -> {
				if (!(mc.screen instanceof HerdBookScreen book)) {
					return "error no book";
				}
				int index = Integer.parseInt(parts[1]);
				boolean hit = book.harnessPick(index);
				return (hit ? "ok " : "error miss ") + book.harnessReport();
			}
			case "bookscroll" -> {
				// bookscroll end|top: move the open book page to its end or back to the top.
				if (!(mc.screen instanceof HerdBookScreen book)) {
					return "error no book";
				}
				book.harnessScroll(parts.length > 1 && parts[1].equals("end"));
				return "ok " + book.harnessReport();
			}
			case "type" -> {
				if (!(mc.screen instanceof HerdBookScreen book)) {
					return "error no book";
				}
				return book.harnessType(line.substring(parts[0].length()).strip());
			}
			case "report" -> {
				if (!(mc.screen instanceof HerdBookScreen book)) {
					return "error no book";
				}
				return "ok " + book.harnessReport();
			}
			case "key" -> {
				if (parts.length < 3) {
					return "error key <forward|back|left|right|jump|sneak|use|attack> <down|up>";
				}
				var key = switch (parts[1]) {
					case "forward" -> mc.options.keyUp;
					case "back" -> mc.options.keyDown;
					case "left" -> mc.options.keyLeft;
					case "right" -> mc.options.keyRight;
					case "jump" -> mc.options.keyJump;
					case "sneak" -> mc.options.keyShift;
					case "use" -> mc.options.keyUse;
					case "attack" -> mc.options.keyAttack;
					default -> null;
				};
				if (key == null) {
					return "error unknown key " + parts[1];
				}
				key.setDown(parts[2].equals("down"));
				return "ok " + parts[1] + " " + parts[2];
			}
			case "pose" -> {
				// pose <swing> <amount> holds every mount at one point of its stride; pose off releases it.
				if (parts.length > 1 && parts[1].equals("off")) {
					MountRenderer.forcedGait = null;
					return "ok pose off";
				}
				if (parts.length < 3) {
					return "error pose <swing> <amount> | off";
				}
				MountRenderer.forcedGait = new float[] { Float.parseFloat(parts[1]), Float.parseFloat(parts[2]) };
				return "ok pose " + parts[1] + " " + parts[2];
			}
			case "wing" -> {
				// wing <1|2> holds every mount in the air, gliding or flapping; wing off releases it.
				if (parts.length < 2) {
					return "error wing <1|2> | off";
				}
				MountRenderer.forcedWing = parts[1].equals("off") ? -1 : Integer.parseInt(parts[1]);
				return "ok wing " + parts[1];
			}
			case "swing" -> {
				if (mc.player == null) {
					return "error no player";
				}
				mc.player.swing(InteractionHand.MAIN_HAND);
				return "ok swung";
			}
			case "dump" -> {
				// Every quad the nearest mount draws, with its atlas cell, into <gameDir>/showcase-dump.txt.
				if (mc.player == null || mc.level == null) {
					return "error no player";
				}
				ShamanicMountNearest: {
					tk.darrow.shamanicmounts.entity.ShamanicMount nearest = null;
					double best = Double.MAX_VALUE;
					for (Entity entity : mc.level.entitiesForRendering()) {
						if (entity instanceof tk.darrow.shamanicmounts.entity.ShamanicMount mount) {
							double d = mount.distanceToSqr(mc.player);
							if (d < best) {
								best = d;
								nearest = mount;
							}
						}
					}
					if (nearest == null) {
						return "error no mount";
					}
					StringBuilder out = new StringBuilder();
					int[] count = { 0 };
					com.mojang.blaze3d.vertex.VertexConsumer sink = new com.mojang.blaze3d.vertex.VertexConsumer() {
						final float[] xs = new float[4];
						final float[] ys = new float[4];
						final float[] zs = new float[4];
						final float[] us = new float[4];
						final float[] vs = new float[4];
						int at;

						@Override
						public com.mojang.blaze3d.vertex.VertexConsumer addVertex(float x, float y, float z) {
							xs[at] = x;
							ys[at] = y;
							zs[at] = z;
							return this;
						}

						@Override
						public com.mojang.blaze3d.vertex.VertexConsumer setColor(int r, int g, int b, int a) {
							return this;
						}

						@Override
						public com.mojang.blaze3d.vertex.VertexConsumer setUv(float u, float v) {
							us[at] = u;
							vs[at] = v;
							return this;
						}

						@Override
						public com.mojang.blaze3d.vertex.VertexConsumer setUv1(int u, int v) {
							return this;
						}

						@Override
						public com.mojang.blaze3d.vertex.VertexConsumer setUv2(int u, int v) {
							return this;
						}

						@Override
						public com.mojang.blaze3d.vertex.VertexConsumer setNormal(float x, float y, float z) {
							at++;
							if (at == 4) {
								at = 0;
								count[0]++;
								float u = (us[0] + us[1] + us[2] + us[3]) / 4f * Mat.ATLAS / Mat.CELL;
								float v = (vs[0] + vs[1] + vs[2] + vs[3]) / 4f * Mat.ATLAS / Mat.CELL;
								out.append(String.format(java.util.Locale.ROOT, "%.2f,%.2f,%.2f %.2f,%.2f,%.2f %.2f,%.2f,%.2f %.2f,%.2f,%.2f cell=%d,%d%n",
										xs[0], ys[0], zs[0], xs[1], ys[1], zs[1], xs[2], ys[2], zs[2], xs[3], ys[3], zs[3],
										(int) Math.floor(u), (int) Math.floor(v)));
							}
							return this;
						}
					};
					MountPose anim = new MountPose(0f, 0f, nearest.tickCount, 0f, 0f, 0f, 0, 0f, 0f, 0f, false, nearest.isBaby(),
							nearest.getId(), false);
					MountMesh.drawWithPlans(nearest.phenotype(), nearest.saddled(), nearest.hasBags(), nearest.armorTier(), nearest.pelt(),
							new PoseStack(), sink, null, 0, 0, anim, new SolidDraw.Plan[60]);
					try {
						Files.writeString(mc.gameDirectory.toPath().resolve("showcase-dump.txt"), out.toString());
					} catch (IOException e) {
						return "error " + e;
					}
					return "ok " + count[0] + " quads";
				}
			}
			case "hud" -> {
				// hud off hides the crosshair, hand, and hotbar for clean pictures; hud on brings them back.
				mc.options.hideGui = parts.length > 1 && parts[1].equals("off");
				return "ok hud " + (mc.options.hideGui ? "off" : "on");
			}
			case "creative" -> {
				// Open the creative inventory on the mod's own tab and report its contents.
				if (mc.player == null) {
					return "error no player";
				}
				var tab = tk.darrow.shamanicmounts.item.MountItems.TAB.get();
				tab.buildContents(new net.minecraft.world.item.CreativeModeTab.ItemDisplayParameters(
						mc.player.level().enabledFeatures(), mc.player.canUseGameMasterBlocks(), mc.player.level().registryAccess()));
				StringJoiner names = new StringJoiner(",");
				for (ItemStack stack : tab.getDisplayItems()) {
					names.add(BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath());
				}
				// The screen remembers its last tab in a private static; point it at ours before opening.
				try {
					var field = net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen.class.getDeclaredField("selectedTab");
					field.setAccessible(true);
					field.set(null, tab);
				} catch (ReflectiveOperationException e) {
					return "error " + e;
				}
				mc.setScreen(new net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen(mc.player,
						mc.player.level().enabledFeatures(), false));
				return "ok " + tab.getDisplayName().getString() + " [" + names + "]";
			}
			case "fps" -> {
				return "ok " + mc.getFps();
			}
			case "camera" -> {
				var type = switch (parts.length > 1 ? parts[1] : "first") {
					case "back" -> net.minecraft.client.CameraType.THIRD_PERSON_BACK;
					case "front" -> net.minecraft.client.CameraType.THIRD_PERSON_FRONT;
					default -> net.minecraft.client.CameraType.FIRST_PERSON;
				};
				mc.options.setCameraType(type);
				return "ok " + type;
			}
			case "riding" -> {
				if (mc.player == null) {
					return "error no player";
				}
				Entity vehicle = mc.player.getVehicle();
				return "ok " + (vehicle == null ? "none" : vehicle.getId() + " " + vehicle.getType().toShortString()
						+ " ground=" + vehicle.onGround() + " y=" + String.format("%.2f", vehicle.getY())) + " keys=" + MountClient.keysSent();
			}
			case "quiet" -> {
				mc.options.tutorialStep = net.minecraft.client.tutorial.TutorialSteps.NONE;
				mc.getToasts().clear();
				return "ok quiet";
			}
			case "close" -> {
				mc.setScreen(null);
				return "ok closed";
			}
			default -> {
				return "error unknown command " + parts[0];
			}
		}
	}
}
