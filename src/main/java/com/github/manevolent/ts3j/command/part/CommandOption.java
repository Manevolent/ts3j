package com.github.manevolent.ts3j.command.part;

import com.github.manevolent.ts3j.enums.CommandPartType;

public class CommandOption implements CommandParameter {
    private final String value;

    public CommandOption(String value) {
        this.value = " -" + value;
    }

    @Override
    public CommandPartType getType() {
        return CommandPartType.OPTION;
    }

}
