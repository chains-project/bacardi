package com.github.games647.changeskin.sponge.command;

import org.spongepowered.api.command.spec.CommandSpecBuilder;

@FunctionalInterface
public interface ChangeSkinCommand {

    CommandSpecBuilder buildSpec();
}