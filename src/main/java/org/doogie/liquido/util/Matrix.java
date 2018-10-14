package org.doogie.liquido.util;

/**
 * Two dimensional array of ints
 */
public class Matrix {

	private int[][] data;

	public Matrix(int rows, int cols) {
		if (rows == 0 || cols == 0) throw new RuntimeException("SizeX/Y of Matrix must nut be 0");
		this.data = new int[rows][cols];
	}

	public int getRows() {
		return data.length;
	}

	public int getCols() {
		return data[0].length;
	}

	public int get(int i, int j) {
		return this.data[i][j];
	}

	public void set(int i, int j, int val) {
		this.data[i][j] = val;
	}

	public void inc(int i, int j) {
		this.data[i][j]++;
	}

	public void add(int row, int col, int value) {
		this.data[row][col] += value;
	}

	/**
	 * add the value of each elements of m to our elements
	 * @param m another Matrix that MUST have the same size
	 */
	public void add(Matrix m) {
		if (m.getRows() != this.getRows() || m.getCols() != this.getCols())
			throw new RuntimeException("Cannot add matrices of different size");
		for (int i = 0; i < getRows(); i++) {
			for (int j = 0; j < getCols(); j++) {
				this.data[i][j] += m.get(i, j);
			}
		}
	}

	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("[");
		for (int i = 0; i < getRows(); i++) {
			sb.append("(");
			for (int j = 0; j < getCols(); j++) {
				sb.append(this.data[i][j]);
				if (j != getCols()-1) sb.append(",");
			}
  		sb.append(")");
			if (i != getRows()-1) sb.append(", ");
		}
		sb.append("]");
		return sb.toString();
	}

	public String toFormattedString() {
		StringBuilder sb = new StringBuilder();
		sb.append("Matrix["+getRows()+", "+getCols()+"]:\n");
		for (int i = 0; i < getRows(); i++) {
			sb.append("(");
			for (int j = 0; j < getCols(); j++) {
				sb.append(String.format("%2d", this.data[i][j]));
				sb.append(j == getCols()-1 ? ")\n" : ", ");
			}
		}
		return sb.toString();
	}

}