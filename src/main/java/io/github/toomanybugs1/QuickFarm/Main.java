package io.github.toomanybugs1.QuickFarm;

import java.util.ArrayList;
import java.util.List;

import org.bukkit.plugin.java.JavaPlugin;

import com.gmail.nossr50.mcMMO;
import com.gmail.nossr50.api.ExperienceAPI;
import com.gmail.nossr50.datatypes.player.McMMOPlayer;
import com.gmail.nossr50.datatypes.skills.PrimarySkillType;
import com.gmail.nossr50.util.player.UserManager;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.inventory.ItemStack;

public final class Main extends JavaPlugin implements Listener {

    List<String> enabledPlayers;
    mcMMO mcmmo;

    @SuppressWarnings("unchecked")
    @Override
    public void onEnable() {
        Bukkit.getPluginManager().registerEvents(this, this);

        this.saveDefaultConfig();

        enabledPlayers = (List<String>) this.getConfig().getList("players-enabled");
        if (enabledPlayers == null) {
            enabledPlayers = new ArrayList<>();
            getLogger().info("Player list is null.");
            getLogger().info(enabledPlayers.toString());
        }

        mcmmo = (mcMMO) Bukkit.getServer().getPluginManager().getPlugin("mcMMO");
        if (mcmmo == null) getLogger().info("This server does not have mcMMO. Disabling mcMMO features.");
        else getLogger().info("mcMMO has been detected!");

        getLogger().info("QuickFarm has been initialized.");
    }

    @Override
    public void onDisable() {
        getLogger().info("QuickFarm has been disabled.");
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {

        if (cmd.getName().equalsIgnoreCase("togglequickfarm")) {
            if (args.length != 0) {
                sender.sendMessage("Usage: /togglequickfarm");
                return false;
            } else if (sender instanceof Player) {
                //get latest config
                if (this.getConfig().getList("players-enabled") != null)
                    enabledPlayers = (List<String>) this.getConfig().getList("players-enabled");

                Player player = (Player) sender;
                //remove returns true if the name is in the list
                if (!enabledPlayers.remove(player.getName())) {
                    enabledPlayers.add(player.getName());
                    sender.sendMessage("§6[QuickFarm] §2Quick farming enabled.");
                } else {
                    sender.sendMessage("§6[QuickFarm] §4Quick farming disabled.");
                }

                this.getConfig().set("players-enabled", enabledPlayers);
                this.saveConfig();

                return true;

            } else {
                sender.sendMessage("Only players can use this command.");
                return false;
            }
        }

        return false;
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
    	//some events were passing null blocks (specifically when using super breaker in mcmmo)
    	if (event != null) {
        Player player = event.getPlayer();
        if (enabledPlayers.contains(player.getName()))  // Ensure the player has permission to use this plugin.
            tryAutoReplant(event, player);
    	}
    }

    /**
     * Attempts to automatically replant a crop from seeds in the player's inventory.
     * 
     * Fails under any of the following conditions:
     * <ul>
     *     <li>the block broken wasn't a crop</li>
     *     <li>the player doesn't have the corresponding seeds in his/her inventory.</li>
     * </ul>
     * @param event The event containing information about the block that was broken
     * @param player The player whose inventory will be searched for an auto-plantable seed.
     */
    private void tryAutoReplant(BlockBreakEvent event, Player player) {
        Block block = event.getBlock();
        ItemStack seed = getPlantableSeed(block);  // If the broken block wasn't a crop, "seed" will be null, ...
        
        //we need to make sure seed is not null because most blocks do not have blockdata that
        //can be casted to Ageable
        if (seed != null) {
	        Ageable age = (Ageable) block.getBlockData();
	
	        if (player.getInventory().containsAtLeast(seed, 1)) {  // ... so the "contains" check will fail.
	            event.setCancelled(true);
	
	            // Drop all items that would normally be dropped.
	            List<ItemStack> drops = (List<ItemStack>) block.getDrops(new ItemStack(Material.IRON_HOE), player);
	            for (ItemStack drop : drops)
	                player.getWorld().dropItemNaturally(block.getLocation(), drop);
	
	            // Auto-replant the crop
	            Block b = block.getLocation().getBlock();  // QUESTION: Is this not just getting a copy of itself?
	            b.setType(block.getType());                // If so, then these first two lines can be removed...
	            Ageable newBlockAge = (Ageable) b.getBlockData();  // ... and here "b" would be replaced by "block"
	            newBlockAge.setAge(0);
	
	            // Update the player's inventory to reflect the use of the seed during auto-replanting.
	            for (int i = 0; i < player.getInventory().getSize(); i++) {
	                ItemStack itm = player.getInventory().getItem(i);
	                if (itm != null && itm.getType().equals(seed.getType())) {  // Find the item we just planted,
	                    itm.setAmount(itm.getAmount() - 1);  // decrement the item's amount
	                    player.getInventory().setItem(i, itm.getAmount() > 0 ? itm : null);  // Remove item if amt == 0
	                    player.updateInventory();  // update the player's inventory
	                    break;  // Once found and updated, no need to continue looping through the inventory.
	                }
	            }
	
	            // mcmmo should only reward xp if the crop is fully grown
	            if (mcmmo != null && age.getAge() == age.getMaximumAge()) {
	                McMMOPlayer mcPlayer = UserManager.getPlayer(player);
	                ExperienceAPI.addXpFromBlockBySkill(block.getState(), mcPlayer, PrimarySkillType.HERBALISM);
	            }
	        }
        }
    }

	/**
	 * Returns the plantable version of the given block, if one exists.
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
