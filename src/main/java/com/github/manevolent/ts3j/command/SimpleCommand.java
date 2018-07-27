package com.github.manevolent.ts3j.command;

import com.github.manevolent.ts3j.command.part.CommandOption;
import com.github.manevolent.ts3j.command.part.CommandParameter;
import com.github.manevolent.ts3j.command.part.CommandSingleParameter;
import com.github.manevolent.ts3j.protocol.ProtocolRole;
import com.github.manevolent.ts3j.util.Ts3String;

import java.util.*;

public class SimpleCommand implements Command {
    private final String name;
    private final ProtocolRole role;
    private final Map<String, CommandParameter> params;

    public SimpleCommand(String name, ProtocolRole role, CommandParameter... params) {
        this(name, role, Arrays.asList(params));
    }

    public SimpleCommand(String name, ProtocolRole role, List<CommandParameter> params) {
        this.name = name;
        this.role = role;

        this.params = new LinkedHashMap<>();

        for (CommandParameter parameter : params) add(parameter);
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
        return Collections.unmodifiableCollection(params.values());
    }

    public boolean has(String key) {
        return params.containsKey(key.toLowerCase());
    }

    public void add(CommandParameter parameter) {
        CommandParameter existing = get(parameter.getName());
        if (existing != null) throw new IllegalArgumentException(parameter.getName() + " already set");

        if (parameter.getName().trim().length() <= 0)
            throw new IllegalArgumentException("invalid key (empty)");

        this.params.put(parameter.getName().toLowerCase(), parameter);
    }

    public CommandParameter get(String key) {
        return this.params.get(key.toLowerCase());
    }

    public CommandParameter remove(String key) {
        return this.params.remove(key);
    }

    public static SimpleCommand parse(ProtocolRole role, String commandText) {
        return ComplexCommand.parse(role, commandText).simplify();
    }
}
