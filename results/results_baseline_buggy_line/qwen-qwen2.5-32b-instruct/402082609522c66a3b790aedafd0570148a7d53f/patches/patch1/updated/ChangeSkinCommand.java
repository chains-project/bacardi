package com.github.games647.changeskin.sponge.command;

import org.checkerframework.checker.units.qual.A;

@FunctionalInterface
public interface ChangeSkinCommand {

    A buildSpec();
}