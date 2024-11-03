package TicTacToeGame;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

public class Server {

    private static final int PORT = 8888;
    private static List<GameRoom> gameRooms = new ArrayList<>();
    private static ServerSocket serverSocket;

    public static void main(String[] args) {
        try {
            serverSocket = new ServerSocket(PORT);
            System.out.println("Server is running and waiting for players...");

            while (true) {
                Socket playerSocket = serverSocket.accept();
                System.out.println("New player connected: " + playerSocket);

                GameRoom gameRoom = getAvailableGameRoom();
                gameRoom.addPlayer(new Player(playerSocket));

                if (gameRoom.isReady()) {
                    startGame(gameRoom);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static GameRoom getAvailableGameRoom() {
        for (GameRoom gameRoom : gameRooms) {
            if (!gameRoom.isFull()) {
                return gameRoom;
            }
        }
        GameRoom newGameRoom = new GameRoom();
        gameRooms.add(newGameRoom);
        return newGameRoom;
    }

    private static void startGame(GameRoom gameRoom) {
        System.out.println("Game started in room: " + gameRoom.getId());
        Thread gameThread = new Thread(gameRoom);
        gameThread.start();
    }
}
