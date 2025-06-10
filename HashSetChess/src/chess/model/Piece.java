package chess.model;

public class Piece {
    private final PieceType type;

    public Piece(PieceType type) {
        this.type = type;
    }

    public PieceType getType() {
        return type;
    }

    public String getSymbol() {
        return type.getSymbol();
    }
}
