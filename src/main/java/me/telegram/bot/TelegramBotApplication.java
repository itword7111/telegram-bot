package me.telegram.bot;

import com.google.gson.Gson;
import com.pengrad.telegrambot.TelegramBot;
import com.pengrad.telegrambot.UpdatesListener;
import com.pengrad.telegrambot.model.Message;
import com.pengrad.telegrambot.model.Update;
import com.pengrad.telegrambot.request.SendMessage;
import com.pengrad.telegrambot.request.SetWebhook;
import me.telegram.bot.model.Group;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.*;

public class TelegramBotApplication extends TelegramBot {
    private Map<String, BotState> botStateCash = new HashMap<>();
    private Map<String, Integer> groupCash = new HashMap<>();
    private Gson gson=new Gson();

    @lombok.Builder
    public TelegramBotApplication(String botToken) {
        super(botToken);
    }

    void run() {
        this.setUpdatesListener(updates -> {
            updates.forEach(this::process);
            return UpdatesListener.CONFIRMED_UPDATES_ALL;
        });
    }

    private void process(Update update) {
        Message message = update.message();
        if (message != null) {
            String text = message.text();
            Optional.ofNullable(text)
                    .ifPresent(commandName -> this.serveCommand(commandName, message.chat().id(),message.from().username()));
        }
    }

    private void serveCommand(String commandName, Long chatId, String userName)  {
        BotState botState = botStateCash.get(userName);
        if(commandName.equals("/quit")){
            botStateCash.remove(userName);
            groupCash.remove(userName);
        }
        else if (botState!=null){
            executeCashedCommand(botState, commandName, userName);
        }
        else {
            executeCommand(commandName, chatId, userName);
        }

    }
    private String RouteRequest(String requestMethod, String requestUrl,Object object,String userName){
        try {
            requestUrl=requestUrl+"username="+userName;
            URL url = new URL(requestUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod(requestMethod);
            String jsonInputString=gson.toJson(object);
            if(!jsonInputString.equals("null")) {
                connection.setDoOutput(true);
                OutputStream os = connection.getOutputStream();
                byte[] input = jsonInputString.getBytes("utf-8");
                os.write(input, 0, input.length);
                os.close();
            }

            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
            String inputLine;
            StringBuilder responseFromRouter = new StringBuilder();

            while ((inputLine = in.readLine()) != null) {
                responseFromRouter.append(inputLine);
            }
            in.close();
            return responseFromRouter.toString();
        }
        catch (IOException e){
            e.printStackTrace();
            return "";
        }
    }

    private void executeCashedCommand(BotState botState, String parameter, String userName){
        switch (botState){
            //for user
            case ADD_TASK:{
                String responseFromRouter=RouteRequest("PUT","http://localhost:8080/comandServise_war/report?",null, userName);
            }
            case ADD_GROUP_NAME:{
                String responseFromRouter=RouteRequest("POST","http://localhost:8080/comandServise_war/group?",parameter, userName);//
            }
            case ADD_USER_TO_GROUP:{
                String responseFromRouter=RouteRequest("PUT","http://localhost:8080/comandServise_war/group?",new Object[] {parameter,groupCash.get(userName),"add"}, userName);//
            }
            case DELETE_USER_FROM_GROUP:{
                String responseFromRouter=RouteRequest("PUT","http://localhost:8080/comandServise_war/group?",new Object[] {parameter,groupCash.get(userName),"delete"}, userName);//
            }
            case GET_GROUP:{
                String responseFromRouter=RouteRequest("GET","http://localhost:8080/comandServise_war/group?group="+parameter+"&",null, userName);//
                Integer groupId =gson.fromJson(responseFromRouter,Integer.class);
                groupCash.put(userName,groupId);
                botStateCash.remove(userName);
            }
        }
    }
    private void executeCommand(String commandName, Long chatId, String userName){
        switch (commandName) {
            case "/start": {
                String responseFromRouter=RouteRequest("GET","http://localhost:8080/comandServise_war/commandService?",null, userName);
                String role =gson.fromJson(responseFromRouter,String.class);
                if(role.equals("admin")){
                    SendMessage response = new SendMessage(chatId, "Ваша роль admin\n" +
                            "Доступные вам команды:\n" +
                            "/get_groups\n" +
                            "/add_group\n" +
                            "для редактирования, удаления, подробной информации о группе,\n" +
                            " воспользуйтесь сначала командой /get_group_list");
                    this.execute(response);
                }
                else if(responseFromRouter.equals("user")){
                    SendMessage response = new SendMessage(chatId,"Ваша роль user\n" +
                            "Доступные вам команды:\n" +
                            "/add_report\n");
                    this.execute(response);
                }
                else {
                    SendMessage response = new SendMessage(chatId,"Пользователь не найден");
                    this.execute(response);
                }
                break;
            }
            case "/get_groups": {
                String responseFromRouter=RouteRequest("GET","http://localhost:8080/comandServise_war/groups?",null, userName);
                List<Group> groups =gson.fromJson(responseFromRouter, ArrayList.class);
                SendMessage response = new SendMessage(chatId, "Запрос групп");
                this.execute(response);
                break;
            }
            case "/get_users": {
                String responseFromRouter=RouteRequest("GET","http://localhost:8080/comandServise_war/users?",null, userName);
                SendMessage response = new SendMessage(chatId, "Запрос групп");
                this.execute(response);
                break;
            }
            case "/get_group": {
                botStateCash.put(userName,BotState.GET_GROUP);
                SendMessage response = new SendMessage(chatId, "Введите название группы");
                this.execute(response);
                break;
            }
            case "/add_group": {
                botStateCash.put(userName,BotState.ADD_GROUP_NAME);
                SendMessage response = new SendMessage(chatId, "Введите название группы");
                this.execute(response);
                break;
            }
            case "/delete_group": {
                String responseFromRouter=RouteRequest("DELETE","http://localhost:8080/comandServise_war/group?",groupCash.get(userName), userName);
                SendMessage response = new SendMessage(chatId, "Введите название группы");
                this.execute(response);
                break;
            }
            // case for user
            case "/add_report": {
                botStateCash.put(userName,BotState.ADD_TASK);
                String responseFromRouter=RouteRequest("POST","http://localhost:8080/comandServise_war/report?",null, userName);
                SendMessage response = new SendMessage(chatId, "Введите название отчета");
                this.execute(response);
                break;
            }
            default: {
                SendMessage response = new SendMessage(chatId, "Команда не найдена");
                this.execute(response);
                break;
            }
        }
    }


}
