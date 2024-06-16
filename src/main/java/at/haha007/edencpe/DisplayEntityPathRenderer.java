package at.haha007.edencpe;

import net.coreprotect.CoreProtectAPI;
import org.bukkit.*;
import org.bukkit.block.data.BlockData;
import org.bukkit.entity.BlockDisplay;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.util.Transformation;

import java.util.*;

public class DisplayEntityPathRenderer implements PathRenderer {
    Map<UUID, Set<UUID>> shownDisplays = new HashMap<>();

    @Override
    public void showPath(List<CoreProtectAPI.ParseResult> path, Player player) {
        hidePath(player);
        for (CoreProtectAPI.ParseResult block : path) {
            spawnDisplay(block, player);
        }
    }

    @Override
    public void hidePath(Player player) {
        Set<UUID> displays = shownDisplays.remove(player.getUniqueId());
        if (displays == null) return;
        for (UUID uuid : displays) {
            Entity e = Bukkit.getEntity(uuid);
            if (e == null) continue;
            e.remove();
        }
    }

    private void spawnDisplay(CoreProtectAPI.ParseResult result, Player player) {
        double x = result.getX();
        double y = result.getY();
        double z = result.getZ();
        World world = player.getWorld();
        Location location = new Location(world, x, y, z);
        BlockDisplay display = world.spawn(location, BlockDisplay.class);
        display.setVisibleByDefault(false);
        display.setPersistent(false);
        display.setGlowing(true);
        display.setGlowColorOverride(Color.RED);
        BlockData blockData;
        try {
            blockData = result.getBlockData();
        } catch (Exception e) {
            blockData = Material.STRUCTURE_BLOCK.createBlockData();
            EdenCPE.getInstance().getSLF4JLogger().debug("Failed to create block data for {},{},{}", x, y, z);
        }
        display.setBlock(blockData);
        Transformation transformation = display.getTransformation();
        transformation.getTranslation().set(0.25, 0.25, 0.25);
        transformation.getScale().set(0.5f, 0.5f, 0.5f);
        display.setTransformation(transformation);
        player.showEntity(EdenCPE.getInstance(), display);
        shownDisplays.computeIfAbsent(player.getUniqueId(), k -> new HashSet<>()).add(display.getUniqueId());
    }
}
