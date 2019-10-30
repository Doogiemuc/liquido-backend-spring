package org.doogie.liquido.model.converter;

import org.doogie.liquido.util.LongMatrix;

import javax.persistence.AttributeConverter;

/**
 * Converter for 2D LongMatrix from/to Json String
 * This is used to store the duelMatrix in a {@link org.doogie.liquido.model.PollModel}
 */
public class MatrixConverter implements AttributeConverter<LongMatrix, String> {

	@Override
	public String convertToDatabaseColumn(LongMatrix matrix) {
		if (matrix == null) return "";
		return matrix.toJsonValue();
	}

	@Override
	public LongMatrix convertToEntityAttribute(String json) {
		if (json == null || json.equals("")) return new LongMatrix(0,0);
		return LongMatrix.fromJsonValue(json);
	}
}
