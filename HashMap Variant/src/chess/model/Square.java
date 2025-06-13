package chess.model;

import javax.swing.*;
import java.awt.*;

public class Square extends JPanel {
    private Piece piece;
    private final int row, col;
    private Color defaultColor;

    public Square(int row, int col) {
        this.row = row;
        this.col = col;
        this.defaultColor = (row + col) % 2 == 0 ? Color.WHITE : new Color(180, 180, 180);
        setBackground(defaultColor);
        setPreferredSize(new Dimension(50, 50));
        setLayout(new GridBagLayout());
    }

    public void setPiece(Piece piece) {
        this.piece = piece;
        removeAll();
        if (piece != null) {
            JLabel pieceLabel = new JLabel(piece.getSymbol());
            pieceLabel.setFont(new Font("Serif", Font.PLAIN, 36));
            add(pieceLabel);
        }
        revalidate();
        repaint();
    }

    public Piece getPiece() {
        return piece;
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
    }
    @Override
    public void setBackground(Color bg) {
        super.setBackground(bg);
        // Only update default color if we're not setting a highlight color
        if (!bg.equals(Color.YELLOW)) {
            this.defaultColor = bg;
        }
    }

    public void resetBackground() {
        setBackground(defaultColor);
    }
}