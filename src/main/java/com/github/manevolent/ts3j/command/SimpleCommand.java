package com.github.manevolent.ts3j.command;

import com.github.manevolent.ts3j.command.part.CommandParameter;
import com.github.manevolent.ts3j.protocol.ProtocolRole;

import java.util.Collection;
import java.util.Collections;

public class SimpleCommand implements Command {
    private final String name;
    private final ProtocolRole role;
    private final Collection<CommandParameter> params;

    public SimpleCommand(String name, ProtocolRole role, Collection<CommandParameter> params) {
        this.name = name;
        this.role = role;
        this.params = params;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public ProtocolRole getRole() {
        return role;
    }

    @Override
    public Iterable<CommandParameter> getParameters() {
        return Collections.unmodifiableCollection(params);
    }
}
