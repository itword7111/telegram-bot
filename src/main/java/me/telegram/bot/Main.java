package me.telegram.bot;

public class Main {

    private static final String BOT_TOKEN = "5417168592:AAG6-FZGMZgHv4uII3Ql34aHkEbmIqw_UCw";

    public static void main(String[] args) {
        TelegramBotApplication application = TelegramBotApplication.builder()
                .botToken(BOT_TOKEN)
                .build();

        application.run();
    }

}