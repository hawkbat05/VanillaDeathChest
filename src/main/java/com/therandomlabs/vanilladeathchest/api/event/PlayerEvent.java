package com.therandomlabs.vanilladeathchest.api.event;

import java.util.List;
import net.fabricmc.fabric.util.HandlerList;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.ActionResult;
import net.minecraft.world.World;

public final class PlayerEvent {
	@FunctionalInterface
	public interface DropAllItems {
		ActionResult onPlayerDropAllItems(World world, PlayerEntity player, List<ItemEntity> drops);
	}

	public static final HandlerList<DropAllItems> DROP_ALL_ITEMS =
			new HandlerList<>(DropAllItems.class);
}