package com.github.games647.changeskin.sponge.command;

import org.spongepowered.api.command.CommandExecutor;

@FunctionalInterface
public interface ChangeSkinCommand {

    CommandExecutor buildExecutor();
}