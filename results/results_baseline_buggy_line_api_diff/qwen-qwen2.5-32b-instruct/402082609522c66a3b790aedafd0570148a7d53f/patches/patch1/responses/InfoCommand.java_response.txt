package com.github.games647.changeskin.sponge.command;

import com.github.games647.changeskin.core.model.UserPreference;
import com.github.games647.changeskin.core.model.skin.SkinModel;
import com.github.games647.changeskin.core.shared.SkinFormatter;
import com.github.games647.changeskin.sponge.ChangeSkinSponge;
import com.github.games647.changeskin.sponge.PomData;
import com.google.inject.Inject;

import java.util.Optional;
import java.util.UUID;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.Command;
import org.spongepowered.api.command.parameter.CommandContext;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.service.permission.Subject;
import org.spongepowered.api.text.channel.MessageChannel;

public class InfoCommand implements Command, ChangeSkinCommand {

{
    @Inject
    private ChangeSkinSponge plugin;

    @Inject
    private SkinFormatter formatter;

    @Override
    public CommandResult execute(CommandContext context) throws CommandException {
        if (!(context.source() instanceof Player)) {
            plugin.sendMessage(context.source(), "no-console");
            return CommandResult.empty();
        }

        UUID uniqueId = ((Player) context.source()).uniqueId();
        Task.builder().execute(() -> {
            UserPreference preferences = plugin.getCore().getStorage().getPreferences(uniqueId);
            Task.builder().execute(() -> sendSkinDetails(uniqueId, preferences)).submit(plugin);
        }).submit(plugin);

        return CommandResult.success();
    }

    @Override
    public Command.Parameterized buildSpec() {
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

                Component component = PlainTextComponentSerializer.plain().deserialize(formatted);
                player.sendMessage(component);
            } else {
                plugin.sendMessage(player, "skin-not-found");
            }
        }
    }
}