package com.github.games647.changeskin.sponge.command;

import org.spongepowered.api.command.CommandSource;

@FunctionalInterface
public interface ChangeSkinCommand {

    void buildSpec(CommandSource src);
}