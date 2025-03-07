package com.github.games647.changeskin.sponge.command;

@FunctionalInterface
public interface ChangeSkinCommand {

    // Assuming the new version of the dependency has replaced CommandSpec with a new class called NewCommandSpec
    NewCommandSpec buildSpec();
}