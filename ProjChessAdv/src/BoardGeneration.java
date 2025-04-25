import java.util.*;

public class BoardGeneration {

    public static void initiateStandardChess() {
        long WP = 0L, WN = 0L, WB = 0L, WR = 0L, WQ = 0L, WK = 0L;
        long BP = 0L, BN = 0L, BB = 0L, BR = 0L, BQ = 0L, BK = 0L;

        String[][] chessBoard = {
                {"r", "n", "b", "q", "k", "b", "n", "r"},
                {"p", "p", "p", "p", "p", "p", "p", "p"},
                {" ", " ", " ", " ", " ", " ", " ", " "},
                {" ", " ", " ", " ", " ", " ", " ", " "},
                {" ", " ", " ", " ", " ", " ", " ", " "},
                {" ", " ", " ", " ", " ", " ", " ", " "},
                {"P", "P", "P", "P", "P", "P", "P", "P"},
                {"R", "N", "B", "Q", "K", "B", "N", "R"}
        };

        arrayToBitboards(chessBoard, WP, WN, WB, WR, WQ, WK, BP, BN, BB, BR, BQ, BK);
    }

    public static void initiateChess960() {
        long WP = 0L, WN = 0L, WB = 0L, WR = 0L, WQ = 0L, WK = 0L;
        long BP = 0L, BN = 0L, BB = 0L, BR = 0L, BQ = 0L, BK = 0L;

        String[][] chessBoard = {
                {" ", " ", " ", " ", " ", " ", " ", " "},
                {"p", "p", "p", "p", "p", "p", "p", "p"},
                {" ", " ", " ", " ", " ", " ", " ", " "},
                {" ", " ", " ", " ", " ", " ", " ", " "},
                {" ", " ", " ", " ", " ", " ", " ", " "},
                {" ", " ", " ", " ", " ", " ", " ", " "},
                {"P", "P", "P", "P", "P", "P", "P", "P"},
                {" ", " ", " ", " ", " ", " ", " ", " "}
        };

        // Place bishops on opposite colors
        int random1 = (int)(Math.random() * 8);
        while (random1 % 2 != 0) {
            random1 = (int)(Math.random() * 8);
        }
        chessBoard[0][random1] = "b";
        chessBoard[7][random1] = "B";

        int random2 = (int)(Math.random() * 8);
        while (random2 % 2 != 1 || random2 == random1) {
            random2 = (int)(Math.random() * 8);
        }
        chessBoard[0][random2] = "b";
        chessBoard[7][random2] = "B";

        // Place queen
        int random3 = (int)(Math.random() * 8);
        while (random3 == random1 || random3 == random2) {
            random3 = (int)(Math.random() * 8);
        }
        chessBoard[0][random3] = "q";
        chessBoard[7][random3] = "Q";

        // Place two knights
        int random4 = (int)(Math.random() * 8);
        while (random4 == random1 || random4 == random2 || random4 == random3) {
            random4 = (int)(Math.random() * 8);
        }
        chessBoard[0][random4] = "n";
        chessBoard[7][random4] = "N";

        int random5 = (int)(Math.random() * 8);
        while (random5 == random1 || random5 == random2 || random5 == random3 || random5 == random4) {
            random5 = (int)(Math.random() * 8);
        }
        chessBoard[0][random5] = "n";
        chessBoard[7][random5] = "N";

        // Place rook, king, rook using counter-style
        int counter = 0;
        while (!" ".equals(chessBoard[0][counter])) {
            counter++;
        }
        chessBoard[0][counter] = "r";
        chessBoard[7][counter] = "R";

        while (!" ".equals(chessBoard[0][counter])) {
            counter++;
        }
        chessBoard[0][counter] = "k";
        chessBoard[7][counter] = "K";

        while (!" ".equals(chessBoard[0][counter])) {
            counter++;
        }
        chessBoard[0][counter] = "r";
        chessBoard[7][counter] = "R";

        arrayToBitboards(chessBoard, WP, WN, WB, WR, WQ, WK, BP, BN, BB, BR, BQ, BK);
    }

    public static void arrayToBitboards(String[][] chessBoard, long WP, long WN, long WB, long WR, long WQ, long WK,
                                        long BP, long BN, long BB, long BR, long BQ, long BK) {
        for (int i = 0; i < 64; i++) {
            String Binary = "0000000000000000000000000000000000000000000000000000000000000000";
            Binary = Binary.substring(i + 1) + "1" + Binary.substring(0, i);

            switch (chessBoard[i / 8][i % 8]) {
                case "P" -> WP += convertStringToBitboard(Binary);
                case "N" -> WN += convertStringToBitboard(Binary);
                case "B" -> WB += convertStringToBitboard(Binary);
                case "R" -> WR += convertStringToBitboard(Binary);
                case "Q" -> WQ += convertStringToBitboard(Binary);
                case "K" -> WK += convertStringToBitboard(Binary);
                case "p" -> BP += convertStringToBitboard(Binary);
                case "n" -> BN += convertStringToBitboard(Binary);
                case "b" -> BB += convertStringToBitboard(Binary);
                case "r" -> BR += convertStringToBitboard(Binary);
                case "q" -> BQ += convertStringToBitboard(Binary);
                case "k" -> BK += convertStringToBitboard(Binary);
            }
        }
        drawArray(WP, WN, WB, WR, WQ, WK, BP, BN, BB, BR, BQ, BK);
    }

    public static long convertStringToBitboard(String Binary) {
        if (Binary.charAt(0) == '0') {
            return Long.parseLong(Binary, 2);
        } else {
            return Long.parseLong("1" + Binary.substring(2), 2) * 2;
        }
    }

    public static void drawArray(long WP, long WN, long WB, long WR, long WQ, long WK,
                                 long BP, long BN, long BB, long BR, long BQ, long BK) {
        String[][] chessBoard = new String[8][8];

        for (int i = 0; i < 64; i++) {
            chessBoard[i / 8][i % 8] = " ";
        }

        for (int i = 0; i < 64; i++) {
            if (((WP >>> i) & 1) == 1) chessBoard[i / 8][i % 8] = "P";
            if (((WN >>> i) & 1) == 1) chessBoard[i / 8][i % 8] = "N";
            if (((WB >>> i) & 1) == 1) chessBoard[i / 8][i % 8] = "B";
            if (((WR >>> i) & 1) == 1) chessBoard[i / 8][i % 8] = "R";
            if (((WQ >>> i) & 1) == 1) chessBoard[i / 8][i % 8] = "Q";
            if (((WK >>> i) & 1) == 1) chessBoard[i / 8][i % 8] = "K";
            if (((BP >>> i) & 1) == 1) chessBoard[i / 8][i % 8] = "p";
            if (((BN >>> i) & 1) == 1) chessBoard[i / 8][i % 8] = "n";
            if (((BB >>> i) & 1) == 1) chessBoard[i / 8][i % 8] = "b";
            if (((BR >>> i) & 1) == 1) chessBoard[i / 8][i % 8] = "r";
            if (((BQ >>> i) & 1) == 1) chessBoard[i / 8][i % 8] = "q";
            if (((BK >>> i) & 1) == 1) chessBoard[i / 8][i % 8] = "k";
        }

        for (int i = 0; i < 8; i++) {
            System.out.println(Arrays.toString(chessBoard[i]));
        }
    }
}
