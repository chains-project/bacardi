package com.github.games647.changeskin.sponge.command;

import org.spongepowered.api.command.args.CommandElement;

@FunctionalInterface
public interface ChangeSkinCommand {

    CommandElement buildSpec();
}