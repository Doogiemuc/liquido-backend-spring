package org.doogie.liquido.util;

import java.util.function.Function;

/**
 * Two dimensional array of ints.
 */
public class Matrix {
  // Implementation note: Be carefull not to accidentically invert the matrix.
	// All methods with two parameters in here think of the order "row" and then "col"
	// This different than "x" and then "y" axis!  But that may only be of importance to completely fanatic geeks :-)

	private int[][] data;

	/**
	 * Create a new matrix. All values are 0 by default.
	 * An empty Matrix of limit 0,0 is allowed.
	 * @param rows
	 * @param cols
	 */
	public Matrix(int rows, int cols) {
		if (rows < 0 || cols < 0) throw new IllegalArgumentException("rows and cols must be positive");
		this.data = new int[rows][cols];
	}

	public int getRows() {
		return data.length;
	}

	public int getCols() {
		if (data.length == 0) return 0;  //BUGFIX for edge case Matrix(0,0)
		return data[0].length;
	}

	public int get(int i, int j) {
		return this.data[i][j];
	}

	public int[][] getRawData() { return this.data; }

	public void set(int i, int j, int val) {
		this.data[i][j] = val;
	}

	public void inc(int i, int j) {
		this.data[i][j]++;
	}

	public void dec(int i, int j) {
		this.data[i][j]--;
	}

	public void add(int row, int col, int value) {
		this.data[row][col] += value;
	}

	/**
	 * Add the value of each elements of m to our elements
	 * If m is larger than this matrix, then only the values that fit into this matrix will be added. This matrix will not be resized.
	 * If m is smaller then this matrix, then only those rows and cols from m will be added to this matrix.
	 * @param m another Matrix
	 */
	public void add(Matrix m) {
		for (int i = 0; i < Math.min(getRows(), m.getRows()); i++) {
			for (int j = 0; j < Math.min(getCols(), m.getCols()); j++) {
				this.add(i,j, m.get(i,j));
			}
		}
	}

	//TODO: inverse
	//TODO: multiply

	/**
	 * Map all integer values of this matrix into a new Matrix
	 * @param mapper mapper function Int -> Int
	 * @return the newly created Matrix
	 */
	public Matrix map(Function<Integer, Integer> mapper) {
		Matrix result = new Matrix(this.getRows(), this.getCols());
		for (int i = 0; i < getRows(); i++) {
			for (int j = 0; j < getCols(); j++) {
				result.set(i, j, mapper.apply(this.get(i, j)));
			}
		}
		return result;
	}

	/**
	 * Resizes the Matrix to the new dimensions and copies the existing data.
	 * If new dimensions are smaller than the existing ones, data will be clipped.
	 * @param newRows new height
	 * @param newCols new width
	 */
	public void resize(int newRows, int newCols) {
		int[][] newData = new int[newRows][newCols];
		for (int i = 0; i < Math.min(getRows(), newRows); i++) {
			for (int j = 0; j < Math.min(getCols(), newCols); j++) {
				newData[i][j] = this.get(i,j);
			}
		}
		this.data = newData;
	}


	public String toString() {
		return toJsonValue();
	}

	/**
	 * Create a Matrix from a JSON array of arrays, e.g. [[1,2,3],[4,5,6],[7,8,9]]
	 * Edge case: "[]" will be converted to a Matrix of limit (0,0)
	 * @param json a json value that contains an array of arrays
	 * @return
	 * @throws IllegalArgumentException when json is null or does not start with [ or does not end with ]
	 * @throws RuntimeException when the the rows in the array do not have the same length
	 * @throws NumberFormatException when any of the values inside the arrays is not an int
	 */
	public static Matrix fromJsonValue(String json) {
		if (json == null) throw new IllegalArgumentException("Cannot create a Matrix object from <null>");  // never ever return null anywhere!
		if (!json.startsWith("[") || !json.endsWith("]")) throw new IllegalArgumentException("JSON must start with [ and end with ] (array of arrays) to create a Matrix object. Cannot convert from '"+json+"'");
		if (json.length() == 2) return new Matrix(0,0);

		json = json.substring(2, json.length()-2);
		String[] parts = json.split("\\], ?\\[");
		int rows = parts.length;
		int cols = parts[0].split(",").length;
		Matrix matrix = new Matrix(rows, cols);
		for (int i = 0; i < parts.length; i++) {
			String[] values = parts[i].split(",");
			if (values.length != cols) throw new RuntimeException("Cannot parse Matrix form JSON. Invalid data for Matrix. Rows in data do not have the same width.");
			for (int j = 0; j < cols; j++) {
				matrix.set(i,j, Integer.valueOf(values[j]));
			}
		}
		return matrix;
	}

	public String toJsonValue() {
		StringBuilder sb = new StringBuilder();
		sb.append('[');
		for (int i = 0; i < getRows(); i++) {
			sb.append('[');
			for (int j = 0; j < getCols(); j++) {
				sb.append(get(i,j));
				if (j < getCols()-1) sb.append(",");
			}
			sb.append(']');
			if (i < getRows()-1) sb.append(",");
		}
		sb.append(']');
		return sb.toString();
	}

}