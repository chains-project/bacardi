package com.github.games647.changeskin.sponge.command;

import com.github.games647.changeskin.core.model.UserPreference;
import com.github.games647.changeskin.core.model.skin.SkinModel;
import com.github.games647.changeskin.core.shared.SkinFormatter;
import com.github.games647.changeskin.sponge.ChangeSkinSponge;
import com.github.games647.changeskin.sponge.PomData;
import com.google.inject.Inject;
import java.util.Optional;
import java.util.UUID;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.CommandExecutor;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.command.source.CommandSource;
import org.spongepowered.api.entity.living.player.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class InfoCommand implements CommandExecutor, ChangeSkinCommand {

    @Inject
    private ChangeSkinSponge plugin;

    @Inject
    private SkinFormatter formatter;

    @Override
    public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
        if (!(src instanceof Player)) {
            plugin.sendMessage(src, "no-console");
            return CommandResult.empty();
        }

        UUID uniqueId = ((Player) src).getUniqueId();
        Sponge.getScheduler().asyncExecutor(plugin).execute(() -> {
            UserPreference preferences = plugin.getCore().getStorage().getPreferences(uniqueId);
            Sponge.getScheduler().syncExecutor(plugin).execute(() -> sendSkinDetails(uniqueId, preferences));
        });

        return CommandResult.success();
    }

    public Command buildSpec() {
        return Command.builder()
                .executor(this)
                .permission(PomData.ARTIFACT_ID + ".command.skininfo.base")
                .build();
    }

    private void sendSkinDetails(UUID uuid, UserPreference preference) {
        Optional<Player> optPlayer = Sponge.server().player(uuid);
        if (optPlayer.isPresent()) {
            Player player = optPlayer.get();
            Optional<SkinModel> optSkin = preference.getTargetSkin();
            if (optSkin.isPresent()) {
                String template = plugin.getCore().getMessage("skin-info");
                String formatted = formatter.apply(template, optSkin.get());
                Component message = LegacyComponentSerializer.legacySection().deserialize(formatted);
                player.sendMessage(message);
            } else {
                plugin.sendMessage(player, "skin-not-found");
            }
        }
    }

    public static class CommandResult {

        public static CommandResult empty() {
            return new CommandResult();
        }

        public static CommandResult success() {
            return new CommandResult();
        }
    }
}