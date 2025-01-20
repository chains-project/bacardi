package com.github.games647.changeskin.sponge.command;

import org.spongepowered.api.command.spec.CommandSpec; // Ensure this import is correct based on the updated dependency

@FunctionalInterface
public interface ChangeSkinCommand {

    CommandSpec buildSpec(); // This method signature remains unchanged
}