package net.avicus.scaffold;

import com.google.common.base.Preconditions;
import com.sk89q.bukkit.util.CommandsManagerRegistration;
import com.sk89q.minecraft.util.commands.*;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.plugin.java.JavaPlugin;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class Scaffold extends JavaPlugin implements TabCompleter {
    private static Scaffold instance;
    private CommandsManager<CommandSender> commands;
    private Map<ScaffoldWorld, Long> locked = new HashMap<>();

    public static Scaffold instance() {
        return instance;
    }

    @Override
    public void onEnable() {
        instance = this;

        getServer().getPluginManager().registerEvents(new ScaffoldListener(), this);

        this.commands = new CommandsManager<CommandSender>() {
            @Override
            public boolean hasPermission(CommandSender sender, String perm) {
                return sender instanceof ConsoleCommandSender || sender.hasPermission(perm);
            }
        };

        CommandsManagerRegistration cmds = new CommandsManagerRegistration(this, this.commands);
        cmds.register(ScaffoldCommands.class);

        getServer().getScheduler().scheduleSyncRepeatingTask(this, new Runnable() {
            @Override
            public void run() {
                for (ScaffoldWorld wrapper : locked.keySet()) {
                    if (wrapper.isOpen())
                        wrapper.getWorld().get().setFullTime(locked.get(wrapper));
                }
            }
        }, 0, 20);
    }

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
        try {
            this.commands.execute(cmd.getName(), args, sender, sender);
        } catch (CommandPermissionsException e) {
            sender.sendMessage(ChatColor.RED + "You don't have permission.");
        } catch (MissingNestedCommandException e) {
            sender.sendMessage(ChatColor.RED + e.getUsage());
        } catch (CommandUsageException e) {
            sender.sendMessage(ChatColor.RED + e.getMessage());
            sender.sendMessage(ChatColor.RED + e.getUsage());
        } catch (WrappedCommandException e) {
            if (e.getCause() instanceof NumberFormatException) {
                sender.sendMessage(ChatColor.RED + "Number expected, string received instead.");
            } else {
                sender.sendMessage(ChatColor.RED + "An error has occurred. See console.");
                e.printStackTrace();
            }
        } catch (CommandException e) {
            sender.sendMessage(ChatColor.RED + e.getMessage());
        }

        return true;
    }

    public void sync(Runnable runnable) {
        getServer().getScheduler().runTask(this, runnable);
    }

    public void async(Runnable runnable) {
        getServer().getScheduler().runTaskAsynchronously(this, runnable);
    }

    public boolean toggleLock(ScaffoldWorld wrapper) {
        Preconditions.checkArgument(wrapper.isOpen(), "World not open.");
        Iterator<ScaffoldWorld> iterator = this.locked.keySet().iterator();
        while (iterator.hasNext()) {
            ScaffoldWorld next = iterator.next();
            if (next.getName().equals(wrapper.getName())) {
                iterator.remove();
                return false;
            }
        }
        locked.put(wrapper, wrapper.getWorld().get().getFullTime());
        return true;
    }
}
