package com.therandomlabs.vanilladeathchest.config;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import blue.endless.jankson.Jankson;
import blue.endless.jankson.impl.SyntaxError;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;
import com.therandomlabs.vanilladeathchest.VanillaDeathChest;
import com.therandomlabs.vanilladeathchest.util.DeathChestPlacer;
import net.minecraft.item.EnumDyeColor;
import org.apache.commons.lang3.StringUtils;

public class VDCConfig {
	public static class Misc {
		@Config.LangKey("vanilladeathchest.config.misc.dropDeathChests")
		@Config.Comment({
				"Whether death chests should be dropped when broken.",
				"Enable this for infinite chests."
		})
		public boolean dropDeathChests;

		@Config.LangKey("vanilladeathchest.config.misc.gameRuleDefaultValue")
		@Config.Comment("The default value of the spawnDeathChests gamerule.")
		public boolean gameRuleDefaultValue = true;

		@Config.LangKey("vanilladeathchest.config.misc.gameruleName")
		@Config.Comment({
				"The name of the spawnDeathChests gamerule.",
				"Set this to an empty string to disable the gamerule."
		})
		public String gameRuleName = "spawnDeathChests";

		@Config.RequiresWorldRestart
		@Config.LangKey("vanilladeathchest.config.misc.vdcreload")
		@Config.Comment("Whether to enable the /vdcreload command.")
		public boolean vdcreload = true;

		@Config.RequiresMcRestart
		@Config.LangKey("vanilladeathchest.config.misc.vdcreloadclient")
		@Config.Comment("Whether to enable the /vdcreloadclient command.")
		public boolean vdcreloadclient = true;
	}

	public static class Protection {
		@Config.LangKey("vanilladeathchest.config.protection.bypassIfCreative")
		@Config.Comment("Whether players in creative mode should be able to bypass death chest " +
				"protection.")
		public boolean bypassIfCreative = true;

		@Config.RangeInt(min = 0)
		@Config.LangKey("vanilladeathchest.config.protection.bypassPermissionLevel")
		@Config.Comment("The required permission level to bypass death chest proteciton.")
		public int bypassPermissionLevel = 4;

		@Config.LangKey("vanilladeathchest.config.protection.enabled")
		@Config.Comment({
				"Whether death chests should be protected.",
				"When this is enabled, death chests can only be broken by their owners."
		})
		public boolean enabled = true;

		@Config.RangeInt(min = 0)
		@Config.LangKey("vanilladeathchest.config.protection.period")
		@Config.Comment({
				"The amount of time in ticks death chest protection should last.",
				"120000 ticks is 5 in-game days.",
				"Set this to 0 to protect death chests indefinitely."
		})
		public int period = 120000;
	}

	public static class Spawning {
		@Config.LangKey("vanilladeathchest.config.spawning.chatMessage")
		@Config.Comment({
				"The message sent to a player when they die and a death chest is placed.",
				"%d refers to the X, Y and Z coordinates.",
				"Set this to an empty string to disable the message."
		})
		public String chatMessage = "Death chest spawned at [%d, %d, %d]";

		@Config.LangKey("vanilladeathchest.config.spawning.chestType")
		@Config.Comment("The type of death chest that should be placed.")
		public DeathChestPlacer.DeathChestType chestType =
				DeathChestPlacer.DeathChestType.SINGLE_OR_DOUBLE;

		@Config.LangKey("vanilladeathchest.config.spawning.locationSearchRadius")
		@Config.Comment("The death chest location search radius.")
		public int locationSearchRadius = 8;

		@Config.LangKey("vanilladeathchest.config.spawning.shulkerBoxColor")
		@Config.Comment("The color of the shulker box if chestType is set to SHULKER_BOX.")
		public EnumDyeColor shulkerBoxColor = EnumDyeColor.WHITE;
	}

	public static final Path PATH =
			Paths.get("config", VanillaDeathChest.MOD_ID + ".json").toAbsolutePath();

	@Config.LangKey("vanilladeathchest.config.misc")
	@Config.Comment("Options that don't fit into any other categories.")
	public static Misc misc = new Misc();

	@Config.LangKey("vanilladeathchest.config.protection")
	@Config.Comment("Options related to death chest protection.")
	public static Protection protection = new Protection();

	@Config.LangKey("vanilladeathchest.config.spawning")
	@Config.Comment("Options related to death chest spawning.")
	public static Spawning spawning = new Spawning();

	private static final Map<String, Category> CATEGORIES = new HashMap<>();

	static {
		try {
			for(Field field : VDCConfig.class.getDeclaredFields()) {
				final int modifiers = field.getModifiers();

				if(!Modifier.isPublic(modifiers) || Modifier.isFinal(modifiers)) {
					continue;
				}

				final Object object = field.get(null);

				CATEGORIES.put(
						field.getName(),
						new Category(
								field, object, object.getClass().getDeclaredFields()
						)
				);
			}
		} catch(Exception ex) {
			VanillaDeathChest.crashReport("Error while getting config properties", ex);
		}
	}

	public static void reload() {
		JsonObject config = null;

		if(Files.exists(PATH)) {
			try {
				final String raw = Jankson.builder().build().load(
						StringUtils.join(Files.readAllLines(PATH), System.lineSeparator())
				).toJson();

				config = new JsonParser().parse(raw).getAsJsonObject();
			} catch(IOException | SyntaxError | IllegalStateException ex) {
				VanillaDeathChest.crashReport("Failed to read file: " + PATH, ex);
			}
		} else {
			config = new JsonObject();
		}

		try {
			for(Map.Entry<String, Category> category : CATEGORIES.entrySet()) {
				loadCategory(
						getObject(config, category.getKey()),
						category.getValue()
				);
			}
		} catch(IllegalAccessException ex) {
			VanillaDeathChest.crashReport("Failed to reload config", ex);
		}

		//Remove non-category entries

		for(Map.Entry<String, JsonElement> entry : config.entrySet()) {
			final String key = entry.getKey();

			if(!CATEGORIES.containsKey(key) || !entry.getValue().isJsonObject()) {
				config.remove(key);
			}
		}

		//Write JSON

		final String raw = new GsonBuilder().setPrettyPrinting().create().toJson(config).
				replaceAll(" {2}", "\t");

		try {
			Files.createDirectories(PATH.getParent());
			Files.write(PATH, (raw + System.lineSeparator()).getBytes(StandardCharsets.UTF_8));
		} catch(IOException ex) {
			VanillaDeathChest.crashReport("Failed to write to: " + PATH, ex);
		}
	}

	private static void loadCategory(JsonObject object, Category category)
			throws IllegalAccessException {
		addDescription(object, category.getDescription());

		final List<Property> properties = category.getProperties();

		for(Property property : properties) {
			loadProperty(
					category.getObject(),
					getObject(object, property.getKey()),
					property
			);
		}

		//Remove non-property entries

		for(Map.Entry<String, JsonElement> entry : object.entrySet()) {
			final String key = entry.getKey();

			if(key.equals("description")) {
				continue;
			}

			boolean found = false;

			for(Property property : properties) {
				if(property.getKey().equals(key)) {
					found = true;
					break;
				}
			}

			if(!found) {
				object.remove(key);
			}
		}
	}

	private static void loadProperty(Object categoryObject, JsonObject object, Property property)
			throws IllegalAccessException {
		addDescription(object, property.getDescription());

		final Field field = property.getField();
		final Class<?> type = field.getType();

		final Object defaultValue = property.getDefaultValue();
		final JsonElement value = object.get("value");
		Object newValue = null;

		if(property.isEnum()) {
			if(value != null && value.isJsonPrimitive()) {
				final JsonPrimitive primitive = value.getAsJsonPrimitive();

				if(primitive.isString()) {
					final String string = primitive.getAsString();

					for(Object constant : type.getEnumConstants()) {
						if(constant.toString().equals(string)) {
							newValue = constant;
							break;
						}
					}
				}
			}

			if(newValue == null) {
				object.addProperty("value", defaultValue.toString());
			}
		} else if(type == String.class) {
			if(value != null && value.isJsonPrimitive()) {
				final JsonPrimitive primitive = value.getAsJsonPrimitive();

				if(primitive.isString()) {
					newValue = primitive.getAsString();
				}
			}

			if(newValue == null) {
				object.addProperty("value", defaultValue.toString());
			}
		} else if(type == boolean.class) {
			if(value != null && value.isJsonPrimitive()) {
				final JsonPrimitive primitive = value.getAsJsonPrimitive();

				if(primitive.isBoolean()) {
					newValue = primitive.getAsBoolean();
				}
			}

			if(newValue == null) {
				object.addProperty("value", (Boolean) defaultValue);
			}
		} else if(type == int.class) {
			if(value != null && value.isJsonPrimitive()) {
				final JsonPrimitive primitive = value.getAsJsonPrimitive();

				if(primitive.isNumber()) {
					int number = primitive.getAsInt();

					final int min = property.getMin();
					final int max = property.getMax();

					if(number < min) {
						number = min;
						object.addProperty("value", number);
					}

					if(number > max) {
						number = max;
						object.addProperty("value", number);
					}

					newValue = number;
				}
			}

			if(newValue == null) {
				object.addProperty("value", (Integer) defaultValue);
			}
		}

		field.set(categoryObject, newValue == null ? defaultValue : newValue);
	}

	private static void addDescription(JsonObject object, String[] description) {
		final JsonArray array = new JsonArray();

		for(String line : description) {
			array.add(line);
		}

		object.add("description", array);
	}

	private static JsonObject getObject(JsonObject object, String key) {
		final JsonElement value = object.get(key);

		if(value != null && value.isJsonObject()) {
			return value.getAsJsonObject();
		}

		final JsonObject newObject = new JsonObject();
		object.add(key, newObject);
		return newObject;
	}
}