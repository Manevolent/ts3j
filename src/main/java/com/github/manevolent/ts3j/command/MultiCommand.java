package com.github.manevolent.ts3j.command;

import com.github.manevolent.ts3j.command.parameter.CommandOption;
import com.github.manevolent.ts3j.command.parameter.CommandParameter;
import com.github.manevolent.ts3j.command.parameter.CommandSingleParameter;
import com.github.manevolent.ts3j.protocol.ProtocolRole;
import com.github.manevolent.ts3j.util.Ts3String;

import java.util.*;

public class MultiCommand implements Command {
    private final String name;
    private final ProtocolRole role;
    private final List<SingleCommand> commands;

    public MultiCommand(String name, ProtocolRole role, SingleCommand... params) {
        this(name, role, Arrays.asList(params));
    }

    public MultiCommand(String name, ProtocolRole role, List<SingleCommand> params) {
        this.name = name;
        this.role = role;
        this.commands = Collections.unmodifiableList(params);
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
        throw new UnsupportedOperationException();
    }

    @Override
    public void add(CommandParameter parameter) {
        throw new UnsupportedOperationException();
    }

    public List<SingleCommand> simplify() {
        return Collections.unmodifiableList(commands);
    }

    public SingleCommand simplifyFirst() {
        return commands.stream().findFirst().orElse(null);
    }

    public SingleCommand simplifyOne() {
        if (commands.size() > 1) throw new IllegalArgumentException("ambiguous; more than one command");
        return commands.stream().findFirst().orElse(null);
    }

    @Override
    public String build() {
        StringBuilder builder = new StringBuilder();

        builder.append(getName().toLowerCase());

        List<SingleCommand> singleCommands = simplify();

        for (int i = 0; i < singleCommands.size(); i ++) {
            boolean first = i == 0;
            boolean last = i == singleCommands.size() - 1;

            if (!first) builder.append("|");

            for (CommandParameter parameter : singleCommands.get(i).getParameters()) {
                builder.append(" " + (parameter.toString()).trim());
            }
        }

        return builder.toString().trim();
    }

    public static MultiCommand parse(ProtocolRole role, String text) {
        String[] labelAndText = text.split(" ", 2);
        String label = labelAndText[0].toLowerCase();

        if (label.contains("=")) {
            label = "";
            labelAndText[1] = text; // No command label!
        }

        String[] commands = labelAndText.length > 1 ? labelAndText[1].split("\\|") : new String[0];

        List<SingleCommand> singleCommands = new LinkedList<>();

        for (String commandText : commands) {
            String[] commandParts = commandText.split(" ");

            List<CommandParameter> parameterList = new LinkedList<>();

            for (int i = 0; i < commandParts.length; i++) {
                String commandParamText = commandParts[i].trim();
                if (commandParamText.length() <= 0) continue;

                if (commandParamText.startsWith("-")) { // Option
                    parameterList.add(new CommandOption(commandParamText.substring(1)));
                } else {
                    String[] commandParamParts = commandParamText.split("=", 2);

                    parameterList.add(new CommandSingleParameter(
                                    commandParamParts[0].toLowerCase(),
                                    commandParamParts.length > 1 ?
                                            Ts3String.unescape(commandParamParts[1]) :
                                            null
                            )
                    );
                }
            }

            // Replace static variables
            SingleCommand command = new SingleCommand(label, role, parameterList);

            if (singleCommands.size() > 0) {
                for (CommandParameter parameter : singleCommands.get(0).getParameters())
                    if (!command.has(parameter.getName())) command.add(parameter);
            }

            singleCommands.add(command);
        }

        return new MultiCommand(label, role, singleCommands);
    }
}
