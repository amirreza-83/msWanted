package top.minestar.minestar;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;

import java.util.ArrayList;
import java.util.List;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class MinestarWanted extends JavaPlugin implements TabCompleter, Listener {
    
    private DataManager dataManager;
    private FileConfiguration config;
    private Map<UUID, Integer> playerStars;
    
    @Override
    public void onEnable() {
        saveDefaultConfig();
        config = getConfig();
        
        dataManager = new DataManager(this);
        playerStars = new HashMap<>();
        
        dataManager.loadData();
        
        getServer().getPluginManager().registerEvents(this, this);
        
        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new StarPlaceholder(this).register();
            getLogger().info("PlaceholderAPI expansion registered successfully!");
        } else {
            getLogger().warning("PlaceholderAPI not found! Placeholders will not work.");
        }
        
        getCommand("wanted").setTabCompleter(this);
        getCommand("zendan").setTabCompleter(this);
        
        getLogger().info("MinestarWanted plugin has been enabled!");
    }
    
    @Override
    public void onDisable() {
        dataManager.saveData();
        getLogger().info("MinestarWanted plugin has been disabled!");
    }
    
    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equalsIgnoreCase("zendan")) {
            return handleZendanCommand(sender, args);
        } else if (command.getName().equalsIgnoreCase("wanted")) {
            return handleWantedCommand(sender, args);
        }
        return false;
    }
    
    private boolean handleZendanCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("minestarwanted.zendan")) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                config.getString("messages.no_permission", "§cYou don't have permission to use this command!")));
            return true;
        }
        
        if (args.length != 1) {
            sender.sendMessage(ChatColor.RED + "Usage: /zendan <player>");
            return true;
        }
        
        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                config.getString("messages.player_not_found", "§cPlayer not found!")));
            return true;
        }
        
        int targetStars = getPlayerStars(target.getUniqueId());
        if (targetStars <= 0) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                config.getString("messages.player_not_wanted", "§c{player} doesn't have any wanted stars!")
                .replace("{player}", target.getName())));
            return true;
        }
        
        if (sender instanceof Player) {
            Player player = (Player) sender;
            int maxDistance = config.getInt("zendan.max_distance", 10);
            
            if (player.getLocation().distance(target.getLocation()) > maxDistance) {
                sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                    config.getString("messages.player_too_far", "§c{player} is too far away! Maximum distance: {distance} blocks")
                    .replace("{player}", target.getName())
                    .replace("{distance}", String.valueOf(maxDistance))));
                return true;
            }
        }
        
        for (String cmd : config.getStringList("zendan.commands")) {
            String executedCmd = cmd.replace("{player}", target.getName());
            Bukkit.dispatchCommand(Bukkit.getConsoleSender(), executedCmd);
        }
        
        if (config.getBoolean("zendan.clear_stars_on_jail", true)) {
            setPlayerStars(target.getUniqueId(), 0);
        }
        
        sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
            config.getString("messages.player_sent_to_jail", "§a{player} has been successfully sent to jail!")
            .replace("{player}", target.getName())));
        
        return true;
    }
    
    private boolean handleWantedCommand(CommandSender sender, String[] args) {
        if (!sender.hasPermission("minestarwanted.admin")) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                config.getString("messages.no_permission", "§cYou don't have permission to use this command!")));
            return true;
        }
        
        if (args.length == 0) {
            sender.sendMessage(ChatColor.GOLD + "=== MinestarWanted Commands ===");
            sender.sendMessage(ChatColor.YELLOW + "/wanted add <player> <amount> - Add stars to player");
            sender.sendMessage(ChatColor.YELLOW + "/wanted remove <player> <amount> - Remove stars from player");
            sender.sendMessage(ChatColor.YELLOW + "/wanted set <player> <amount> - Set player's stars");
            sender.sendMessage(ChatColor.YELLOW + "/wanted check <player> - Check player's stars");
            sender.sendMessage(ChatColor.YELLOW + "/wanted reload - Reload configuration");
            return true;
        }
        
        String subCommand = args[0].toLowerCase();
        
        switch (subCommand) {
            case "help":
                sender.sendMessage(ChatColor.GOLD + "=== MinestarWanted Commands ===");
                sender.sendMessage(ChatColor.YELLOW + "/wanted add <player> <amount> - Add stars to player");
                sender.sendMessage(ChatColor.YELLOW + "/wanted remove <player> <amount> - Remove stars from player");
                sender.sendMessage(ChatColor.YELLOW + "/wanted set <player> <amount> - Set player's stars");
                sender.sendMessage(ChatColor.YELLOW + "/wanted check <player> - Check player's stars");
                sender.sendMessage(ChatColor.YELLOW + "/wanted reload - Reload configuration");
                break;
                

                
            case "reload":
                reloadConfig();
                config = getConfig();
                sender.sendMessage(ChatColor.GREEN + "Configuration reloaded!");
                break;
                
            case "add":
            case "remove":
            case "set":
            case "check":
                if (args.length < 2) {
                    sender.sendMessage(ChatColor.RED + "Usage: /wanted " + subCommand + " <player> [amount]");
                    return true;
                }
                
                Player target = Bukkit.getPlayer(args[1]);
                if (target == null) {
                    sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                        config.getString("messages.player_not_found", "§cPlayer not found!")));
                    return true;
                }
                
                UUID playerUUID = target.getUniqueId();
                int currentStars = getPlayerStars(playerUUID);
                
                switch (subCommand) {
                    case "add":
                        if (args.length < 3) {
                            sender.sendMessage(ChatColor.RED + "Usage: /wanted add <player> <amount>");
                            return true;
                        }
                        try {
                            int amount = Integer.parseInt(args[2]);
                            int newStars = Math.min(currentStars + amount, config.getInt("stars.max_stars", 5));
                            setPlayerStars(playerUUID, newStars);
                            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                                config.getString("messages.stars_updated", "§a{player}'s stars have been updated to {stars}!")
                                .replace("{player}", target.getName())
                                .replace("{stars}", String.valueOf(newStars))));
                        } catch (NumberFormatException e) {
                            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                                config.getString("messages.invalid_amount", "§cInvalid amount!")));
                        }
                        break;
                        
                    case "remove":
                        if (args.length < 3) {
                            sender.sendMessage(ChatColor.RED + "Usage: /wanted remove <player> <amount>");
                            return true;
                        }
                        try {
                            int amount = Integer.parseInt(args[2]);
                            int newStars = Math.max(currentStars - amount, 0);
                            setPlayerStars(playerUUID, newStars);
                            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                                config.getString("messages.stars_updated", "§a{player}'s stars have been updated to {stars}!")
                                .replace("{player}", target.getName())
                                .replace("{stars}", String.valueOf(newStars))));
                        } catch (NumberFormatException e) {
                            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                                config.getString("messages.invalid_amount", "§cInvalid amount!")));
                        }
                        break;
                        
                    case "set":
                        if (args.length < 3) {
                            sender.sendMessage(ChatColor.RED + "Usage: /wanted set <player> <amount>");
                            return true;
                        }
                        try {
                            int amount = Integer.parseInt(args[2]);
                            int maxStars = config.getInt("stars.max_stars", 5);
                            if (amount < 0 || amount > maxStars) {
                                sender.sendMessage(ChatColor.RED + "Stars must be between 0 and " + maxStars + "!");
                                return true;
                            }
                            setPlayerStars(playerUUID, amount);
                            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                                config.getString("messages.stars_updated", "§a{player}'s stars have been updated to {stars}!")
                                .replace("{player}", target.getName())
                                .replace("{stars}", String.valueOf(amount))));
                        } catch (NumberFormatException e) {
                            sender.sendMessage(ChatColor.translateAlternateColorCodes('&', 
                                config.getString("messages.invalid_amount", "§cInvalid amount!")));
                        }
                        break;
                        
                    case "check":
                        sender.sendMessage(ChatColor.GREEN + target.getName() + " has " + currentStars + " stars.");
                        break;
                }
                break;
                
            default:
                sender.sendMessage(ChatColor.RED + "Unknown subcommand. Use /wanted help for help.");
                break;
        }
        
        return true;
    }
    
    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!config.getBoolean("wanted_system.enabled", true)) {
            return;
        }
        
        Player killer = event.getEntity().getKiller();
        if (killer == null) return;

        Player victim = event.getEntity();
        UUID killerUUID = killer.getUniqueId();
        UUID victimUUID = victim.getUniqueId();
        
        int killerStars = getPlayerStars(killerUUID);
        int victimStars = getPlayerStars(victimUUID);
        
        if (victimStars > 0) {
            setPlayerStars(victimUUID, 0);
            setPlayerStars(killerUUID, 0);
            
            String message = ChatColor.translateAlternateColorCodes('&', 
                config.getString("messages.both_lost_wanted", "§a{player1} and {player2} both lost their wanted status!")
                .replace("{player1}", killer.getName())
                .replace("{player2}", victim.getName()));
            
            if (config.getBoolean("wanted_system.broadcast_messages", true)) {
                Bukkit.broadcastMessage(message);
            }
            
            killer.sendMessage(ChatColor.GREEN + "You killed " + victim.getName() + " and both lost wanted status!");
            victim.sendMessage(ChatColor.RED + "You were killed by " + killer.getName() + " and both lost wanted status!");
            
        } else {
            int starsToAdd = config.getInt("wanted_system.stars_per_kill", 1);
            int newKillerStars = Math.min(killerStars + starsToAdd, config.getInt("stars.max_stars", 5));
            setPlayerStars(killerUUID, newKillerStars);
            
            String message = ChatColor.translateAlternateColorCodes('&', 
                config.getString("messages.killer_got_wanted", "§c{player} now has {stars} wanted stars!")
                .replace("{player}", killer.getName())
                .replace("{stars}", String.valueOf(newKillerStars)));
            
            if (config.getBoolean("wanted_system.broadcast_messages", true)) {
                Bukkit.broadcastMessage(message);
            }
            
            killer.sendMessage(ChatColor.RED + "You killed " + victim.getName() + " and now have " + newKillerStars + " wanted stars!");
            victim.sendMessage(ChatColor.GREEN + "You were killed by " + killer.getName() + " who now has " + newKillerStars + " wanted stars!");
        }
    }
    
    public int getPlayerStars(UUID playerUUID) {
        return playerStars.getOrDefault(playerUUID, 0);
    }
    
    public void setPlayerStars(UUID playerUUID, int stars) {
        playerStars.put(playerUUID, stars);
        dataManager.saveData();
    }
    
    public Map<UUID, Integer> getPlayerStars() {
        return playerStars;
    }
    
    public void setPlayerStars(Map<UUID, Integer> stars) {
        this.playerStars = stars;
    }
    
    public String getStarDisplay(int stars) {
        if (stars <= 0) return "";
        
        String emoji = config.getString("stars.star_emoji", "⭐");
        String color = config.getString("stars.star_color", "RED");
        ChatColor chatColor = ChatColor.valueOf(color.toUpperCase());
        
        StringBuilder display = new StringBuilder();
        for (int i = 0; i < stars; i++) {
            display.append(chatColor).append(emoji);
        }
        
        return display.toString();
    }
    
    public DataManager getDataManager() {
        return dataManager;
    }
    
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> completions = new ArrayList<>();
        
        if (command.getName().equalsIgnoreCase("wanted")) {
            if (!sender.hasPermission("minestarwanted.admin")) {
                return completions;
            }
            
            if (args.length == 1) {
                List<String> subCommands = new ArrayList<>();
                subCommands.add("add");
                subCommands.add("remove");
                subCommands.add("set");
                subCommands.add("check");
                subCommands.add("reload");
                subCommands.add("help");
                
                String input = args[0].toLowerCase();
                for (String subCommand : subCommands) {
                    if (subCommand.startsWith(input)) {
                        completions.add(subCommand);
                    }
                }
            } else if (args.length == 2) {
                String input = args[1].toLowerCase();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    if (player.getName().toLowerCase().startsWith(input)) {
                        completions.add(player.getName());
                    }
                }
            } else if (args.length == 3) {
                String subCommand = args[0].toLowerCase();
                if (subCommand.equals("add") || subCommand.equals("remove") || subCommand.equals("set")) {
                    String input = args[2].toLowerCase();
                    
                    if (subCommand.equals("set")) {
                        int maxStars = config.getInt("stars.max_stars", 5);
                        for (int i = 0; i <= maxStars; i++) {
                            String num = String.valueOf(i);
                            if (num.startsWith(input)) {
                                completions.add(num);
                            }
                        }
                    } else {
                        String[] commonAmounts = {"1", "2", "3", "4", "5"};
                        for (String amount : commonAmounts) {
                            if (amount.startsWith(input)) {
                                completions.add(amount);
                            }
                        }
                    }
                }
            }
        } else if (command.getName().equalsIgnoreCase("zendan")) {
            if (!sender.hasPermission("minestarwanted.zendan")) {
                return completions;
            }
            
            if (args.length == 1) {
                String input = args[0].toLowerCase();
                for (Player player : Bukkit.getOnlinePlayers()) {
                    int playerStars = getPlayerStars(player.getUniqueId());
                    if (playerStars > 0 && player.getName().toLowerCase().startsWith(input)) {
                        completions.add(player.getName());
                    }
                }
            }
        }
        
        return completions;
    }
}
