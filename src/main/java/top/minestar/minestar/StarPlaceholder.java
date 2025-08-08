package top.minestar.minestar;

import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class StarPlaceholder extends PlaceholderExpansion {
    
    private final MinestarWanted plugin;
    
    public StarPlaceholder(MinestarWanted plugin) {
        this.plugin = plugin;
    }
    
    @Override
    public @NotNull String getIdentifier() {
        return "minestarwanted";
    }
    
    @Override
    public @NotNull String getAuthor() {
        return "MinestarWanted";
    }
    
    @Override
    public @NotNull String getVersion() {
        return plugin.getDescription().getVersion();
    }
    
    @Override
    public boolean persist() {
        return true;
    }
    
    @Override
    public String onPlaceholderRequest(Player player, @NotNull String params) {
        if (player == null) {
            return "";
        }
        
        switch (params.toLowerCase()) {
            case "stars":
                int stars = plugin.getPlayerStars(player.getUniqueId());
                return String.valueOf(stars);
                
            case "stars_display":
                int starCount = plugin.getPlayerStars(player.getUniqueId());
                return plugin.getStarDisplay(starCount);
                
            case "max_stars":
                return String.valueOf(plugin.getConfig().getInt("stars.max_stars", 5));
                
            case "star_emoji":
                return plugin.getConfig().getString("stars.star_emoji", "⭐");
                
            case "star_color":
                return plugin.getConfig().getString("stars.star_color", "RED");
                
            default:
                return null;
        }
    }
}
