package at.haha007.edencpe;

import at.haha007.edencommands.CommandRegistry;
import at.haha007.edencommands.SyncCommandExecutor;
import at.haha007.edencommands.argument.Completion;
import at.haha007.edencommands.argument.IntegerArgument;
import at.haha007.edencommands.argument.player.PlayerArgument;
import at.haha007.edencommands.tree.LiteralCommandNode;
import net.coreprotect.CoreProtect;
import net.coreprotect.CoreProtectAPI;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.util.TriState;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerQuitEvent;
import org.jetbrains.annotations.NotNull;

import java.util.List;

import static at.haha007.edencommands.CommandRegistry.argument;
import static at.haha007.edencommands.CommandRegistry.literal;

public class EdenCpeCommand implements Listener {
    private static final int PAGE_SIZE = 1000;
    public static final @NotNull TextComponent MUST_BE_PLAYER_MESSAGE = Component.text("This can only be done by players!", NamedTextColor.RED);
    private final EdenCPE plugin;
    private final CommandRegistry commandRegistry;
    private final PathRenderer renderer = new DisplayEntityPathRenderer();

    public EdenCpeCommand(EdenCPE plugin) {
        this.plugin = plugin;
        Bukkit.getPluginManager().registerEvents(this, plugin);
        commandRegistry = new CommandRegistry(plugin);
        LiteralCommandNode.LiteralCommandBuilder cmd = literal("coe");
        cmd.requires(p -> p.sender().hasPermission("edencpe.command.coe"));

        cmd.executor(c -> {
            if (!(c.sender() instanceof Player player)) {
                c.sender().sendMessage(MUST_BE_PLAYER_MESSAGE);
                return;
            }
            renderer.hidePath(player);
            player.sendMessage(Component.text("Cleared path", NamedTextColor.GOLD));
        });
        cmd.defaultExecutor(c -> {
            c.sender().sendMessage(Component.text("/edencpe <player> <page>", NamedTextColor.GOLD)
                    .append(Component.text(" - Shows the player's path", NamedTextColor.GRAY)));
            c.sender().sendMessage(Component.text("/edencpe <player>", NamedTextColor.GOLD)
                    .append(Component.text(" - Shows the player's path on page 1", NamedTextColor.GRAY)));
            c.sender().sendMessage(Component.text("/edencpe", NamedTextColor.GOLD)
                    .append(Component.text(" - Hides the player's path", NamedTextColor.GRAY)));
        });

        cmd.then(literal("help").executor(c -> {
            c.sender().sendMessage(Component.text("/edencpe <player> <page>", NamedTextColor.GOLD)
                    .append(Component.text(" - Shows the player's path", NamedTextColor.GRAY)));
            c.sender().sendMessage(Component.text("/edencpe <player>", NamedTextColor.GOLD)
                    .append(Component.text(" - Shows the player's path on page 1", NamedTextColor.GRAY)));
            c.sender().sendMessage(Component.text("/edencpe", NamedTextColor.GOLD)
                    .append(Component.text(" - Hides the player's path", NamedTextColor.GRAY)));
        }));

        cmd.then(argument("player", PlayerArgument.builder().exact(TriState.TRUE).build()).executor(c -> {
            if (!(c.sender() instanceof Player player)) {
                c.sender().sendMessage(MUST_BE_PLAYER_MESSAGE);
                return;
            }
            OfflinePlayer target = c.parameter("player");
            sendPage(player, target, 1);
        }).then(argument("page", IntegerArgument.builder()
                .filter(new IntegerArgument.MinimumFilter(Component.text("Page must be at least 1"), 1))
                .completion(new Completion<>(1, null))
                .completion(new Completion<>(2, null))
                .build()).executor(new SyncCommandExecutor(c -> {
            if (!(c.sender() instanceof Player player)) {
                c.sender().sendMessage(MUST_BE_PLAYER_MESSAGE);
                return;
            }
            OfflinePlayer target = c.parameter("player");
            int page = c.parameter("page");
            sendPage(player, target, page);
        }, plugin))));
        commandRegistry.register(cmd.build());
    }

    public void unregister() {
        commandRegistry.destroy();
        Bukkit.getOnlinePlayers().forEach(renderer::hidePath);
    }

    @EventHandler
    public void onPlayerQuit(PlayerQuitEvent event) {
        renderer.hidePath(event.getPlayer());
    }

    private void sendPage(Player player, OfflinePlayer target, int page) {
        String playerName = target.getName();
        if (playerName == null) {
            player.sendMessage(Component.text("Player not found", NamedTextColor.RED));
            return;
        }
        CoreProtectAPI api = CoreProtect.getInstance().getAPI();
        List<CoreProtectAPI.ParseResult> blocks = api.performLookup(Integer.MAX_VALUE,
                        List.of(playerName),
                        null,
                        null,
                        null,
                        null,
                        0,
                        null)
                .stream()
                .map(api::parseResult)
                .filter(result -> result.worldName().equals(player.getWorld().getName()))
                .filter(result -> result.getActionId() == 0) // (0=removed, 1=placed, 2=interaction)
                .toList();
        int skip = Math.max(0, blocks.size() - PAGE_SIZE);
        skip = Math.min(skip, (page - 1) * PAGE_SIZE);
        int to = skip + PAGE_SIZE;
        String msg = "Showing path for %s from %s to %s out of %s"
                .formatted(playerName, skip + 1, to, blocks.size());
        player.sendMessage(Component.text(msg, NamedTextColor.GOLD));
        List<CoreProtectAPI.ParseResult> pageBlocks = blocks.stream()
                .skip(skip)
                .limit(PAGE_SIZE).toList();
        Bukkit.getScheduler().runTask(plugin, () -> {
            try {
                renderer.hidePath(player);
                renderer.showPath(pageBlocks, player);
            } catch (Exception e) {
                player.sendMessage(Component.text("Error: " + e.getMessage(), NamedTextColor.RED));
                plugin.getSLF4JLogger().error("Error rendering path", e);
            }
        });
    }
}
