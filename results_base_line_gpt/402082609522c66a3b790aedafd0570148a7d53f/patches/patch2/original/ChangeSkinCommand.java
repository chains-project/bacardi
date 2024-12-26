package com.github.games647.changeskin.sponge.command;

// Update the import statement to match the new package structure
import org.spongepowered.api.command.CommandSpec; // Corrected import

@FunctionalInterface
public interface ChangeSkinCommand {

    CommandSpec buildSpec();
}