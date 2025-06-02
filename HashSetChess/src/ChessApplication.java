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
    private JButton prevButton, nextButton;
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

    // Change to HashMap for history with integer key for move number
    private Map<Integer, BoardState> moveHistoryMap = new HashMap<>();
    private int currentMoveNumber = 0; // Represents the current move number in the history

    private JSpinner historySpinner; // Re-introduced
    private JButton jumpButton; // Re-introduced

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
        setTitle("Java Chess with HashMap-based History");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        chessBoard = new JPanel(new GridLayout(8, 8));
        chessBoard.setPreferredSize(new Dimension(400, 400));

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

        whiteRooksMoved.add(false);
        whiteRooksMoved.add(false);
        blackRooksMoved.add(false);
        blackRooksMoved.add(false);

        JPanel controlPanel = new JPanel(new BorderLayout());

        fenTextField = new JTextField("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");

        prevButton = new JButton("← Undo");
        nextButton = new JButton("Redo →");
        prevButton.addActionListener(e -> navigateMoveHistory(-1));
        nextButton.addActionListener(e -> navigateMoveHistory(1));

        historySpinner = new JSpinner(new SpinnerNumberModel(1, 1, 1, 1));

        jumpButton = new JButton("Jump to Move");
        jumpButton.addActionListener(e -> {
            int targetMove = (Integer) historySpinner.getValue();
            jumpToMove(targetMove);
        });

        turnLabel = new JLabel("White's turn", SwingConstants.CENTER);
        turnLabel.setFont(new Font("Serif", Font.BOLD, 16));
        turnLabel.setOpaque(true);
        turnLabel.setBackground(Color.WHITE);
        turnLabel.setBorder(BorderFactory.createLineBorder(Color.BLACK));

        JPanel historyButtonPanel = new JPanel();
        exportHistoryButton = new JButton("Export History");
        importHistoryButton = new JButton("Import History");
        exportHistoryButton.addActionListener(e -> exportHistoryToFile());
        importHistoryButton.addActionListener(e -> importHistoryFromFile());
        historyButtonPanel.add(exportHistoryButton);
        historyButtonPanel.add(importHistoryButton);

        JPanel navigationPanel = new JPanel();
        navigationPanel.add(prevButton);
        navigationPanel.add(historySpinner);
        navigationPanel.add(jumpButton);
        navigationPanel.add(nextButton);

        JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.add(navigationPanel, BorderLayout.CENTER);
        buttonPanel.add(turnLabel, BorderLayout.SOUTH);

        controlPanel.add(fenTextField, BorderLayout.CENTER);
        controlPanel.add(buttonPanel, BorderLayout.NORTH);
        controlPanel.add(historyButtonPanel, BorderLayout.SOUTH);

        add(chessBoard, BorderLayout.CENTER);
        add(controlPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);

        importFEN(true); // Initial FEN import, add to history
        updateTurnIndicator();
        updateNavigationButtons();
    }

    private void exportHistoryToFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File("chess_history_hashmap.txt"));
        int option = fileChooser.showSaveDialog(this);

        if (option == JFileChooser.APPROVE_OPTION) {
            try {
                Path file = fileChooser.getSelectedFile().toPath();
                List<String> fenHistory = new ArrayList<>();
                int originalCurrentMoveNumber = currentMoveNumber; // Store current position

                // Iterate through the HashMap keys (move numbers) in order
                for (int i = 1; i <= moveHistoryMap.size(); i++) {
                    BoardState tempState = moveHistoryMap.get(i);
                    loadBoardState(tempState);
                    fenHistory.add(generateFEN());
                }
                // After generating FENs, load back the original current state
                loadBoardState(moveHistoryMap.get(originalCurrentMoveNumber));

                Files.write(file, fenHistory);
                JOptionPane.showMessageDialog(this, "History exported to:\n" + file.toString());
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error exporting history:\n" + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void importHistoryFromFile() {
        JFileChooser fileChooser = new JFileChooser();
        int option = fileChooser.showOpenDialog(this);

        if (option == JFileChooser.APPROVE_OPTION) {
            try {
                Path file = fileChooser.getSelectedFile().toPath();
                List<String> importedFenHistory = Files.readAllLines(file);

                if (!importedFenHistory.isEmpty()) {
                    moveHistoryMap.clear();
                    currentMoveNumber = 0; // Reset history

                    for (String fen : importedFenHistory) {
                        importFENOnly(fen); // New helper to only parse FEN, not add to history
                        currentMoveNumber++;
                        moveHistoryMap.put(currentMoveNumber, saveBoardState()); // Manually add current board state
                    }
                    // Load the last state after importing all
                    loadBoardState(moveHistoryMap.get(currentMoveNumber));

                    updateNavigationButtons(); // Update spinner after new history is loaded
                    JOptionPane.showMessageDialog(this, "History imported from:\n" + file.toString());
                } else {
                    JOptionPane.showMessageDialog(this, "The file is empty",
                            "Error", JOptionPane.ERROR_MESSAGE);
                }
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error importing history:\n" + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void navigateMoveHistory(int direction) {
        int targetMove = currentMoveNumber + direction;
        // System.out.println("Navigate: currentMoveNumber=" + currentMoveNumber + ", direction=" + direction + ", targetMove=" + targetMove);

        if (targetMove >= 1 && targetMove <= moveHistoryMap.size()) {
            currentMoveNumber = targetMove;
            BoardState stateToLoad = moveHistoryMap.get(currentMoveNumber);
            if (stateToLoad != null) {
                loadBoardState(stateToLoad);
                // System.out.println("Navigated to move " + currentMoveNumber + ". Board loaded.");
            } else {
                // System.out.println("Error: BoardState is null for move number " + currentMoveNumber);
            }
        } else {
            // System.out.println("Navigation out of bounds: targetMove=" + targetMove + ", history size=" + moveHistoryMap.size());
        }
        updateNavigationButtons();
    }

    private void jumpToMove(int moveNum) {
        // System.out.println("Jump to: targetMove=" + moveNum + ", currentHistorySize=" + moveHistoryMap.size());
        if (moveNum >= 1 && moveNum <= moveHistoryMap.size()) {
            currentMoveNumber = moveNum;
            BoardState stateToLoad = moveHistoryMap.get(currentMoveNumber);
            if (stateToLoad != null) {
                loadBoardState(stateToLoad);
                // System.out.println("Jumped to move " + currentMoveNumber + ". Board loaded.");
            } else {
                // System.out.println("Error: BoardState is null for jump to move number " + currentMoveNumber);
            }
        } else {
            // System.out.println("Jump out of bounds: targetMove=" + moveNum + ", history size=" + moveHistoryMap.size());
        }
        updateNavigationButtons();
    }

    private void addToMoveHistory() {
        // System.out.println("Before addToMoveHistory: currentMoveNumber=" + currentMoveNumber + ", historySize=" + moveHistoryMap.size());
        // Remove future moves if we are not at the end of history
        // Iterate backwards from the current end of history to currentMoveNumber + 1
        for (int i = moveHistoryMap.size(); i > currentMoveNumber; i--) {
            moveHistoryMap.remove(i);
            // System.out.println("Removed future move: " + i);
        }
        currentMoveNumber++; // Increment to the new move number
        moveHistoryMap.put(currentMoveNumber, saveBoardState());
        // System.out.println("After addToMoveHistory: New move " + currentMoveNumber + " added. New history size=" + moveHistoryMap.size());
        updateNavigationButtons();
        historySpinner.setValue(currentMoveNumber); // Ensure spinner shows the current move
    }

    private void updateNavigationButtons() {
        prevButton.setEnabled(currentMoveNumber > 1);
        nextButton.setEnabled(currentMoveNumber < moveHistoryMap.size());

        // Update spinner model to reflect current move and total history size
        // The minimum value should be 1 if there's any history, otherwise it stays 1 (for initial FEN)
        int maxSpinnerValue = moveHistoryMap.size() > 0 ? moveHistoryMap.size() : 1;
        int currentSpinnerValue = (currentMoveNumber > 0) ? currentMoveNumber : 1; // Ensure spinner value is at least 1

        SpinnerNumberModel model = (SpinnerNumberModel) historySpinner.getModel();
        model.setMinimum(1); // Always allow selection of move 1
        model.setMaximum(maxSpinnerValue);
        model.setValue(currentSpinnerValue);

        // System.out.println("Update Nav Buttons: current=" + currentSpinnerValue + ", max=" + maxSpinnerValue + ", map size=" + moveHistoryMap.size());
    }

    private String generateFEN() {
        StringBuilder fen = new StringBuilder();

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

        fen.append(isWhiteTurn ? " w " : " b ");

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

        if (enPassantTarget != null) {
            char colChar = (char) ('a' + enPassantTarget.get(1));
            int rowNum = 8 - enPassantTarget.get(0);
            fen.append(" ").append(colChar).append(rowNum).append(" ");
        } else {
            fen.append(" - ");
        }

        fen.append("0 1"); // Halfmove clock and fullmove number are placeholders

        return fen.toString();
    }

    private void handleSquareClick(int row, int col) {
        if (selectedPiece == null) {
            Piece clickedPiece = squares.get(row).get(col).getPiece();
            if (clickedPiece != null && ((isWhiteTurn && clickedPiece.getType().isWhite()) ||
                    (!isWhiteTurn && clickedPiece.getType().isBlack()))) {
                selectedPiece = clickedPiece;
                selectedRow = row;
                selectedCol = col;
                squares.get(row).get(col).setBackground(Color.YELLOW);
            }
        } else {
            if (isValidMove(selectedRow, selectedCol, row, col)) {
                // Save original state before move to validate
                BoardState stateBeforeMove = saveBoardState(); // Capture state BEFORE actual move

                Piece movedPiece = squares.get(selectedRow).get(selectedCol).getPiece();

                boolean isPawnMove = (movedPiece.getType() == PieceType.PAWN_WHITE ||
                        movedPiece.getType() == PieceType.PAWN_BLACK);

                // Handle En Passant Capture
                if (isPawnMove && Math.abs(selectedCol - col) == 1 && squares.get(row).get(col).getPiece() == null) {
                    if (enPassantTarget != null && row == enPassantTarget.get(0) && col == enPassantTarget.get(1)) {
                        squares.get(selectedRow).get(col).setPiece(null); // Remove captured pawn
                    }
                }

                // Handle Castling
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
                            whiteRooksMoved.set(kingside ? 1 : 0, true); // Mark appropriate rook as moved
                        } else {
                            blackRooksMoved.set(kingside ? 1 : 0, true);
                        }
                    }
                }

                // Actually move the piece
                squares.get(selectedRow).get(selectedCol).setPiece(null);
                squares.get(row).get(col).setPiece(selectedPiece);

                // Pawn Promotion
                if ((selectedPiece.getType() == PieceType.PAWN_WHITE && row == 0) ||
                        (selectedPiece.getType() == PieceType.PAWN_BLACK && row == 7)) {
                    squares.get(row).get(col).setPiece(new Piece(
                            selectedPiece.getType().isWhite() ? PieceType.QUEEN_WHITE : PieceType.QUEEN_BLACK));
                }

                // Set En Passant Target
                enPassantTarget = null; // Clear previous en passant target
                if (isPawnMove && Math.abs(row - selectedRow) == 2) { // Two-square pawn advance
                    enPassantTarget = new ArrayList<>();
                    enPassantTarget.add(selectedRow + (movedPiece.getType().isWhite() ? -1 : 1)); // Row of target
                    enPassantTarget.add(col); // Column of target
                }

                isWhiteTurn = !isWhiteTurn; // Switch turn
                updateTurnIndicator();
                addToMoveHistory(); // Add this new board state to history
            }

            // Reset selection highlighting
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
            // Cannot capture your own piece
            return false;
        }

        // --- Temporarily make the move to check for King in Check ---
        BoardState originalState = saveBoardState(); // Save current state

        // Simulate the move
        Piece pieceAtDest = squares.get(toRow).get(toCol).getPiece(); // Store piece at destination
        Piece pieceAtSource = squares.get(fromRow).get(fromCol).getPiece(); // Store piece at source
        squares.get(toRow).get(toCol).setPiece(pieceAtSource); // Move piece
        squares.get(fromRow).get(fromCol).setPiece(null); // Clear source square

        // Special handling for en passant simulation for check validation
        if (piece.getType() == PieceType.PAWN_WHITE || piece.getType() == PieceType.PAWN_BLACK) {
            if (Math.abs(fromCol - toCol) == 1 && pieceAtDest == null &&
                    enPassantTarget != null && toRow == enPassantTarget.get(0) && toCol == enPassantTarget.get(1)) {
                // If it's an en passant capture, remove the captured pawn from its square for the check validation
                squares.get(fromRow).get(toCol).setPiece(null);
            }
        }

        // Special handling for castling simulation for check validation
        if ((piece.getType() == PieceType.KING_WHITE || piece.getType() == PieceType.KING_BLACK) && Math.abs(fromCol - toCol) == 2) {
            boolean kingside = toCol > fromCol;
            int rookCol = kingside ? 7 : 0;
            int newRookCol = kingside ? toCol - (kingside ? 1 : -1) : toCol + (kingside ? 1 : -1);

            Piece rookPiece = squares.get(fromRow).get(rookCol).getPiece();
            squares.get(fromRow).get(newRookCol).setPiece(rookPiece);
            squares.get(fromRow).get(rookCol).setPiece(null);
        }

        boolean kingInCheckAfterMove = isKingInCheck(piece.getType().isWhite());

        // --- Revert the board to original state ---
        loadBoardState(originalState);

        if (kingInCheckAfterMove) {
            return false; // Cannot make a move that puts or leaves your king in check
        }

        // --- Basic piece movement rules (without check validation) ---
        switch (piece.getType()) {
            case KING_WHITE:
            case KING_BLACK:
                // Normal king moves (one square in any direction)
                if (Math.abs(fromRow - toRow) <= 1 && Math.abs(fromCol - toCol) <= 1) return true;
                // Castling move (two squares horizontally)
                return Math.abs(fromCol - toCol) == 2 && fromRow == toRow && isValidCastling(fromRow, fromCol, toRow, toCol);
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
        if ((isWhite && whiteKingMoved) || (!isWhite && blackKingMoved)) return false; // King has moved

        boolean kingside = toCol > fromCol;
        int rookOriginalCol = kingside ? 7 : 0;

        // Check if rook has moved or is not in place
        Piece rook = squares.get(fromRow).get(rookOriginalCol).getPiece();
        if (rook == null || ((isWhite && !rook.getType().equals(PieceType.ROOK_WHITE)) || (!isWhite && !rook.getType().equals(PieceType.ROOK_BLACK)))) return false;
        if ((isWhite && whiteRooksMoved.get(kingside ? 1 : 0)) || (!isWhite && blackRooksMoved.get(kingside ? 1 : 0))) return false;

        // Check for pieces in between king and rook
        int startCol = Math.min(fromCol, rookOriginalCol) + 1;
        int endCol = Math.max(fromCol, rookOriginalCol);
        for (int col = startCol; col < endCol; col++) {
            if (squares.get(fromRow).get(col).getPiece() != null) return false;
        }

        // Check squares king passes through or lands on are not under attack
        int kingPathStart = fromCol;
        int kingPathEnd = toCol;
        int step = kingside ? 1 : -1;
        for (int col = kingPathStart; (step == 1 && col <= kingPathEnd) || (step == -1 && col >= kingPathEnd); col += step) {
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
        if (kingRow == -1) return false; // Should not happen in a valid game

        return isSquareUnderAttack(kingRow, kingCol, !isWhite);
    }

    private boolean isSquareUnderAttack(int row, int col, boolean byWhite) {
        // Iterate through all opponent's pieces
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece piece = squares.get(r).get(c).getPiece();
                if (piece != null && piece.getType().isWhite() == byWhite) {
                    // Temporarily set target square to null to check attack paths
                    // This is crucial for pieces that attack empty squares (e.g., pawn diagonal moves)
                    Piece tempTargetPiece = squares.get(row).get(col).getPiece();
                    squares.get(row).get(col).setPiece(null);

                    // If it's a pawn, and the target is its own piece, isValidMoveForCheck needs to be careful
                    // isValidMoveForCheck is simplified for attack checking
                    boolean attacks = isValidMoveForCheck(r, c, row, col);

                    // Restore the piece at the target square
                    squares.get(row).get(col).setPiece(tempTargetPiece);

                    if (attacks) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // Simplified move validation for checking if a square is attacked
    private boolean isValidMoveForCheck(int fromRow, int fromCol, int toRow, int toCol) {
        Piece piece = squares.get(fromRow).get(fromCol).getPiece();
        if (piece == null) return false;

        switch (piece.getType()) {
            case PAWN_WHITE:
                // Pawns attack diagonally
                return (Math.abs(fromCol - toCol) == 1 && fromRow - toRow == 1);
            case PAWN_BLACK:
                return (Math.abs(fromCol - toCol) == 1 && toRow - fromRow == 1);
            case KNIGHT_WHITE:
            case KNIGHT_BLACK:
                return (Math.abs(fromRow - toRow) == 2 && Math.abs(fromCol - toCol) == 1) ||
                        (Math.abs(fromRow - toRow) == 1 && Math.abs(fromCol - toCol) == 2);
            case BISHOP_WHITE:
            case BISHOP_BLACK:
                return isValidBishopMove(fromRow, fromCol, toRow, toCol); // Uses full path check
            case ROOK_WHITE:
            case ROOK_BLACK:
                return isValidRookMove(fromRow, fromCol, toRow, toCol); // Uses full path check
            case QUEEN_WHITE:
            case QUEEN_BLACK:
                return isValidQueenMove(fromRow, fromCol, toRow, toCol); // Uses full path check
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
        if (fromRow != toRow && fromCol != toCol) return false; // Must be horizontal or vertical

        int rowStep = Integer.compare(toRow, fromRow);
        int colStep = Integer.compare(toCol, fromCol);

        int currentRow = fromRow + rowStep;
        int currentCol = fromCol + colStep;

        while (currentRow != toRow || currentCol != toCol) {
            if (squares.get(currentRow).get(currentCol).getPiece() != null) {
                return false; // Path is blocked
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
        if (Math.abs(fromRow - toRow) != Math.abs(fromCol - toCol)) return false; // Must be diagonal

        int rowStep = toRow > fromRow ? 1 : -1;
        int colStep = toCol > fromCol ? 1 : -1;

        int currentRow = fromRow + rowStep;
        int currentCol = fromCol + colStep;

        while (currentRow != toRow || currentCol != toCol) {
            if (squares.get(currentRow).get(currentCol).getPiece() != null) {
                return false; // Path is blocked
            }
            currentRow += rowStep;
            currentCol += colStep;
        }
        return true;
    }

    private boolean isValidPawnMove(int fromRow, int fromCol, int toRow, int toCol) {
        Piece pawn = squares.get(fromRow).get(fromCol).getPiece();
        boolean isWhite = pawn.getType().isWhite();
        int direction = isWhite ? -1 : 1; // Direction of pawn movement

        // Normal 1-square move
        if (fromCol == toCol && squares.get(toRow).get(toCol).getPiece() == null) {
            if (toRow == fromRow + direction) return true;
            // Two-square initial move
            if (((isWhite && fromRow == 6) || (!isWhite && fromRow == 1)) && // Starting row
                    toRow == fromRow + 2 * direction && // Two squares
                    squares.get(fromRow + direction).get(fromCol).getPiece() == null) { // Path clear
                return true;
            }
        }

        // Capture move (diagonal)
        if (Math.abs(fromCol - toCol) == 1 && toRow == fromRow + direction) {
            if (squares.get(toRow).get(toCol).getPiece() != null) { // Direct capture
                return true;
            }
            // En Passant capture
            if (enPassantTarget != null && toRow == enPassantTarget.get(0) && toCol == enPassantTarget.get(1)) {
                return true;
            }
        }
        return false;
    }

    private BoardState saveBoardState() {
        Piece[][] currentPieces = new Piece[8][8];
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                currentPieces[r][c] = squares.get(r).get(c).getPiece(); // Copy Piece references (Piece is immutable, so it's fine)
            }
        }
        // Deep copy lists to prevent aliasing issues when history is modified
        List<Integer> currentEnPassantTarget = (enPassantTarget != null) ? new ArrayList<>(enPassantTarget) : null;
        List<Boolean> currentWhiteRooksMoved = new ArrayList<>(whiteRooksMoved);
        List<Boolean> currentBlackRooksMoved = new ArrayList<>(blackRooksMoved);

        return new BoardState(currentPieces, isWhiteTurn, currentEnPassantTarget,
                whiteKingMoved, blackKingMoved, currentWhiteRooksMoved, currentBlackRooksMoved);
    }

    private void loadBoardState(BoardState state) {
        if (state == null) {
            // System.out.println("Attempted to load a null BoardState.");
            return;
        }

        Piece[][] loadedPieces = state.getPieces();
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                squares.get(r).get(c).setPiece(loadedPieces[r][c]);
                // No need to change background here, it's done on selection
                squares.get(r).get(c).setBackground((r + c) % 2 == 0 ? Color.WHITE : new Color(180, 180, 180)); // Reset background
            }
        }

        isWhiteTurn = state.isWhiteTurn();
        enPassantTarget = (state.getEnPassantTarget() != null) ? new ArrayList<>(state.getEnPassantTarget()) : null;
        whiteKingMoved = state.isWhiteKingMoved();
        blackKingMoved = state.isBlackKingMoved();
        whiteRooksMoved = new ArrayList<>(state.getWhiteRooksMoved());
        blackRooksMoved = new ArrayList<>(state.getBlackRooksMoved());

        updateTurnIndicator();
        fenTextField.setText(generateFEN()); // Update FEN text field to reflect loaded state
    }

    // Helper method to parse FEN only, without side effects on history
    private void importFENOnly(String fen) {
        String[] parts = fen.split(" ");
        if (parts.length < 1) return;

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                squares.get(row).get(col).setPiece(null);
            }
        }

        String[] ranks = parts[0].split("/");
        if (ranks.length != 8) {
            // This should ideally throw an exception or handle more robustly
            return;
        }

        try {
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
        } catch (Exception e) {
            // Log this error
            e.printStackTrace();
        }

        if (parts.length >= 2) {
            isWhiteTurn = parts[1].equalsIgnoreCase("w");
        } else {
            isWhiteTurn = true;
        }

        if (parts.length >= 3) {
            String castling = parts[2];
            whiteKingMoved = true;
            blackKingMoved = true;
            whiteRooksMoved.set(0, true); // Assume rooks moved unless specified
            whiteRooksMoved.set(1, true);
            blackRooksMoved.set(0, true);
            blackRooksMoved.set(1, true);

            if (castling.contains("K")) whiteRooksMoved.set(1, false); // Kingside White
            if (castling.contains("Q")) whiteRooksMoved.set(0, false); // Queenside White
            if (castling.contains("k")) blackRooksMoved.set(1, false); // Kingside Black
            if (castling.contains("q")) blackRooksMoved.set(0, false); // Queenside Black

            // If any castling rights exist, king hasn't moved
            if (castling.contains("K") || castling.contains("Q")) whiteKingMoved = false;
            if (castling.contains("k") || castling.contains("q")) blackKingMoved = false;

        } else { // No castling rights specified, assume kings and rooks have moved
            whiteKingMoved = true;
            blackKingMoved = true;
            whiteRooksMoved.set(0, true);
            whiteRooksMoved.set(1, true);
            blackRooksMoved.set(0, true);
            blackRooksMoved.set(1, true);
        }

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
        updateTurnIndicator(); // Update turn label to reflect parsed FEN
    }


    // Added boolean parameter to control if history is added
    private void importFEN(boolean addToHistory) {
        String fen = fenTextField.getText().trim();
        importFENOnly(fen); // Parse the FEN into the current board state

        if (addToHistory) {
            moveHistoryMap.clear(); // Clear existing history
            currentMoveNumber = 0;   // Reset move number
            addToMoveHistory(); // Add the initial state as move #1
        }
        updateNavigationButtons(); // Update UI elements
    }


    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ChessApplication chessApp = new ChessApplication();
            chessApp.setVisible(true);
        });
    }
}

// BoardState class - moved back into ChessApplication.java
class BoardState implements Serializable {
    private final Piece[][] pieces;
    private final boolean isWhiteTurn;
    private final List<Integer> enPassantTarget;
    private final boolean whiteKingMoved;
    private final boolean blackKingMoved;
    private final List<Boolean> whiteRooksMoved;
    private final List<Boolean> blackRooksMoved;

    public BoardState(Piece[][] pieces, boolean isWhiteTurn, List<Integer> enPassantTarget,
                      boolean whiteKingMoved, boolean blackKingMoved, List<Boolean> whiteRooksMoved, List<Boolean> blackRooksMoved) {
        this.pieces = new Piece[8][8];
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                this.pieces[r][c] = pieces[r][c]; // Piece objects are immutable, so shallow copy is fine
            }
        }
        this.isWhiteTurn = isWhiteTurn;
        this.enPassantTarget = (enPassantTarget != null) ? new ArrayList<>(enPassantTarget) : null;
        this.whiteKingMoved = whiteKingMoved;
        this.blackKingMoved = blackKingMoved;
        this.whiteRooksMoved = new ArrayList<>(whiteRooksMoved);
        this.blackRooksMoved = new ArrayList<>(blackRooksMoved);
    }

    public Piece[][] getPieces() {
        return pieces;
    }

    public boolean isWhiteTurn() {
        return isWhiteTurn;
    }

    public List<Integer> getEnPassantTarget() {
        return enPassantTarget;
    }

    public boolean isWhiteKingMoved() {
        return whiteKingMoved;
    }

    public boolean isBlackKingMoved() {
        return blackKingMoved;
    }

    public List<Boolean> getWhiteRooksMoved() {
        return whiteRooksMoved;
    }

    public List<Boolean> getBlackRooksMoved() {
        return blackRooksMoved;
    }
}

// ChessSquare class - kept in ChessApplication.java
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

// Piece class - kept in ChessApplication.java
class Piece implements Serializable {
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

// PieceType enum - kept in ChessApplication.java
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