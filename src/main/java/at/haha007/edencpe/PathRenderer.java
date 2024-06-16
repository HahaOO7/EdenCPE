package at.haha007.edencpe;

import net.coreprotect.CoreProtectAPI;
import org.bukkit.entity.Player;

import java.util.List;

public interface PathRenderer {
    public void showPath(List<CoreProtectAPI.ParseResult> path, Player player);
    public void hidePath(Player player);
}
