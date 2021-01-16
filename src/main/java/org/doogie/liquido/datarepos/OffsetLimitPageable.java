package org.doogie.liquido.datarepos;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * A Pageable that does not use "pages" but plain offset and limit.
 * Offset does not necessarily need to be a multiple of the page size.
 * It can be any offset. This is what the underlying created SQL will use anyway.
 */
public class OffsetLimitPageable implements Pageable {
	long offset;
	long limit;
	Sort sort = Sort.unsorted();

	public OffsetLimitPageable(long offset, long limit) {
		if (offset < 0) offset = 0;
		this.offset = offset;
		this.limit = limit;
	}

	public OffsetLimitPageable(long offset, long limit, Sort sort) {
		if (offset < 0) offset = 0;
		this.offset = offset;
		this.limit = limit;
		this.sort = sort;
	}

	/** page = floor(offset / limit) + 1 */
	@Override
	public int getPageNumber() {
		return (int)Math.floorDiv(offset, limit)+1;
	}

	/** Page size is limit. But can be any size of elements that can be returned. */
	@Override
	public int getPageSize() {
		return (int)this.limit;
	}

	public long getLimit() {
		return this.limit;
	}

	@Override
	public long getOffset() {
		return this.offset;
	}

	@Override
	public Sort getSort() {
		return this.sort;
	}

	@Override
	public Pageable next() {
		return new OffsetLimitPageable(this.offset+this.limit, this.limit, this.sort);
	}

	@Override
	public Pageable previousOrFirst() {
		if (offset - limit <= 0) return this;
		return new OffsetLimitPageable(this.offset-this.limit, this.limit, this.sort);
	}

	@Override
	public Pageable first() {
		return new OffsetLimitPageable(0, this.limit, this.sort);
	}

	@Override
	public boolean hasPrevious() {
		return this.offset - this.limit > 0;
	}
}
