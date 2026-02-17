package com.cyberheist;

import javafx.scene.paint.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents one of the 7 Tetris tetrominoes, with precomputed rotations.
 */
public class Piece {
    // The 7 color choices in the same order as TYPES
    public static final Color[] PIECE_COLORS = {
            Color.CYAN, Color.BLUE, Color.ORANGE,
            Color.YELLOW, Color.GREEN, Color.PURPLE, Color.RED
    };

    // All 7 piece definitions: each is an array of rotations, each rotation is a small 2D int matrix.
    private static final int[][][][] SHAPES = {
            // I-piece
            {{{1,1,1,1}},
                    {{1},{1},{1},{1}}},
            // J-piece
            {{{1,0,0},
                    {1,1,1}},
                    {{1,1},{1,0},{1,0}},
                    {{1,1,1},{0,0,1}},
                    {{0,1},{0,1},{1,1}}},
            // L-piece
            {{{0,0,1},
                    {1,1,1}},
                    {{1,0},{1,0},{1,1}},
                    {{1,1,1},{1,0,0}},
                    {{1,1},{0,1},{0,1}}},
            // O-piece
            {{{1,1},
                    {1,1}}},
            // S-piece
            {{{0,1,1},
                    {1,1,0}},
                    {{1,0},{1,1},{0,1}}},
            // T-piece
            {{{0,1,0},
                    {1,1,1}},
                    {{1,0},{1,1},{1,0}},
                    {{1,1,1},{0,1,0}},
                    {{0,1},{1,1},{0,1}}},
            // Z-piece
            {{{1,1,0},
                    {0,1,1}},
                    {{0,1},{1,1},{1,0}}}
    };

    private final int type;       // 0–6 index into SHAPES
    private final int rotation;   // which rotation 0..(n−1)

    public Piece(int type, int rotation) {
        this.type = type;
        int rCount = SHAPES[type].length;
        this.rotation = (rotation % rCount + rCount) % rCount;
    }

    /** Returns a new Piece rotated one step counter-clockwise. */
    public Piece rotated() {
        return new Piece(type, rotation + 1);
    }

    /** The current shape matrix: 1 = block, 0 = empty. */
    public int[][] shape() {
        return SHAPES[type][rotation];
    }

    /** The width in cells of this rotation. */
    public int width() {
        return shape()[0].length;
    }

    /** The height in cells of this rotation. */
    public int height() {
        return shape().length;
    }

    /** The piece’s type index (0–6). */
    public int type() {
        return type;
    }

    /** The display color for this piece. */
    public Color color() {
        return PIECE_COLORS[type];
    }

    /** Factory: get a new random piece. */
    public static Piece randomPiece() {
        int t = (int) (Math.random() * SHAPES.length);
        return new Piece(t, 0);
    }
}
