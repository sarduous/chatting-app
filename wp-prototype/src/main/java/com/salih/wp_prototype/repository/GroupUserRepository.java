package com.salih.wp_prototype.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.salih.wp_prototype.model.groupkullanici;
import java.util.List;
import org.springframework.transaction.annotation.Transactional;

public interface GroupUserRepository extends JpaRepository<groupkullanici, Long> {
    @Transactional
    void deleteByGroupIdAndUsername(Long groupId, String username);

    List<groupkullanici> findByUsername(String username);

    List<groupkullanici> findByGroupId(Long groupId);
}