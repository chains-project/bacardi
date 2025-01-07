package com.github.games647.changeskin.sponge.command;

// Update the import statement to the correct package where CommandSpec is located
import org.spongepowered.api.command.spec.CommandSpec; // Ensure this import is valid in the new dependency version

@FunctionalInterface
public interface ChangeSkinCommand {

    CommandSpec buildSpec();
}