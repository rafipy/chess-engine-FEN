import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.*;
import java.nio.file.*;
import javax.swing.JOptionPane;

public class ChessApplication extends JFrame {
    private JPanel chessBoard;
    private JTextField fenTextField;
    private JButton importButton, exportButton, prevButton, nextButton;
    private JLabel turnLabel;
    private List<List<ChessSquare>> squares = new ArrayList<>();
    private Piece selectedPiece = null;
    private int selectedRow = -1, selectedCol = -1;
    private boolean isWhiteTurn = true;
    private List<Integer> enPassantTarget = null;
    private boolean whiteKingMoved = false;
    private boolean blackKingMoved = false;
    private List<Boolean> whiteRooksMoved = new ArrayList<>();
    private List<Boolean> blackRooksMoved = new ArrayList<>();
    private ArrayList<String> moveHistory = new ArrayList<>();
    private int currentMoveIndex = -1;
    private JSpinner historySpinner;
    private JButton jumpButton;
    private JButton exportHistoryButton;
    private JButton importHistoryButton;

    private static final Map<Character, PieceType> fenToPiece = new HashMap<>();
    static {
        fenToPiece.put('P', PieceType.PAWN_WHITE);
        fenToPiece.put('N', PieceType.KNIGHT_WHITE);
        fenToPiece.put('B', PieceType.BISHOP_WHITE);
        fenToPiece.put('R', PieceType.ROOK_WHITE);
        fenToPiece.put('Q', PieceType.QUEEN_WHITE);
        fenToPiece.put('K', PieceType.KING_WHITE);
        fenToPiece.put('p', PieceType.PAWN_BLACK);
        fenToPiece.put('n', PieceType.KNIGHT_BLACK);
        fenToPiece.put('b', PieceType.BISHOP_BLACK);
        fenToPiece.put('r', PieceType.ROOK_BLACK);
        fenToPiece.put('q', PieceType.QUEEN_BLACK);
        fenToPiece.put('k', PieceType.KING_BLACK);
    }

    private static final Map<PieceType, Character> pieceToFen = new HashMap<>();
    static {
        pieceToFen.put(PieceType.PAWN_WHITE, 'P');
        pieceToFen.put(PieceType.KNIGHT_WHITE, 'N');
        pieceToFen.put(PieceType.BISHOP_WHITE, 'B');
        pieceToFen.put(PieceType.ROOK_WHITE, 'R');
        pieceToFen.put(PieceType.QUEEN_WHITE, 'Q');
        pieceToFen.put(PieceType.KING_WHITE, 'K');
        pieceToFen.put(PieceType.PAWN_BLACK, 'p');
        pieceToFen.put(PieceType.KNIGHT_BLACK, 'n');
        pieceToFen.put(PieceType.BISHOP_BLACK, 'b');
        pieceToFen.put(PieceType.ROOK_BLACK, 'r');
        pieceToFen.put(PieceType.QUEEN_BLACK, 'q');
        pieceToFen.put(PieceType.KING_BLACK, 'k');
    }

    public ChessApplication() {
        setTitle("Java Chess with Move History");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Initialize chess board
        chessBoard = new JPanel(new GridLayout(8, 8));
        chessBoard.setPreferredSize(new Dimension(400, 400));

        // Create chess squares
        for (int row = 0; row < 8; row++) {
            List<ChessSquare> rowList = new ArrayList<>();
            for (int col = 0; col < 8; col++) {
                ChessSquare square = new ChessSquare(row, col);
                square.setBackground((row + col) % 2 == 0 ? Color.WHITE : new Color(180, 180, 180));
                square.setOpaque(true);
                square.setBorder(BorderFactory.createLineBorder(Color.BLACK));

                final int r = row, c = col;
                square.addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        handleSquareClick(r, c);
                    }
                });

                rowList.add(square);
                chessBoard.add(square);
            }
            squares.add(rowList);
        }

        // Initialize rook moved status
        whiteRooksMoved.add(false); // queenside
        whiteRooksMoved.add(false); // kingside
        blackRooksMoved.add(false); // queenside
        blackRooksMoved.add(false); // kingside

        // Create control panel
        JPanel controlPanel = new JPanel(new BorderLayout());

        // FEN controls
        fenTextField = new JTextField("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");
        importButton = new JButton("Import FEN");
        exportButton = new JButton("Export FEN");
        prevButton = new JButton("← Previous");
        nextButton = new JButton("Next →");

        importButton.addActionListener(e -> importFEN());
        exportButton.addActionListener(e -> exportFEN());
        prevButton.addActionListener(e -> navigateMoveHistory(-1));
        nextButton.addActionListener(e -> navigateMoveHistory(1));

        // Turn indicator
        turnLabel = new JLabel("White's turn", SwingConstants.CENTER);
        turnLabel.setFont(new Font("Serif", Font.BOLD, 16));
        turnLabel.setOpaque(true);
        turnLabel.setBackground(Color.WHITE);
        turnLabel.setBorder(BorderFactory.createLineBorder(Color.BLACK));

        // History navigation controls
        historySpinner = new JSpinner(new SpinnerNumberModel(0, 0, 0, 1));
        jumpButton = new JButton("Jump to Move");
        exportHistoryButton = new JButton("Export History");
        importHistoryButton = new JButton("Import History");

        jumpButton.addActionListener(e -> jumpToMove());
        exportHistoryButton.addActionListener(e -> exportHistoryToFile());
        importHistoryButton.addActionListener(e -> importHistoryFromFile());

        JPanel historyControlPanel = new JPanel();
        historyControlPanel.add(new JLabel("Move:"));
        historyControlPanel.add(historySpinner);
        historyControlPanel.add(jumpButton);
        historyControlPanel.add(exportHistoryButton);
        historyControlPanel.add(importHistoryButton);

        // Button panels
        JPanel fenButtonPanel = new JPanel();
        fenButtonPanel.add(importButton);
        fenButtonPanel.add(exportButton);

        JPanel historyPanel = new JPanel();
        historyPanel.add(prevButton);
        historyPanel.add(nextButton);

        JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.add(historyControlPanel, BorderLayout.NORTH);
        buttonPanel.add(fenButtonPanel, BorderLayout.CENTER);
        buttonPanel.add(historyPanel, BorderLayout.SOUTH);
        buttonPanel.add(turnLabel, BorderLayout.AFTER_LAST_LINE);

        controlPanel.add(fenTextField, BorderLayout.CENTER);
        controlPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(chessBoard, BorderLayout.CENTER);
        add(controlPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);

        // Initialize board with starting position
        importFEN();
        updateTurnIndicator();
        updateNavigationButtons();
    }

    private void jumpToMove() {
        int index = (Integer)historySpinner.getValue();
        if (index >= 0 && index < moveHistory.size()) {
            currentMoveIndex = index;
            fenTextField.setText(moveHistory.get(currentMoveIndex));
            importFEN();
            updateNavigationButtons();
        }
    }

    private void exportHistoryToFile() {
        try {
            String desktopPath = System.getProperty("user.home") + "/Desktop/chess_history.txt";
            Path file = Paths.get(desktopPath);

            Files.write(file, moveHistory, StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
            JOptionPane.showMessageDialog(this, "History exported to Desktop/chess_history.txt");
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error exporting history: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void importHistoryFromFile() {
        try {
            String desktopPath = System.getProperty("user.home") + "/Desktop/chess_history.txt";
            Path file = Paths.get(desktopPath);

            if (Files.exists(file)) {
                List<String> importedHistory = Files.readAllLines(file);
                if (!importedHistory.isEmpty()) {
                    moveHistory = new ArrayList<>(importedHistory);
                    currentMoveIndex = moveHistory.size() - 1;
                    historySpinner.setModel(new SpinnerNumberModel(
                            currentMoveIndex, 0, Math.max(0, moveHistory.size() - 1), 1));
                    fenTextField.setText(moveHistory.get(currentMoveIndex));
                    importFEN();
                    updateNavigationButtons();
                    JOptionPane.showMessageDialog(this, "History imported successfully");
                }
            } else {
                JOptionPane.showMessageDialog(this, "No history file found on Desktop",
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error importing history: " + ex.getMessage(),
                    "Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void navigateMoveHistory(int direction) {
        int newIndex = currentMoveIndex + direction;
        if (newIndex >= 0 && newIndex < moveHistory.size()) {
            currentMoveIndex = newIndex;
            fenTextField.setText(moveHistory.get(currentMoveIndex));
            importFEN();
            updateNavigationButtons();
        }
    }

    private void addToMoveHistory() {
        if (currentMoveIndex < moveHistory.size() - 1) {
            moveHistory.subList(currentMoveIndex + 1, moveHistory.size()).clear();
        }

        String currentFEN = generateFEN();
        moveHistory.add(currentFEN);
        currentMoveIndex = moveHistory.size() - 1;
        historySpinner.setModel(new SpinnerNumberModel(
                currentMoveIndex, 0, Math.max(0, moveHistory.size() - 1), 1));
        updateNavigationButtons();
    }

    private void updateNavigationButtons() {
        prevButton.setEnabled(currentMoveIndex > 0);
        nextButton.setEnabled(currentMoveIndex < moveHistory.size() - 1);
    }

    private String generateFEN() {
        StringBuilder fen = new StringBuilder();

        // Piece placement
        for (int row = 0; row < 8; row++) {
            int emptyCount = 0;
            for (int col = 0; col < 8; col++) {
                Piece piece = squares.get(row).get(col).getPiece();
                if (piece == null) {
                    emptyCount++;
                } else {
                    if (emptyCount > 0) {
                        fen.append(emptyCount);
                        emptyCount = 0;
                    }
                    fen.append(pieceToFen.get(piece.getType()));
                }
            }
            if (emptyCount > 0) {
                fen.append(emptyCount);
            }
            if (row < 7) {
                fen.append("/");
            }
        }

        // Active color
        fen.append(isWhiteTurn ? " w " : " b ");

        // Castling availability
        StringBuilder castling = new StringBuilder();
        if (!whiteKingMoved) {
            if (!whiteRooksMoved.get(1)) castling.append("K");
            if (!whiteRooksMoved.get(0)) castling.append("Q");
        }
        if (!blackKingMoved) {
            if (!blackRooksMoved.get(1)) castling.append("k");
            if (!blackRooksMoved.get(0)) castling.append("q");
        }
        fen.append(castling.length() > 0 ? castling.toString() : "-");

        // En passant
        if (enPassantTarget != null) {
            char colChar = (char) ('a' + enPassantTarget.get(1));
            int rowNum = 8 - enPassantTarget.get(0);
            fen.append(" ").append(colChar).append(rowNum).append(" ");
        } else {
            fen.append(" - ");
        }

        // Halfmove clock and fullmove number (simplified)
        fen.append("0 1");

        return fen.toString();
    }

    private void handleSquareClick(int row, int col) {
        if (selectedPiece == null) {
            // Select a piece
            Piece clickedPiece = squares.get(row).get(col).getPiece();
            if (clickedPiece != null && ((isWhiteTurn && clickedPiece.getType().isWhite()) ||
                    (!isWhiteTurn && clickedPiece.getType().isBlack()))) {
                selectedPiece = clickedPiece;
                selectedRow = row;
                selectedCol = col;
                squares.get(row).get(col).setBackground(Color.YELLOW);
            }
        } else {
            // Attempt to move the selected piece
            if (isValidMove(selectedRow, selectedCol, row, col)) {
                Piece movedPiece = squares.get(selectedRow).get(selectedCol).getPiece();

                // Handle castling
                if (movedPiece.getType() == PieceType.KING_WHITE || movedPiece.getType() == PieceType.KING_BLACK) {
                    if (movedPiece.getType().isWhite()) {
                        whiteKingMoved = true;
                    } else {
                        blackKingMoved = true;
                    }

                    if (Math.abs(selectedCol - col) == 2) { // Castling move
                        boolean kingside = col > selectedCol;
                        int rookCol = kingside ? 7 : 0;
                        int newRookCol = kingside ? col - 1 : col + 1;

                        squares.get(row).get(newRookCol).setPiece(squares.get(row).get(rookCol).getPiece());
                        squares.get(row).get(rookCol).setPiece(null);

                        if (movedPiece.getType().isWhite()) {
                            whiteRooksMoved.set(kingside ? 1 : 0, true);
                        } else {
                            blackRooksMoved.set(kingside ? 1 : 0, true);
                        }
                    }
                }

                // Handle pawn moves
                if (movedPiece.getType() == PieceType.PAWN_WHITE || movedPiece.getType() == PieceType.PAWN_BLACK) {
                    handlePawnMove(selectedRow, selectedCol, row, col);
                }

                // Move the piece
                squares.get(selectedRow).get(selectedCol).setPiece(null);
                squares.get(row).get(col).setPiece(selectedPiece);

                // Handle pawn promotion
                if ((selectedPiece.getType() == PieceType.PAWN_WHITE && row == 0) ||
                        (selectedPiece.getType() == PieceType.PAWN_BLACK && row == 7)) {
                    squares.get(row).get(col).setPiece(new Piece(
                            selectedPiece.getType().isWhite() ? PieceType.QUEEN_WHITE : PieceType.QUEEN_BLACK));
                }

                // Switch turns and update history
                isWhiteTurn = !isWhiteTurn;
                updateTurnIndicator();
                addToMoveHistory();
            }

            // Reset selection
            squares.get(selectedRow).get(selectedCol).setBackground(
                    (selectedRow + selectedCol) % 2 == 0 ? Color.WHITE : new Color(180, 180, 180));
            selectedPiece = null;
            selectedRow = -1;
            selectedCol = -1;
        }
    }

    private void updateTurnIndicator() {
        turnLabel.setText(isWhiteTurn ? "White's turn" : "Black's turn");
        turnLabel.setBackground(isWhiteTurn ? Color.WHITE : Color.BLACK);
        turnLabel.setForeground(isWhiteTurn ? Color.BLACK : Color.WHITE);
    }

    private boolean isValidMove(int fromRow, int fromCol, int toRow, int toCol) {
        Piece piece = squares.get(fromRow).get(fromCol).getPiece();
        if (piece == null) return false;

        Piece targetPiece = squares.get(toRow).get(toCol).getPiece();
        if (targetPiece != null && targetPiece.getType().isWhite() == piece.getType().isWhite()) {
            return false;
        }

        switch (piece.getType()) {
            case KING_WHITE:
            case KING_BLACK:
                if (Math.abs(fromCol - toCol) == 2 && fromRow == toRow) {
                    return isValidCastling(fromRow, fromCol, toRow, toCol);
                }
                return Math.abs(fromRow - toRow) <= 1 && Math.abs(fromCol - toCol) <= 1;
            case QUEEN_WHITE:
            case QUEEN_BLACK:
                return isValidQueenMove(fromRow, fromCol, toRow, toCol);
            case ROOK_WHITE:
            case ROOK_BLACK:
                return isValidRookMove(fromRow, fromCol, toRow, toCol);
            case BISHOP_WHITE:
            case BISHOP_BLACK:
                return isValidBishopMove(fromRow, fromCol, toRow, toCol);
            case KNIGHT_WHITE:
            case KNIGHT_BLACK:
                return isValidKnightMove(fromRow, fromCol, toRow, toCol);
            case PAWN_WHITE:
            case PAWN_BLACK:
                return isValidPawnMove(fromRow, fromCol, toRow, toCol);
            default:
                return false;
        }
    }

    private boolean isValidCastling(int fromRow, int fromCol, int toRow, int toCol) {
        boolean isWhite = squares.get(fromRow).get(fromCol).getPiece().getType().isWhite();
        if ((isWhite && whiteKingMoved) || (!isWhite && blackKingMoved)) return false;
        if (isKingInCheck(isWhite)) return false;

        boolean kingside = toCol > fromCol;
        int rookCol = kingside ? 7 : 0;
        if ((isWhite && whiteRooksMoved.get(kingside ? 1 : 0))) return false;
        if (!isWhite && blackRooksMoved.get(kingside ? 1 : 0)) return false;

        // Check path is clear
        int start = Math.min(fromCol, rookCol) + 1;
        int end = Math.max(fromCol, rookCol);
        for (int col = start; col < end; col++) {
            if (squares.get(fromRow).get(col).getPiece() != null) return false;
        }

        // Check path is safe
        int step = kingside ? 1 : -1;
        for (int col = fromCol; col != toCol; col += step) {
            if (isSquareUnderAttack(fromRow, col, !isWhite)) return false;
        }

        return true;
    }

    private boolean isKingInCheck(boolean isWhite) {
        int kingRow = -1, kingCol = -1;
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Piece piece = squares.get(row).get(col).getPiece();
                if (piece != null && ((isWhite && piece.getType() == PieceType.KING_WHITE) ||
                        (!isWhite && piece.getType() == PieceType.KING_BLACK))) {
                    kingRow = row;
                    kingCol = col;
                    break;
                }
            }
            if (kingRow != -1) break;
        }
        return isSquareUnderAttack(kingRow, kingCol, !isWhite);
    }

    private boolean isSquareUnderAttack(int row, int col, boolean byWhite) {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece piece = squares.get(r).get(c).getPiece();
                if (piece != null && piece.getType().isWhite() == byWhite) {
                    if (isValidMoveForCheck(r, c, row, col)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private boolean isValidMoveForCheck(int fromRow, int fromCol, int toRow, int toCol) {
        Piece piece = squares.get(fromRow).get(fromCol).getPiece();
        if (piece == null) return false;

        switch (piece.getType()) {
            case PAWN_WHITE:
                return (Math.abs(fromCol - toCol) == 1 && fromRow - toRow == 1);
            case PAWN_BLACK:
                return (Math.abs(fromCol - toCol) == 1 && toRow - fromRow == 1);
            case KNIGHT_WHITE:
            case KNIGHT_BLACK:
                return (Math.abs(fromRow - toRow) == 2 && Math.abs(fromCol - toCol) == 1) ||
                        (Math.abs(fromRow - toRow) == 1 && Math.abs(fromCol - toCol) == 2);
            case BISHOP_WHITE:
            case BISHOP_BLACK:
                return isValidBishopMove(fromRow, fromCol, toRow, toCol);
            case ROOK_WHITE:
            case ROOK_BLACK:
                return isValidRookMove(fromRow, fromCol, toRow, toCol);
            case QUEEN_WHITE:
            case QUEEN_BLACK:
                return isValidQueenMove(fromRow, fromCol, toRow, toCol);
            case KING_WHITE:
            case KING_BLACK:
                return Math.abs(fromRow - toRow) <= 1 && Math.abs(fromCol - toCol) <= 1;
            default:
                return false;
        }
    }

    private boolean isValidQueenMove(int fromRow, int fromCol, int toRow, int toCol) {
        return isValidRookMove(fromRow, fromCol, toRow, toCol) ||
                isValidBishopMove(fromRow, fromCol, toRow, toCol);
    }

    private boolean isValidRookMove(int fromRow, int fromCol, int toRow, int toCol) {
        if (fromRow != toRow && fromCol != toCol) return false;

        int rowStep = Integer.compare(toRow, fromRow);
        int colStep = Integer.compare(toCol, fromCol);

        int currentRow = fromRow + rowStep;
        int currentCol = fromCol + colStep;

        while (currentRow != toRow || currentCol != toCol) {
            if (squares.get(currentRow).get(currentCol).getPiece() != null) {
                return false;
            }
            currentRow += rowStep;
            currentCol += colStep;
        }
        return true;
    }

    private boolean isValidKnightMove(int fromRow, int fromCol, int toRow, int toCol) {
        int rowDiff = Math.abs(toRow - fromRow);
        int colDiff = Math.abs(toCol - fromCol);
        return (rowDiff == 2 && colDiff == 1) || (rowDiff == 1 && colDiff == 2);
    }

    private boolean isValidBishopMove(int fromRow, int fromCol, int toRow, int toCol) {
        if (Math.abs(fromRow - toRow) != Math.abs(fromCol - toCol)) return false;

        int rowStep = toRow > fromRow ? 1 : -1;
        int colStep = toCol > fromCol ? 1 : -1;

        int currentRow = fromRow + rowStep;
        int currentCol = fromCol + colStep;

        while (currentRow != toRow || currentCol != toCol) {
            if (squares.get(currentRow).get(currentCol).getPiece() != null) {
                return false;
            }
            currentRow += rowStep;
            currentCol += colStep;
        }
        return true;
    }

    private boolean isValidPawnMove(int fromRow, int fromCol, int toRow, int toCol) {
        Piece pawn = squares.get(fromRow).get(fromCol).getPiece();
        boolean isWhite = pawn.getType().isWhite();
        int direction = isWhite ? -1 : 1;

        // Forward move
        if (fromCol == toCol && squares.get(toRow).get(toCol).getPiece() == null) {
            if (toRow == fromRow + direction) return true;
            if ((isWhite && fromRow == 6) || (!isWhite && fromRow == 1)) {
                return toRow == fromRow + 2 * direction &&
                        squares.get(fromRow + direction).get(fromCol).getPiece() == null;
            }
        }

        // Capture
        if (Math.abs(fromCol - toCol) == 1 && toRow == fromRow + direction) {
            if (squares.get(toRow).get(toCol).getPiece() != null) return true;
            // En passant
            if (enPassantTarget != null && toRow == enPassantTarget.get(0) && toCol == enPassantTarget.get(1)) {
                squares.get(fromRow).get(toCol).setPiece(null);
                return true;
            }
        }

        return false;
    }

    private void handlePawnMove(int fromRow, int fromCol, int toRow, int toCol) {
        enPassantTarget = null;
        if (Math.abs(toRow - fromRow) == 2) {
            enPassantTarget = new ArrayList<>();
            enPassantTarget.add(fromRow + (toRow > fromRow ? 1 : -1));
            enPassantTarget.add(fromCol);
        }
    }

    private void importFEN() {
        String fen = fenTextField.getText().trim();
        String[] parts = fen.split(" ");
        if (parts.length < 1) return;

        // Clear the board
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                squares.get(row).get(col).setPiece(null);
            }
        }

        // Parse piece placement
        String[] ranks = parts[0].split("/");
        for (int row = 0; row < 8; row++) {
            String rank = ranks[row];
            int col = 0;
            for (int i = 0; i < rank.length(); i++) {
                char c = rank.charAt(i);
                if (Character.isDigit(c)) {
                    col += Character.getNumericValue(c);
                } else {
                    PieceType type = fenToPiece.get(c);
                    if (type != null) {
                        squares.get(row).get(col).setPiece(new Piece(type));
                    }
                    col++;
                }
            }
        }

        // Parse active color
        if (parts.length >= 2) {
            isWhiteTurn = parts[1].equalsIgnoreCase("w");
        }

        // Parse castling availability
        if (parts.length >= 3) {
            String castling = parts[2];
            whiteKingMoved = !(castling.contains("K") || castling.contains("Q"));
            blackKingMoved = !(castling.contains("k") || castling.contains("q"));
            whiteRooksMoved.set(1, !castling.contains("K")); // kingside
            whiteRooksMoved.set(0, !castling.contains("Q")); // queenside
            blackRooksMoved.set(1, !castling.contains("k")); // kingside
            blackRooksMoved.set(0, !castling.contains("q")); // queenside
        }

        // Parse en passant
        if (parts.length >= 4 && !parts[3].equals("-")) {
            String ep = parts[3];
            int col = ep.charAt(0) - 'a';
            int row = 8 - Character.getNumericValue(ep.charAt(1));
            enPassantTarget = new ArrayList<>();
            enPassantTarget.add(row);
            enPassantTarget.add(col);
        } else {
            enPassantTarget = null;
        }

        updateTurnIndicator();
    }

    private void exportFEN() {
        fenTextField.setText(generateFEN());
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ChessApplication chessApp = new ChessApplication();
            chessApp.setVisible(true);
        });
    }
}

class ChessSquare extends JPanel {
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

class Piece {
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

enum PieceType {
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