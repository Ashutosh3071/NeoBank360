// src/main/java/com/neobank/repository/DevMailRepository.java

package com.neobank.repository;

import com.neobank.entity.DevMail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DevMailRepository extends JpaRepository<DevMail, Long> {

    // Fetch all unseen mails ordered by newest first
    List<DevMail> findBySeenFalseOrderByCreatedAtDesc();

    // Mark a single mail as seen
    @Modifying
    @Query("UPDATE DevMail m SET m.seen = true WHERE m.id = :id")
    void markAsSeen(@Param("id") Long id);

    // Mark all as seen
    @Modifying
    @Query("UPDATE DevMail m SET m.seen = true WHERE m.seen = false")
    void markAllAsSeen();
}