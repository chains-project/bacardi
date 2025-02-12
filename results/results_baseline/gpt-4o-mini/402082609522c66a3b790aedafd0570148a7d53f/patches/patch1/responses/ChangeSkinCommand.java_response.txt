package com.github.games647.changeskin.sponge.command;

import org.spongepowered.api.command.CommandSpec; // Updated import statement

@FunctionalInterface
public interface ChangeSkinCommand {

    CommandSpec buildSpec();
}