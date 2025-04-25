import javax.swing.*;
import java.awt.*;

public class UserInterface extends JPanel {

    private final int squareSize = 64;
    private final int border = 10;
    private String[][] chessBoard;

    public UserInterface() {
        BoardGeneration.initiateStandardChess();
        chessBoard = BoardGeneration.getLastGeneratedBoard();
        setPreferredSize(new Dimension(8 * squareSize + 2 * border + 200, 8 * squareSize + 2 * border));
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        setBackground(new Color(200, 100, 0)); // orange background
        drawBorders(g);
        drawBoard(g);
        drawPieces(g);
    }

    private void drawBorders(Graphics g) {
        g.setColor(Color.BLACK);
        g.fill3DRect(0, 0, border, border, true); // top-left
        g.fill3DRect((int) (8 * squareSize) + border, 0, border, border, true); // top-right
        g.fill3DRect(0, (int) (8 * squareSize) + border, border, border, true); // bottom-left
        g.fill3DRect((int) (8 * squareSize) + border, (int) (8 * squareSize) + border, border, border, true); // bottom-right

        g.fill3DRect(0, border, border, (int) (8 * squareSize), true); // left
        g.fill3DRect((int) (8 * squareSize) + border, border, border, (int) (8 * squareSize), true); // right
        g.fill3DRect(border, 0, (int) (8 * squareSize), border, true); // top
        g.fill3DRect(border, (int) (8 * squareSize) + border, (int) (8 * squareSize), border, true); // bottom

        g.fill3DRect((int) (8 * squareSize) + 2 * border + 200, 0, border, border, true);
        g.fill3DRect((int) (8 * squareSize) + 2 * border + 200, (int) (8 * squareSize) + border, border, border, true);
        g.fill3DRect((int) (8 * squareSize) + 2 * border, 0, 200, border, true);
        g.fill3DRect((int) (8 * squareSize) + 2 * border, (int) (8 * squareSize) + border, 200, border, true);

        g.setColor(new Color(0, 100, 0)); // green strip at far right
        g.fill3DRect((int) (8 * squareSize) + 2 * border + 200, 0, border, (int) (8 * squareSize) + border, true);
    }

    private void drawBoard(Graphics g) {
        for (int i = 0; i < 64; i++) {
            int x = (i % 8) * squareSize + border;
            int y = (i / 8) * squareSize + border;

            boolean isLight = ((i % 2 == 0) ^ (i / 8 % 2 == 0));
            g.setColor(isLight ? new Color(255, 200, 100) : new Color(150, 50, 30));
            g.fillRect(x, y, squareSize, squareSize);
        }
    }

    private void drawPieces(Graphics g) {
        g.setFont(new Font("SansSerif", Font.BOLD, squareSize - 10));
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                String piece = chessBoard[row][col];
                if (!piece.equals(" ")) {
                    g.setColor(Character.isUpperCase(piece.charAt(0)) ? Color.WHITE : Color.BLACK);
                    g.drawString(piece, col * squareSize + border + 20, row * squareSize + border + 45);
                }
            }
        }
    }

    public void newGame() {
        BoardGeneration.initiateStandardChess(); // or .initiateChess960();
        chessBoard = BoardGeneration.getLastGeneratedBoard();
        repaint();
    }

    // Main method
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            JFrame frame = new JFrame("CHESS960 ENGINE");
            frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

            UserInterface ui = new UserInterface();

            // Orange panel on the right
            JPanel rightPanel = new JPanel();
            rightPanel.setPreferredSize(new Dimension(200, 0));
            rightPanel.setBackground(new Color(255, 140, 0));
            JButton newGameButton = new JButton("New Game");
            newGameButton.setPreferredSize(new Dimension(160, 40));
            newGameButton.addActionListener(e -> ui.newGame());
            rightPanel.add(newGameButton);

            // Frame layout
            frame.setLayout(new BorderLayout());
            frame.add(ui, BorderLayout.CENTER);
            frame.add(rightPanel, BorderLayout.EAST);

            frame.pack();
            frame.setLocationRelativeTo(null);
            frame.setVisible(true);
        });
    }
}
