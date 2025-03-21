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
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.CommandMapping;
import org.spongepowered.api.command.CommandCompletion;
import org.spongepowered.api.command.CommandCallable;
import org.spongepowered.api.command.CommandException;
import org.spongepowered.api.command.CommandSpec;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.scheduler.Task;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.serializer.TextSerializers;

public class InfoCommand implements CommandCallable, ChangeSkinCommand {

    @Inject
    private ChangeSkinSponge plugin;

    @Inject
    private SkinFormatter formatter;

    @Override
    public CommandResult process(CommandSource src, String arguments) throws CommandException {
        if (!(src instanceof Player)) {
            plugin.sendMessage(src, "no-console");
            return CommandResult.empty();
        }

        UUID uniqueId = ((Player) src).getUniqueId();
        Task.builder().async()
                .execute(() -> {
                    UserPreference preferences = plugin.getCore().getStorage().getPreferences(uniqueId);
                    Task.builder().execute(() -> sendSkinDetails(uniqueId, preferences)).submit(plugin);
                })
                .submit(plugin);

        return CommandResult.success();
    }

    @Override
    public List<String> getSuggestions(CommandSource src, String arguments, CommandContext context, TextChannel channel) throws CommandException {
        return new ArrayList<>();
    }

    @Override
    public boolean testPermission(CommandSource src) {
        return src.hasPermission(PomData.ARTIFACT_ID + ".command.skininfo.base");
    }

    @Override
    public CommandSpec getSpec() {
        return CommandSpec.builder()
                .executor(this)
                .permission(PomData.ARTIFACT_ID + ".command.skininfo.base")
                .build();
    }

    private void sendSkinDetails(UUID uuid, UserPreference preference) {
        Optional<Player> optPlayer = Sponge.getServer().getPlayer(uuid);
        if (optPlayer.isPresent()) {
            Player player = optPlayer.get();

            Optional<SkinModel> optSkin = preference.getTargetSkin();
            if (optSkin.isPresent()) {
                String template = plugin.getCore().getMessage("skin-info");
                String formatted = formatter.apply(template, optSkin.get());

                Text text = TextSerializers.LEGACY_FORMATTING_CODE.deserialize(formatted);
                player.sendMessage(text);
            } else {
                plugin.sendMessage(player, "skin-not-found");
            }
        }
    }
}