package io.github.toomanybugs1.QuickFarm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.Item;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.PlayerInventory;
import org.bukkit.plugin.java.JavaPlugin;

import com.gmail.nossr50.mcMMO;
import com.gmail.nossr50.api.ExperienceAPI;
import com.gmail.nossr50.datatypes.player.McMMOPlayer;
import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import com.gmail.nossr50.util.player.UserManager;

import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

public final class Main extends JavaPlugin implements Listener {

	List<String> enabledPlayers;
	mcMMO mcmmo;

	@Override
	public void onEnable() {

		Bukkit.getPluginManager().registerEvents(this, this);

		this.saveDefaultConfig();

		this.enabledPlayers = this.getConfig().getStringList("players-enabled");

		this.mcmmo = (mcMMO) Bukkit.getServer().getPluginManager().getPlugin("mcMMO");

		if (this.mcmmo == null)
			getLogger().info("This server does not have mcMMO. Disabling mcMMO features.");
		else
			getLogger().info("mcMMO has been detected!");
	}

	@Override
	public boolean onCommand(@NotNull CommandSender sender, Command cmd, @NotNull String label, String[] args) {

		if (!cmd.getName().equalsIgnoreCase("togglequickfarm") || args.length != 0)
			return false;

		if (sender instanceof Player) {

			this.enabledPlayers = this.getConfig().getStringList("players-enabled");

			String playerName = sender.getName();

			if (this.enabledPlayers.contains(playerName)) {
				this.enabledPlayers.remove(playerName);

				sender.sendMessage(ChatColor.GOLD + "[QuickFarm] "
					+ ChatColor.DARK_RED + "Quick farming disabled.");
			} else {
				this.enabledPlayers.add(playerName);

				sender.sendMessage(ChatColor.GOLD + "[QuickFarm] "
					+ ChatColor.DARK_GREEN + "Quick farming enabled.");
			}

			this.getConfig().set("players-enabled", this.enabledPlayers);
			this.saveConfig();
		} else {
			sender.sendMessage("Only players can use this command.");
		}


		return true;
	}

	@EventHandler
	public void onPlayerClicks(PlayerInteractEvent event) {
		Player player = event.getPlayer();
		Action action = event.getAction();
		ItemStack item = event.getItem();
		Block block = event.getClickedBlock();

		if (action.equals(Action.RIGHT_CLICK_BLOCK))
			harvestCrop(player, block);
	}

	/**
	 * Attempts harvest a crop
	 *
	 * @param player The player using the right click
	 * @param block The block about to be harvested
	 */
	private void harvestCrop(Player player, Block block) {
		ItemStack seed = this.getPlantableSeed(block);
		BlockData blockData = block.getBlockData();

		if (seed == null || !(blockData instanceof Ageable))
			return;

		Ageable blockAge = (Ageable) block.getBlockData();

		if (blockAge.getAge() != blockAge.getMaximumAge())
			return;

		// Drop all items that would normally be dropped.
		block.breakNaturally(player.getInventory().getItemInMainHand());

		// Auto-replant the crop
		blockAge.setAge(0);
		block.setBlockData(blockAge);


		// reward mcmmo xp
		if (this.mcmmo != null) {
			McMMOPlayer mcPlayer = UserManager.getPlayer(player);
			ExperienceAPI.addXpFromBlockBySkill(block.getState(), mcPlayer, PrimarySkillType.HERBALISM);
		}


	}

	/**
	 * Returns the plantable version of the given block, if one exists.
	 *
	 * @param block The block of which to get the plantable version.
	 * @return The plantable version of the given block if it exists, otherwise null.
	 */
	private ItemStack getPlantableSeed(Block block) {
		// Get the seed corresponding to the block just broken.
		switch(block.getType()) {
			case BEETROOTS:
				return new ItemStack(Material.BEETROOT_SEEDS);
			case CARROTS:
				return new ItemStack(Material.CARROT);
			case POTATOES:
				return new ItemStack(Material.POTATO);
			case WHEAT:
				return new ItemStack(Material.WHEAT_SEEDS);
			// case NEW_FARM_PLANT:
			// return new ItemStack(Material.NEW_FARM_SEED);
			default:  // Indicate no corresponding seed if "block" wasn't a valid crop.
				return null;
		}
	}

}
