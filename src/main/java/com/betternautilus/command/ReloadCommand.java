package com.betternautilus.command;

import com.betternautilus.BetterNautilus;
import com.betternautilus.config.BetterNautilusConfig;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.context.CommandContext;
import net.minecraft.command.permission.Permission;
import net.minecraft.command.permission.PermissionLevel;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.text.Text;

public class ReloadCommand {

    private static final Permission REQUIRE_OP_LEVEL_2 = new Permission.Level(PermissionLevel.GAMEMASTERS);

    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
            CommandManager.literal("betternautilus")
                .requires(source -> source.getPermissions().hasPermission(REQUIRE_OP_LEVEL_2))
                .then(
                    CommandManager.literal("reload")
                        .executes(ReloadCommand::execute)
                )
        );
    }

    private static int execute(CommandContext<ServerCommandSource> ctx) {
        try {
            BetterNautilusConfig.load();
            BetterNautilusConfig cfg = BetterNautilusConfig.get();

            String summary = String.format(
                    "[BetterNautilus] Config reloaded. Recipes: %s | Enchantments: %s",
                    cfg.recipesEnabled ? "ON" : "OFF",
                    cfg.enchantmentsEnabled ? "ON" : "OFF"
            );

            BetterNautilus.LOGGER.info("{} (by {})", summary, ctx.getSource().getName());
            ctx.getSource().sendFeedback(() -> Text.literal(summary), true);
            return 1;
        } catch (Exception e) {
            BetterNautilus.LOGGER.error("[BetterNautilus] Config reload failed: {}", e.getMessage());
            ctx.getSource().sendError(
                    Text.literal("[BetterNautilus] Config reload failed: " + e.getMessage())
            );
            return 0;
        }
    }
}
