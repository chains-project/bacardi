package com.github.games647.changeskin.sponge.command;

import org.spongepowered.api.command.spec.CommandSpec; // This import will be removed
import org.spongepowered.api.command.spec.CommandSpecBuilder; // New import for the updated API

@FunctionalInterface
public interface ChangeSkinCommand {

    default CommandSpec buildSpec() { // Changed to default method to avoid overriding
        return new CommandSpecBuilder().build(); // Updated to use CommandSpecBuilder
    }
}