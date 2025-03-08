package com.github.games647.changeskin.sponge.command;

// Import the new class that replaces CommandSpec, if applicable.
// If there is no direct replacement, the import might be removed or replaced with a new one.
// Assuming there is a new class named NewCommandSpec that replaces CommandSpec.
import org.spongepowered.api.command.spec.NewCommandSpec;

@FunctionalInterface
public interface ChangeSkinCommand {

    // Assuming the new class NewCommandSpec is a direct replacement for CommandSpec.
    NewCommandSpec buildSpec();
}