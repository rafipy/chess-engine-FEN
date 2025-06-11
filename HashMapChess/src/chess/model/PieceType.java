package chess.model;

public enum PieceType {
    PAWN_WHITE("♙", true),
    KNIGHT_WHITE("♘", true),
    BISHOP_WHITE("♗", true),
    ROOK_WHITE("♖", true),
    QUEEN_WHITE("♕", true),
    KING_WHITE("♔", true),
    PAWN_BLACK("♟", false),
    KNIGHT_BLACK("♞", false),
    BISHOP_BLACK("♝", false),
    ROOK_BLACK("♜", false),
    QUEEN_BLACK("♛", false),
    KING_BLACK("♚", false);

    private final String symbol;
    private final boolean white;

    PieceType(String symbol, boolean white) {
        this.symbol = symbol;
        this.white = white;
    }

    public String getSymbol() {
        return symbol;
    }

    public boolean isWhite() {
        return white;
    }

    public boolean isBlack() {
        return !white;
    }
}