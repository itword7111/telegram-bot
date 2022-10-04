package me.telegram.bot;

public enum Command {
    START("/start"),
    QUIT_CASHED_COMMAND("/quit"),
    GET_GROUPS("/get_groups"),
    ADD_GROUP("/add_group"),
    ADD_REPORT("/add_report");

    private final String command;

    private Command(String command)
    {
        this.command = command;
    }

    public String getCommand() {
        return command;
    }
}
