
package TicTacToeGame;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class TicTacToeClient {

    public static void main(String[] args) {
        String serverAddress = "localhost"; 
        int serverPort = 8888; 

        try (Socket socket = new Socket(serverAddress, serverPort);
             BufferedReader serverInput = new BufferedReader(new InputStreamReader(socket.getInputStream()));
             PrintWriter serverOutput = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader userInput = new BufferedReader(new InputStreamReader(System.in))) {

            System.out.println("Connected to the Tic Tac Toe server!\nWait for another player!");

            
            Thread serverListener = new Thread(() -> {
                try {
                    while (true) {
                        String message = serverInput.readLine();
                        if (message == null) {
                            break;
                        }
                        System.out.println(message);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
            serverListener.start();

            
            String userInputMessage;
            while (true) {
                userInputMessage = userInput.readLine();
                if (userInputMessage.equalsIgnoreCase("exit")) {
                    break;
                }
                serverOutput.println(userInputMessage);
            }

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

