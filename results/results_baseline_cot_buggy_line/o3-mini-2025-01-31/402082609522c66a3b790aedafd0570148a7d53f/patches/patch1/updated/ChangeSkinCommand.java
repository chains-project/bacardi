package com.github.games647.changeskin.sponge.command;

import org.spongepowered.api.command.Command;

@FunctionalInterface
public interface ChangeSkinCommand {

    CommandSpec buildSpec();

    interface CommandSpec extends Command.Parameterized {
    }
}