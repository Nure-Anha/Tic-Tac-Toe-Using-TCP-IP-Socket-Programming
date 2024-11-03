package TicTacToeGame;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;

public class GameRoom extends JFrame implements Runnable {

    private static int roomIdCounter = 1;
    private int id;
    private List<Player> players;
    private char[][] board;
    private boolean gameActive;
    private JButton[][] buttons;
    private char currentPlayerSymbol = 'X';
    private JLabel subtitleLabel;
    private boolean closed = false;
    private boolean closing = false;

    public GameRoom() {
        this.id = roomIdCounter++;
        this.players = new ArrayList<>();
        this.board = new char[3][3];
        this.gameActive = false;
        initializeBoard();
        initializeGUI();
    }

    public synchronized void startGame() {
        if (isReady() && !gameActive) {
            gameActive = true;
            notifyNextPlayer();
        }
    }

    private void initializeBoard() {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                board[i][j] = ' ';
            }
        }
    }

    public int getId() {
        return id;
    }

    public synchronized void addPlayer(Player player) {
        players.add(player);
        if (isReady()) {
            startGame();
        }
    }

    public synchronized boolean isFull() {
        return players.size() == 2;
    }

    public synchronized boolean isReady() {
        return isFull();
    }

    private void initializeGUI() {
        setTitle("Tic Tac Toe - Game Room " + id);
        setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
        setSize(350, 350);

        setLayout(new BorderLayout());

        subtitleLabel = new JLabel("Waiting for players to connect...", SwingConstants.CENTER);
        subtitleLabel.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 16));
        add(subtitleLabel, BorderLayout.NORTH);

        JPanel gridPanel = new JPanel(new GridLayout(3, 3));
        buttons = new JButton[3][3];
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                buttons[i][j] = new JButton();
                buttons[i][j].setFont(new Font(Font.SANS_SERIF, Font.BOLD, 30));
                buttons[i][j].setFocusPainted(false);
                int finalI = i;
                int finalJ = j;
                buttons[i][j].addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent e) {
                        handleButtonClick(finalI, finalJ);
                    }
                });
                gridPanel.add(buttons[i][j]);
            }
        }

        add(gridPanel, BorderLayout.CENTER);

        addWindowListener(new java.awt.event.WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
                handleWindowClosing();
            }
        });

        setVisible(true);
    }

    private void handleWindowClosing() {
        int option = JOptionPane.showConfirmDialog(this, "Are you sure you want to exit the game?", "Exit Game", JOptionPane.YES_NO_OPTION);
        if (option == JOptionPane.YES_OPTION) {
            closing = true;

            for (Player player : players) {
                try {
                    player.getSocket().close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            dispose();
        }
    }

    private void handleButtonClick(int row, int col) {
        if (gameActive && isValidMove(row, col)) {
            char symbol = currentPlayerSymbol;

            buttons[row][col].setText(String.valueOf(symbol));
            board[row][col] = symbol;

            if (checkWin(row, col, symbol)) {
                JOptionPane.showMessageDialog(this, "Player " + (symbol == 'X' ? "1" : "2") + " wins!");
                gameActive = false;
            } else if (isBoardFull()) {
                int option = JOptionPane.showConfirmDialog(this, "The game is a draw! Play again?", "Draw", JOptionPane.YES_NO_OPTION);
                if (option == JOptionPane.YES_OPTION) {
                    resetGame();
                } else {
                    gameActive = false;
                }
            }

            currentPlayerSymbol = (symbol == 'X') ? 'O' : 'X';

            notifyNextPlayer();
        }
    }

    private synchronized void notifyNextPlayer() {
        Player currentPlayer = players.get(0);
        Player nextPlayer = players.get(1);

        subtitleLabel.setText("Player " + (currentPlayerSymbol == 'X' ? "1" : "2") + "'s turn. Make a move.");
    }
    
    private synchronized void resetGame() {
        initializeBoard();
        currentPlayerSymbol = (currentPlayerSymbol == 'X') ? 'O' : 'X';
        clearBoardGUI();
        notifyNextPlayer();
    }

    public synchronized void close() {
        if (!closed) {
            closed = true;

            cleanup();

            SwingUtilities.invokeLater(() -> {
                dispose();
            });
        }
    }

    private void cleanup() {
        for (Player player : players) {
            try {
                player.getSocket().close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void tossAndSetFirstMove() {
        int startingPlayerIndex = (int) (Math.random() * 2);

        if (startingPlayerIndex == 0) {
            currentPlayerSymbol = 'X';
        } else {
            currentPlayerSymbol = 'O';
        }

        notifyNextPlayer();
    }
    
    private void clearBoardGUI() {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                buttons[i][j].setText("");
            }
        }
    }

    @Override
    public void run() {
        try {
            for (Player player : players) {
                player.getOutput().println("Game is starting! You are player " + (players.indexOf(player) + 1));
            }
            tossAndSetFirstMove();
            while (gameActive) {
                makeMove(players.get(1), 'O');
            }
        } finally {
            for (Player player : players) {
                try {
                    player.getSocket().close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            dispose();
        }
    }

    private void makeMove(Player player, char symbol) {
        try {
            BufferedReader input = player.getInput();

            if (!closing && !player.getSocket().isClosed()) {
                String move = input.readLine();
                if (move != null) {
                    String[] coordinates = move.split(" ");
                    int row = Integer.parseInt(coordinates[0]) - 1;
                    int col = Integer.parseInt(coordinates[1]) - 1;
                } else {
                    handleOpponentDisconnect(player);
                }
            }
        } catch (IOException e) {
            handleOpponentDisconnect(player);
        }
    }

    private void handleOpponentDisconnect(Player player) {
        System.out.println("Game room " + id + " closed.");
        gameActive = false;
        closing = true;
        close();
    }

    private boolean isValidMove(int row, int col) {
        return row >= 0 && row < 3 && col >= 0 && col < 3 && board[row][col] == ' ';
    }

    private boolean checkWin(int row, int col, char symbol) {
        return (board[row][0] == symbol && board[row][1] == symbol && board[row][2] == symbol)
                || (board[0][col] == symbol && board[1][col] == symbol && board[2][col] == symbol)
                || (row == col && board[0][0] == symbol && board[1][1] == symbol && board[2][2] == symbol)
                || (row + col == 2 && board[0][2] == symbol && board[1][1] == symbol && board[2][0] == symbol);
    }

    private boolean isBoardFull() {
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                if (board[i][j] == ' ') {
                    return false;
                }
            }
        }
        return true;
    }
}
