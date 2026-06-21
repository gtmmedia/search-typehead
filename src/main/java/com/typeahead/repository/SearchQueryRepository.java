package com.typeahead.repository;

import com.typeahead.model.SearchQuery;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SearchQueryRepository extends JpaRepository<SearchQuery, String> {
}
