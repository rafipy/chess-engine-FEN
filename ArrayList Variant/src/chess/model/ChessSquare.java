package chess.model;

import javax.swing.*;
import java.awt.*;

public class ChessSquare extends JPanel {
    private Piece piece;
    private final int row, col;

    public ChessSquare(int row, int col) {
        this.row = row;
        this.col = col;
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
}