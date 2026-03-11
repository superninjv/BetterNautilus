package com.betternautilus.command;

import com.betternautilus.BetterNautilusCommon;
import com.betternautilus.config.BetterNautilusConfig;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.permissions.Permission;
import net.minecraft.server.permissions.PermissionLevel;

public class ReloadCommand {

    public static void register(CommandDispatcher<CommandSourceStack> dispatcher) {
        dispatcher.register(
            Commands.literal("betternautilus")
                .requires(source -> source.permissions().hasPermission(new Permission.HasCommandLevel(PermissionLevel.GAMEMASTERS)))
                .then(
                    Commands.literal("reload")
                        .executes(ReloadCommand::execute)
                )
        );
    }

    private static int execute(CommandContext<CommandSourceStack> ctx) {
        try {
            BetterNautilusConfig.load();
            BetterNautilusConfig cfg = BetterNautilusConfig.get();

            String summary = String.format(
                    "[BetterNautilus] Config reloaded. Recipes: %s | Enchantments: %s",
                    cfg.recipesEnabled ? "ON" : "OFF",
                    cfg.enchantmentsEnabled ? "ON" : "OFF"
            );

            BetterNautilusCommon.LOGGER.info("{} (by {})", summary, ctx.getSource().getTextName());
            ctx.getSource().sendSuccess(() -> Component.literal(summary), true);
            return 1;
        } catch (Exception e) {
            BetterNautilusCommon.LOGGER.error("[BetterNautilus] Config reload failed: {}", e.getMessage());
            ctx.getSource().sendFailure(
                    Component.literal("[BetterNautilus] Config reload failed: " + e.getMessage())
            );
            return 0;
        }
    }
}
