package org.doogie.liquido.rest.dto;

import lombok.Data;
import org.doogie.liquido.model.LawModel;
import org.springframework.data.domain.Sort;
import org.springframework.lang.Nullable;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;

/**
 * Filter, paging and sorting parmateres for server side filtering of LawModels.
 * @see org.doogie.liquido.services.LawService#findBySearchQuery(LawQuery)
 */
@Data
public class LawQuery {

	// filter criteria (all criterias are optional)
	Optional<LawModel.LawStatus> status = Optional.empty();
	Optional<String> searchText = Optional.empty();
	Optional<Date> updatedAfter = Optional.empty();
	Optional<Date> updatedBefore= Optional.empty();
	Optional<String> areaTitle = Optional.empty();
	Optional<String> createdByEmail = Optional.empty();
	Optional<String> supportedByEMail = Optional.empty();

	// paging and sorting
	int page = 0;
	int size = 10;
	Sort.Direction direction = Sort.DEFAULT_DIRECTION;
	List<String> sortByProperties = new ArrayList<>();

	public void setStatus(@Nullable LawModel.LawStatus status) {
		this.status = Optional.of(status);
	}

	public void setSearchText(String searchText) {
		this.searchText = Optional.of(searchText);
	}

	public void setUpdatedAfter(Date updatedAfter) {
		this.updatedAfter = Optional.of(updatedAfter);
	}

	public void setUpdatedBefore(Date updatedBefore) {
		this.updatedBefore = Optional.of(updatedBefore);
	}

	public void setAreaTitle(String areaTitle) {
		this.areaTitle = Optional.of(areaTitle);
	}

	public void setCreatedByEmail(String createdByEmail) {
		this.createdByEmail = Optional.of(createdByEmail);
	}

	public void setSupportedByEMail(String supportedByEMail) {
		this.supportedByEMail = Optional.of(supportedByEMail);
	}

	public String[] getSortbyPropertiesAsStringArray() {
		return sortByProperties.toArray(new String[sortByProperties.size()]);
	}

	public void addSortProperty(String prop) {
		this.sortByProperties.add(prop);
	}

	public void setSingleSortProperty(String prop) {
		this.sortByProperties.clear();
		this.sortByProperties.add(prop);
	}

}
