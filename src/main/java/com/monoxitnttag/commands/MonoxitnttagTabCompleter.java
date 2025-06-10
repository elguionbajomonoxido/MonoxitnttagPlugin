package com.monoxitnttag.commands;

import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class MonoxitnttagTabCompleter implements TabCompleter {
    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (args.length == 1) {
            List<String> subcommands = Arrays.asList("start", "stop", "reset", "reload");
            List<String> completions = new ArrayList<>();
            for (String sub : subcommands) {
                if (sub.startsWith(args[0].toLowerCase())) {
                    completions.add(sub);
                }
            }
            return completions;
        }
        if (args.length == 2 && args[0].equalsIgnoreCase("start")) {
            // Sugerir cantidad de jugadores
            return Collections.singletonList("1");
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("start")) {
            // Sugerir formato de tiempo
            return Collections.singletonList("1:00");
        }
        return Collections.emptyList();
    }
}

