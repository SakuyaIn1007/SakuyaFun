package com.sakuya.backend.catalog;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface Wenku8CatalogEntryRepository extends JpaRepository<Wenku8CatalogEntry, String> {
    List<Wenku8CatalogEntry> findByFeedOrderByDisplayOrderAsc(String feed);
    void deleteByFeed(String feed);
}
