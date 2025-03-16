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
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.command.parameter.Parameter;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.scheduler.Task;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class InfoCommand implements Command, ChangeSkinCommand {

    @Inject
    private ChangeSkinSponge plugin;

    @Inject
    private SkinFormatter formatter;

    @Override
    public CommandResult execute(CommandContext context) {
        Optional<Player> optPlayer = context.cause().first(Player.class);
        if (!optPlayer.isPresent()) {
            plugin.sendMessage(context.cause().audience(), "no-console");
            return CommandResult.success();
        }

        Player player = optPlayer.get();
        UUID uniqueId = player.uniqueId();
        Task.builder().execute(() -> {
            UserPreference preferences = plugin.getCore().getStorage().getPreferences(uniqueId);
            sendSkinDetails(uniqueId, preferences);
        }).submit(plugin);

        return CommandResult.success();
    }

    @Override
    public Command.Builder buildSpec() {
        return Command.builder()
                .executionRequirements(cause -> cause.hasPermission(PomData.ARTIFACT_ID + ".command.skininfo.base"))
                .executor(this);
    }

    private void sendSkinDetails(UUID uuid, UserPreference preference) {
        Optional<Player> optPlayer = Sponge.server().player(uuid);
        if (optPlayer.isPresent()) {
            Player player = optPlayer.get();

            Optional<SkinModel> optSkin = preference.getTargetSkin();
            if (optSkin.isPresent()) {
                String template = plugin.getCore().getMessage("skin-info");
                String formatted = formatter.apply(template, optSkin.get());

                Component text = LegacyComponentSerializer.legacySection().deserialize(formatted);
                player.sendMessage(text);
            } else {
                plugin.sendMessage(player, "skin-not-found");
            }
        }
    }
}