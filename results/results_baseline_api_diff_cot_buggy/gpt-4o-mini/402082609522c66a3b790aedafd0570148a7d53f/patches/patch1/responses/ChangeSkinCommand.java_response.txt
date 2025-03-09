package com.github.games647.changeskin.sponge.command;

import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.args.CommandContext;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.command.spec.CommandSpec.Builder;

@FunctionalInterface
public interface ChangeSkinCommand {

    CommandSpec buildSpec();
}