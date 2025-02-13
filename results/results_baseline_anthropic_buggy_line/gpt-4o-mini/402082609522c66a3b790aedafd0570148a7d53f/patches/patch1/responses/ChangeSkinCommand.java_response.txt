package com.github.games647.changeskin.sponge.command;

import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.command.spec.CommandSpec.Builder; // Importing the Builder class if needed

@FunctionalInterface
public interface ChangeSkinCommand {

    CommandSpec buildSpec();
}