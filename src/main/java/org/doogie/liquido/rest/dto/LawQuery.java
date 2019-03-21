package org.doogie.liquido.rest.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import org.doogie.liquido.model.LawModel;
import org.springframework.data.domain.Sort;
import org.springframework.lang.Nullable;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Filter criteria, paging and sorting parameters for server side filtering of LawModels.
 * @see org.doogie.liquido.services.LawService#findBySearchQuery(LawQuery)
 */
@Data
@JsonInclude(JsonInclude.Include.NON_ABSENT)
public class LawQuery {

	// filter criteria (all are optional)
	Optional<List<LawModel.LawStatus>> statusList = Optional.empty();  // List of status values to filter for. Empty list or Optional.empty() returns all laws in any status
	Optional<String> searchText = Optional.empty();
	Optional<Date> updatedAfter = Optional.empty();
	Optional<Date> updatedBefore= Optional.empty();
	Optional<Long> areaId = Optional.empty();
	Optional<String> areaTitle = Optional.empty();
	Optional<String> createdByEmail = Optional.empty();
	Optional<String> supportedByEMail = Optional.empty();

	long offset = 0;   // start index from where we will load data. (This of course depends on sort settings.)
	long limit = 20;   // How many rows will be loaded and returned.  (This corresponds to one "page")
	Sort.Direction direction = Sort.DEFAULT_DIRECTION;
	List<String> sortByProperties = new ArrayList<>();

	public void setStatusList(@Nullable List<LawModel.LawStatus> statusList) {
		this.statusList = Optional.of(statusList);
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

	@JsonIgnore
	public String[] getSortByPropertiesAsStringArray() {
		return sortByProperties.toArray(new String[sortByProperties.size()]);
	}

	public void addSortProperty(String prop) {
		this.sortByProperties.add(prop);
	}

	public void setSingleSortProperty(String prop) {
		this.sortByProperties.clear();
		this.sortByProperties.add(prop);
	}

	@Override
	public String toString() {
		final StringBuffer sb = new StringBuffer("LawQuery[");
		if (statusList.isPresent()) sb.append(", status=").append(statusList);
		if (searchText.isPresent()) sb.append(", searchText=").append(searchText);
		if (updatedAfter.isPresent()) sb.append(", updatedAfter=").append(updatedAfter);
		if (updatedBefore.isPresent()) sb.append(", updatedBefore=").append(updatedBefore);
		if (areaTitle.isPresent()) sb.append(", areaTitle=").append(areaTitle);
		if (areaId.isPresent()) sb.append(", areaId=").append(areaId);
		if (createdByEmail.isPresent()) sb.append(", createdByEmail=").append(createdByEmail);
		if (supportedByEMail.isPresent()) sb.append(", supportedByEMail=").append(supportedByEMail);
		sb.append(", offset=").append(offset);
		sb.append(", limit=").append(limit);
		if (sortByProperties.size() > 0) {
			sb.append(", direction=").append(direction);
			String props = sortByProperties.stream().collect(Collectors.joining(","));
			sb.append(", sortByProperties=").append(props);
		}
		sb.append(']');
		return sb.toString();
	}
}
