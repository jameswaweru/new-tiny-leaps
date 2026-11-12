package com.tinyleaps.catalogue.repository;

import com.tinyleaps.catalogue.entity.VirtualGame;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface VirtualGameRepository extends JpaRepository<VirtualGame, UUID> {

    @Query("""
            select g from VirtualGame g
            where g.status = 'ACTIVE'
              and (:category is null or g.category = :category)
              and (:age is null or (g.minAge <= :age and g.maxAge >= :age))
            order by g.sortOrder asc, g.title asc
            """)
    Page<VirtualGame> search(@Param("category") String category, @Param("age") Integer age, Pageable pageable);

    Optional<VirtualGame> findByGameIdAndStatus(String gameId, String status);

    @Query("select distinct g.category from VirtualGame g where g.status = 'ACTIVE' order by g.category")
    List<String> activeCategories();
}
