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
import org.spongepowered.api.command.CommandCause;
import org.spongepowered.api.command.CommandExecutor;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.exception.CommandException;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.entity.living.player.Player;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class InfoCommand implements CommandExecutor, ChangeSkinCommand {

    @Inject
    private ChangeSkinSponge plugin;

    @Inject
    private SkinFormatter formatter;

    @Override
    public CommandResult execute(CommandContext context) throws CommandException {
        CommandCause cause = context.cause();
        Optional<Player> optionalPlayer = cause.first(Player.class);
        if (!optionalPlayer.isPresent()) {
            plugin.sendMessage(cause, "no-console");
            return CommandResult.builder().successCount(0).build();
        }

        Player player = optionalPlayer.get();
        UUID uniqueId = player.uniqueId();
        Sponge.server().scheduler().executor(plugin).submit(() -> {
            UserPreference preferences = plugin.getCore().getStorage().getPreferences(uniqueId);
            Sponge.server().scheduler().executor(plugin).submit(() -> sendSkinDetails(uniqueId, preferences));
        });

        return CommandResult.builder().successCount(1).build();
    }

    public Command buildSpec() {
        return Command.builder()
                .executor(this)
                .permission(PomData.ARTIFACT_ID + ".command.skininfo.base")
                .build();
    }

    private void sendSkinDetails(UUID uuid, UserPreference preference) {
        Optional<Player> optPlayer = Sponge.server().onlinePlayers().stream()
                .filter(player -> player.uniqueId().equals(uuid))
                .findFirst();
        if (optPlayer.isPresent()) {
            Player player = optPlayer.get();
            Optional<SkinModel> optSkin = preference.getTargetSkin();
            if (optSkin.isPresent()) {
                String template = plugin.getCore().getMessage("skin-info");
                String formatted = formatter.apply(template, optSkin.get());
                Component component = LegacyComponentSerializer.legacyAmpersand().deserialize(formatted);
                player.sendMessage(component);
            } else {
                plugin.sendMessage(player, "skin-not-found");
            }
        }
    }
}