package at.haha007.edencpe;

import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

public final class EdenCPE extends JavaPlugin implements Listener {

    private EdenCpeCommand command;
    private static Plugin instance;

    public static Plugin getInstance() {
        return instance;
    }


    @Override
    public void onEnable() {
        instance = this;
        command = new EdenCpeCommand(this);
    }



    @Override
    public void onDisable() {
        command.unregister();
    }
}
