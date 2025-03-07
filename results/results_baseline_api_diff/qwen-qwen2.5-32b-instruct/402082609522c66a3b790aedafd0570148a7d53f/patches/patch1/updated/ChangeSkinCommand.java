package com.github.games647.changeskin.sponge.command;

import org.spongepowered.api.command.CommandMapping;

@FunctionalInterface
public interface ChangeSkinCommand {

    CommandMapping buildSpec();
}