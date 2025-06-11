package chess;

import chess.model.Piece;
import chess.model.PieceType;
import chess.model.Square;
import chess.utils.Benchmark;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.List;

import static chess.utils.Benchmark.getSpace;
import static chess.utils.Benchmark.getTime;

public class ChessApplication extends JFrame {
    private JPanel chessBoard;
    private JTextField fenTextField;
    private JButton importButton, exportButton;
    private JLabel turnLabel;
    private Square[][] squares = new Square[8][8];
    private Piece selectedPiece = null;
    private int selectedRow = -1, selectedCol = -1;
    private boolean isWhiteTurn = true;
    private int[] enPassantTarget = null; // [row, col] of en passant target square
    private boolean whiteKingMoved = false;
    private boolean blackKingMoved = false;
    private boolean[] whiteRooksMoved = {false, false}; // queenside, kingside
    private boolean[] blackRooksMoved = {false, false};
    private HashMap<Integer, String> fenHistory = new HashMap<>();
    private int currentHistoryIndex = -1;
    private int maxHistoryIndex = -1;
    private JButton backButton, forwardButton;
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
        setTitle("Java Chess with FEN");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(new BorderLayout());

        // Initialize chess board
        chessBoard = new JPanel(new GridLayout(8, 8));
        chessBoard.setPreferredSize(new Dimension(400, 400));

        // Create chess squares
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                squares[row][col] = new Square(row, col);
                squares[row][col].setBackground((row + col) % 2 == 0 ? Color.WHITE : new Color(180, 180, 180));
                squares[row][col].setOpaque(true);
                squares[row][col].setBorder(BorderFactory.createLineBorder(Color.BLACK));

                final int r = row, c = col;
                squares[row][col].addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        handleSquareClick(r, c);
                    }
                });

                chessBoard.add(squares[row][col]);
            }
        }

        // FEN controls
        JPanel fenPanel = new JPanel(new BorderLayout());
        fenTextField = new JTextField("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");


        // Navigation buttons
        backButton = new JButton("← Previous");
        forwardButton = new JButton("Next →");

        backButton.setEnabled(false);
        forwardButton.setEnabled(false);

        backButton.addActionListener(e -> navigateHistory(-1));
        forwardButton.addActionListener(e -> navigateHistory(1));

        // History navigation controls
        historySpinner = new JSpinner(new SpinnerNumberModel(0, 0, 0, 1));
        jumpButton = new JButton("Jump to Move");
        jumpButton.addActionListener(e -> jumpToMove());

        // History buttons panel (now above turn indicator)
        JPanel historyButtonPanel = new JPanel();
        exportHistoryButton = new JButton("Export History");
        importHistoryButton = new JButton("Import History");
        exportHistoryButton.addActionListener(e -> exportHistoryToFile());
        importHistoryButton.addActionListener(e -> importHistoryFromFile());
        historyButtonPanel.add(exportHistoryButton);
        historyButtonPanel.add(importHistoryButton);

        // Turn indicator
        turnLabel = new JLabel("White's turn", SwingConstants.CENTER);
        turnLabel.setFont(new Font("Serif", Font.BOLD, 16));
        turnLabel.setOpaque(true);
        turnLabel.setBackground(Color.WHITE);
        turnLabel.setBorder(BorderFactory.createLineBorder(Color.BLACK));

        JPanel buttonPanel = new JPanel(new BorderLayout());
        JPanel fenButtonPanel = new JPanel();

        fenButtonPanel.add(backButton);
        fenButtonPanel.add(forwardButton);
        fenButtonPanel.add(new JLabel("Move:"));
        fenButtonPanel.add(historySpinner);
        fenButtonPanel.add(jumpButton);

        buttonPanel.add(fenButtonPanel, BorderLayout.NORTH);
        buttonPanel.add(historyButtonPanel, BorderLayout.CENTER);
        buttonPanel.add(turnLabel, BorderLayout.SOUTH);

        fenPanel.add(fenTextField, BorderLayout.CENTER);
        fenPanel.add(buttonPanel, BorderLayout.SOUTH);

        add(chessBoard, BorderLayout.CENTER);
        add(fenPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);

        // Initialize board with starting position
        initializeFEN();
        updateTurnIndicator();
        updateNavigationButtons();
    }

    private void jumpToMove() {
        long startTime, endTime = 0;
        startTime = System.nanoTime();

        int index = (Integer)historySpinner.getValue();
        if (fenHistory.containsKey(index)) {
            currentHistoryIndex = index;
            fenTextField.setText(fenHistory.get(currentHistoryIndex));
            initializeFEN();
            updateNavigationButtons();
        }

        endTime = System.nanoTime();
        getTime(startTime, endTime);
        getSpace();
    }

    private void updateNavigationButtons() {
        backButton.setEnabled(currentHistoryIndex > 0);
        forwardButton.setEnabled(currentHistoryIndex < maxHistoryIndex);
    }

    private void exportHistoryToFile() {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setSelectedFile(new File("chess_history.txt"));
        int option = fileChooser.showSaveDialog(this);
        long startTime = 0;
        long endTime = 0;

        if (option == JFileChooser.APPROVE_OPTION) {
            try {
                startTime = System.nanoTime();
                Path file = fileChooser.getSelectedFile().toPath();
                List<String> historyList = new ArrayList<>();
                for (int i = 0; i <= maxHistoryIndex; i++) {
                    if (fenHistory.containsKey(i)) {
                        historyList.add(fenHistory.get(i));
                    }
                }
                Files.write(file, historyList);
                endTime = System.nanoTime();
                JOptionPane.showMessageDialog(this, "History exported to:\n" + file.toString());
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error exporting history:\n" + ex.getMessage(),
                        "Error", JOptionPane.ERROR_MESSAGE);
            }
        }
        getTime(startTime, endTime); //get time used
        getSpace(); //get space used
    }

    private void importHistoryFromFile() {
        JFileChooser fileChooser = new JFileChooser();
        int option = fileChooser.showOpenDialog(this);
        long startTime = 0;
        long endTime = 0;

        if (option == JFileChooser.APPROVE_OPTION) {
            try {
                startTime = System.nanoTime();
                Path file = fileChooser.getSelectedFile().toPath();
                List<String> importedHistory = Files.readAllLines(file);

                if (!importedHistory.isEmpty()) {
                    fenHistory.clear();
                    for (int i = 0; i < importedHistory.size(); i++) {
                        fenHistory.put(i, importedHistory.get(i));
                    }
                    maxHistoryIndex = importedHistory.size() - 1;
                    currentHistoryIndex = maxHistoryIndex;
                    historySpinner.setModel(new SpinnerNumberModel(
                            currentHistoryIndex, 0, Math.max(0, maxHistoryIndex), 1));
                    fenTextField.setText(fenHistory.get(currentHistoryIndex));
                    initializeFEN();
                    updateNavigationButtons();
                    endTime = System.nanoTime();
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
        getTime(startTime, endTime); //get time used
        getSpace(); //get space used
    }



    private void navigateHistory(int direction) {
        int newIndex = currentHistoryIndex + direction;

        if (!fenHistory.containsKey(newIndex)) {
            return;
        }

        currentHistoryIndex = newIndex;
        String fen = fenHistory.get(currentHistoryIndex);
        loadFENPosition(fen);

        backButton.setEnabled(currentHistoryIndex > 0);
        forwardButton.setEnabled(currentHistoryIndex < maxHistoryIndex);
    }

    private void addToMoveHistory() {
        long startTime = 0;
        long endTime = 0;

        startTime = System.nanoTime();
        if (currentHistoryIndex < maxHistoryIndex) {
            for (int i = currentHistoryIndex + 1; i <= maxHistoryIndex; i++) {
                fenHistory.remove(i);
            }
            maxHistoryIndex = currentHistoryIndex;
        }

        String currentFEN = generateFEN();
        maxHistoryIndex++;
        fenHistory.put(maxHistoryIndex, currentFEN);
        currentHistoryIndex = maxHistoryIndex;
        historySpinner.setModel(new SpinnerNumberModel(
                currentHistoryIndex, 0, Math.max(0, maxHistoryIndex), 1));
        updateNavigationButtons();

        endTime = System.nanoTime();
        getTime(startTime, endTime); //get time used
        getSpace();
    }


    private void loadFENPosition(String fen) {
        backButton.setEnabled(false);
        forwardButton.setEnabled(false);

        fenTextField.setText(fen);
        initializeFEN();

        backButton.setEnabled(currentHistoryIndex > 0);
        forwardButton.setEnabled(currentHistoryIndex < maxHistoryIndex);
    }


    private void updateTurnIndicator() {
        turnLabel.setText(isWhiteTurn ? "White's turn" : "Black's turn");
        turnLabel.setBackground(isWhiteTurn ? Color.WHITE : Color.BLACK);
        turnLabel.setForeground(isWhiteTurn ? Color.BLACK : Color.WHITE);
    }

    private void handleSquareClick(int row, int col) {
        // If no piece is selected, try to select one
        if (selectedPiece == null) {
            Piece clickedPiece = squares[row][col].getPiece();

            // Check if the clicked piece belongs to the current player
            if (clickedPiece != null &&
                    ((isWhiteTurn && clickedPiece.getType().isWhite()) ||
                            (!isWhiteTurn && clickedPiece.getType().isBlack()))) {

                selectedPiece = clickedPiece;
                selectedRow = row;
                selectedCol = col;
                squares[row][col].setBackground(Color.YELLOW);
            }
        }
        // If a piece is already selected, try to move it
        else {
            // Check if the move is valid
            if (isValidMove(selectedRow, selectedCol, row, col)) {
                Piece movedPiece = squares[selectedRow][selectedCol].getPiece();

                // Handle castling
                if (movedPiece.getType() == PieceType.KING_WHITE ||
                        movedPiece.getType() == PieceType.KING_BLACK) {

                    // Mark king as moved
                    if (movedPiece.getType().isWhite()) {
                        whiteKingMoved = true;
                    } else {
                        blackKingMoved = true;
                    }

                    // Handle castling rook movement
                    if (Math.abs(selectedCol - col) == 2) {
                        boolean kingside = col > selectedCol;
                        int rookCol = kingside ? 7 : 0;
                        int newRookCol = kingside ? col - 1 : col + 1;

                        // Move the rook
                        squares[row][newRookCol].setPiece(squares[row][rookCol].getPiece());
                        squares[row][rookCol].setPiece(null);

                        // Mark rook as moved
                        if (movedPiece.getType().isWhite()) {
                            whiteRooksMoved[kingside ? 1 : 0] = true;
                        } else {
                            blackRooksMoved[kingside ? 1 : 0] = true;
                        }
                    }
                }

                // Handle en passant capture
                if (selectedPiece.getType() == PieceType.PAWN_WHITE ||
                        selectedPiece.getType() == PieceType.PAWN_BLACK) {
                    handlePawnMove(selectedRow, selectedCol, row, col);
                }

                // Move the piece
                squares[selectedRow][selectedCol].setPiece(null);
                squares[row][col].setPiece(selectedPiece);



                // Handle pawn promotion (always to queen for simplicity)
                if ((selectedPiece.getType() == PieceType.PAWN_WHITE && row == 0) ||
                        (selectedPiece.getType() == PieceType.PAWN_BLACK && row == 7)) {
                    squares[row][col].setPiece(new Piece(
                            selectedPiece.getType().isWhite() ? PieceType.QUEEN_WHITE : PieceType.QUEEN_BLACK));
                }

                // Switch turns
                isWhiteTurn = !isWhiteTurn;
                updateTurnIndicator();
                addToMoveHistory();
            }

            // Reset selection (whether move was valid or not)
            squares[selectedRow][selectedCol].setBackground(
                    (selectedRow + selectedCol) % 2 == 0 ? Color.WHITE : new Color(180, 180, 180));

            selectedPiece = null;
            selectedRow = -1;
            selectedCol = -1;

        }
    }


    private String generateFEN() {
        StringBuilder fen = new StringBuilder();

        // Piece placement
        for (int row = 0; row < 8; row++) {
            int emptyCount = 0;

            for (int col = 0; col < 8; col++) {
                Piece piece = squares[row][col].getPiece();

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
            if (!whiteRooksMoved[1]) castling.append("K");
            if (!whiteRooksMoved[0]) castling.append("Q");
        }
        if (!blackKingMoved) {
            if (!blackRooksMoved[1]) castling.append("k");
            if (!blackRooksMoved[0]) castling.append("q");
        }
        fen.append(castling.length() > 0 ? castling.toString() : "-");
        fen.append(" ");

        // En passant
        if (enPassantTarget != null) {
            char colChar = (char) ('a' + enPassantTarget[1]);
            int rowNum = 8 - enPassantTarget[0];
            fen.append(colChar).append(rowNum).append(" ");
        } else {
            fen.append("- ");
        }

        // Halfmove clock and fullmove number (simplified)
        fen.append("0 1");

        fenTextField.setText(fen.toString());
        return fen.toString();
    }

    private boolean isValidMove(int fromRow, int fromCol, int toRow, int toCol) {
        Piece piece = squares[fromRow][fromCol].getPiece();
        if (piece == null) return false;

        // Check if it's the current player's turn
        if (piece.getType().isWhite() != isWhiteTurn) {
            return false;
        }

        // Check if destination has a piece of the same color
        Piece targetPiece = squares[toRow][toCol].getPiece();
        if (targetPiece != null && targetPiece.getType().isWhite() == piece.getType().isWhite()) {
            return false;
        }

        // First check piece-specific movement rules
        boolean validMove = false;
        switch (piece.getType()) {
            case PAWN_WHITE:
            case PAWN_BLACK:
                validMove = isValidPawnMove(fromRow, fromCol, toRow, toCol);
                break;
            case KNIGHT_WHITE:
            case KNIGHT_BLACK:
                validMove = isValidKnightMove(fromRow, fromCol, toRow, toCol);
                break;
            case BISHOP_WHITE:
            case BISHOP_BLACK:
                validMove = isValidBishopMove(fromRow, fromCol, toRow, toCol);
                break;
            case ROOK_WHITE:
            case ROOK_BLACK:
                validMove = isValidRookMove(fromRow, fromCol, toRow, toCol);
                break;
            case QUEEN_WHITE:
            case QUEEN_BLACK:
                validMove = isValidQueenMove(fromRow, fromCol, toRow, toCol);
                break;
            case KING_WHITE:
            case KING_BLACK:
                return Math.abs(fromRow - toRow) <= 1 && Math.abs(fromCol - toCol) <= 1;

        }

        if (!validMove) {
            return false;
        }

        // Simulate the move to check if it leaves king in check
        Piece temp = squares[toRow][toCol].getPiece();
        squares[toRow][toCol].setPiece(piece);
        squares[fromRow][fromCol].setPiece(null);

        boolean inCheckAfterMove = isKingInCheck(piece.getType().isWhite());

        // Undo the simulation
        squares[fromRow][fromCol].setPiece(piece);
        squares[toRow][toCol].setPiece(temp);

        if (inCheckAfterMove) {
            return false;
        }

        return true;
    }

    private boolean isKingInCheck(boolean isWhite) {
        // Find the king's position
        int kingRow = -1, kingCol = -1;
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Piece piece = squares[row][col].getPiece();
                if (piece != null && piece.getType().isWhite() == isWhite &&
                        (piece.getType() == PieceType.KING_WHITE || piece.getType() == PieceType.KING_BLACK)) {
                    kingRow = row;
                    kingCol = col;
                    break;
                }
            }
            if (kingRow != -1) break;
        }

        if (kingRow == -1) return false; // shouldn't happen

        // Check if any opponent piece can attack the king
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                Piece piece = squares[row][col].getPiece();
                if (piece != null && piece.getType().isWhite() != isWhite) {
                    if (isValidAttack(row, col, kingRow, kingCol)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    private boolean isValidAttack(int fromRow, int fromCol, int toRow, int toCol) {
        Piece attacker = squares[fromRow][fromCol].getPiece();
        if (attacker == null) return false;

        // Special handling for pawn attacks (different from movement)
        if (attacker.getType() == PieceType.PAWN_WHITE) {
            return (fromRow - toRow == 1) && (Math.abs(fromCol - toCol) == 1);
        }
        if (attacker.getType() == PieceType.PAWN_BLACK) {
            return (toRow - fromRow == 1) && (Math.abs(fromCol - toCol) == 1);
        }

        // For other pieces, use their normal movement rules
        switch (attacker.getType()) {
            case KNIGHT_WHITE:
            case KNIGHT_BLACK:
                return isValidKnightMove(fromRow, fromCol, toRow, toCol);
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
                // Makes it so they can only move between 1 space
                return Math.abs(fromRow - toRow) <= 1 && Math.abs(fromCol - toCol) <= 1;
            default:
                return false;
        }
    }

    private boolean isValidCastling(int fromRow, int fromCol, int toRow, int toCol) {
        boolean isWhite = squares[fromRow][fromCol].getPiece().getType().isWhite();

        // Check if king has moved
        if ((isWhite && whiteKingMoved) || (!isWhite && blackKingMoved)) {
            return false;
        }

        // Check if in check
        if (isKingInCheck(isWhite)) {
            return false;
        }

        // Determine castling side
        boolean kingside = toCol > fromCol;
        int rookCol = kingside ? 7 : 0;

        // Check if rook has moved
        if ((isWhite && whiteRooksMoved[kingside ? 1 : 0]) ||
                (!isWhite && blackRooksMoved[kingside ? 1 : 0])) {
            return false;
        }

        // Check if squares between are empty
        int start = Math.min(fromCol, rookCol) + 1;
        int end = Math.max(fromCol, rookCol);
        for (int col = start; col < end; col++) {
            if (squares[fromRow][col].getPiece() != null) {
                return false;
            }
        }

        // Check if king would pass through attacked square
        int step = kingside ? 1 : -1;
        for (int col = fromCol; col != toCol; col += step) {
            if (isSquareUnderAttack(fromRow, col, !isWhite)) {
                return false;
            }
        }

        return true;
    }



    private boolean isSquareUnderAttack(int row, int col, boolean byWhite) {
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece piece = squares[r][c].getPiece();
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
        Piece piece = squares[fromRow][fromCol].getPiece();
        if (piece == null) return false;

        // Simplified movement checks just for check detection
        switch (piece.getType()) {
            case PAWN_WHITE:
                return (Math.abs(fromCol - toCol) == 1 && fromRow - toRow == 1); // Pawn capture
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
        }
        return false;
    }


    private boolean isValidQueenMove(int fromRow, int fromCol, int toRow, int toCol) {
        // Queen combines rook and bishop movement
        return isValidRookMove(fromRow, fromCol, toRow, toCol) ||
                isValidBishopMove(fromRow, fromCol, toRow, toCol);
    }

    private boolean isValidRookMove(int fromRow, int fromCol, int toRow, int toCol) {
        // Must move in straight line (same row or same column)
        if (fromRow != toRow && fromCol != toCol) {
            return false;
        }

        // Determine direction of movement
        int rowStep = Integer.compare(toRow, fromRow);
        int colStep = Integer.compare(toCol, fromCol);

        // Check if path is clear
        int currentRow = fromRow + rowStep;
        int currentCol = fromCol + colStep;

        while (currentRow != toRow || currentCol != toCol) {
            if (squares[currentRow][currentCol].getPiece() != null) {
                return false; // Path is blocked
            }
            currentRow += rowStep;
            currentCol += colStep;
        }

        return true;
    }

    private boolean isValidKnightMove(int fromRow, int fromCol, int toRow, int toCol) {
        // Calculate row and column differences
        int rowDiff = Math.abs(toRow - fromRow);
        int colDiff = Math.abs(toCol - fromCol);

        // Knight moves in L-shape: 2 squares in one direction and 1 square perpendicular
        return (rowDiff == 2 && colDiff == 1) || (rowDiff == 1 && colDiff == 2);
    }

    private boolean isValidBishopMove(int fromRow, int fromCol, int toRow, int toCol) {
        // Must move diagonally
        if (Math.abs(fromRow - toRow) != Math.abs(fromCol - toCol)) {
            return false;
        }

        int rowDirection = (toRow > fromRow) ? 1 : -1;
        int colDirection = (toCol > fromCol) ? 1 : -1;

        // Check if path is clear
        int currentRow = fromRow + rowDirection;
        int currentCol = fromCol + colDirection;

        while (currentRow != toRow || currentCol != toCol) {
            if (squares[currentRow][currentCol].getPiece() != null) {
                return false; // Path is blocked
            }
            currentRow += rowDirection;
            currentCol += colDirection;
        }

        return true;
    }

    private boolean isValidPawnMove(int fromRow, int fromCol, int toRow, int toCol) {
        Piece pawn = squares[fromRow][fromCol].getPiece();
        boolean isWhite = pawn.getType().isWhite();
        int direction = isWhite ? -1 : 1; // White moves up (decreasing row), black moves down

        // Normal forward move (1 square)
        if (fromCol == toCol && squares[toRow][toCol].getPiece() == null) {
            // Single square forward
            if (toRow == fromRow + direction) {
                return true;
            }
            // Two squares forward from starting position
            if ((isWhite && fromRow == 6) || (!isWhite && fromRow == 1)) {
                if (toRow == fromRow + 2 * direction &&
                        squares[fromRow + direction][fromCol].getPiece() == null &&
                        squares[toRow][toCol].getPiece() == null) {
                    // Set en passant target
                    enPassantTarget = new int[]{fromRow + direction, fromCol};
                    return true;
                }
            }
        }

        // Capture (diagonal)
        if (Math.abs(fromCol - toCol) == 1 && toRow == fromRow + direction) {
            // Normal capture
            if (squares[toRow][toCol].getPiece() != null) {
                return true;
            }
            // En passant capture
            if (enPassantTarget != null && toRow == enPassantTarget[0] && toCol == enPassantTarget[1]) {
                squares[fromRow][enPassantTarget[1]].setPiece(null); // Remove the captured pawn
                return true;
            }
        }

        return false;
    }

    private void handlePawnMove(int fromRow, int fromCol, int toRow, int toCol) {
        // Clear en passant target after move
        enPassantTarget = null;

        // If pawn moved two squares, set new en passant target
        Piece pawn = squares[fromRow][fromCol].getPiece();
        boolean isWhite = pawn.getType().isWhite();
        int direction = isWhite ? -1 : 1;

        if (Math.abs(toRow - fromRow) == 2 && fromCol == toCol) {
            enPassantTarget = new int[]{fromRow + direction, fromCol};
        }
    }

    private void initializeFEN() {
        String fen = fenTextField.getText().trim();
        String[] parts = fen.split(" ");
        if (parts.length < 1) return;

        // Only add to history if this is a new position (not from navigation)
        if (currentHistoryIndex == -1 || !fen.equals(fenHistory.get(currentHistoryIndex))) {
            maxHistoryIndex++;
            fenHistory.put(maxHistoryIndex, fen);
            currentHistoryIndex = maxHistoryIndex;
            historySpinner.setModel(new SpinnerNumberModel(
                    currentHistoryIndex, 0, Math.max(0, maxHistoryIndex), 1));
        }

        // Parse active color from FEN
        if (parts.length >= 2) {
            isWhiteTurn = parts[1].equalsIgnoreCase("w");
        } else {
            isWhiteTurn = true;
        }

        // Parse en passant target square
        if (parts.length >= 3 && !parts[2].equals("-")) {
            String ep = parts[2];
            int col = ep.charAt(0) - 'a';
            int row = 8 - Character.getNumericValue(ep.charAt(1));
            enPassantTarget = new int[]{row, col};
        } else {
            enPassantTarget = null;
        }

        if (parts.length >= 3) {
            String castling = parts[2];
            whiteRooksMoved[1] = !castling.contains("K"); // kingside
            whiteRooksMoved[0] = !castling.contains("Q"); // queenside
            blackRooksMoved[1] = !castling.contains("k");
            blackRooksMoved[0] = !castling.contains("q");
            whiteKingMoved = !(castling.contains("K") || castling.contains("Q"));
            blackKingMoved = !(castling.contains("k") || castling.contains("q"));
        }

        String[] ranks = parts[0].split("/");
        if (ranks.length != 8) {
            JOptionPane.showMessageDialog(this, "Invalid FEN - must have 8 ranks", "Error", JOptionPane.ERROR_MESSAGE);
            return;
        }

        // Clear the board
        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                squares[row][col].setPiece(null);
            }
        }

        // Parse the FEN string
        try {
            for (int row = 0; row < 8; row++) {
                String rank = ranks[row];
                int col = 0;

                for (int i = 0; i < rank.length(); i++) {
                    char c = rank.charAt(i);

                    if (Character.isDigit(c)) {
                        int emptySquares = Character.getNumericValue(c);
                        col += emptySquares;
                    } else {
                        PieceType type = fenToPiece.get(c);
                        if (type != null) {
                            squares[row][col].setPiece(new Piece(type));
                        }
                        col++;
                    }
                }
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Invalid FEN format", "Error", JOptionPane.ERROR_MESSAGE);
            e.printStackTrace();
        }

        updateNavigationButtons();
        updateTurnIndicator();
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            new ChessApplication().setVisible(true);
        });
    }
}
