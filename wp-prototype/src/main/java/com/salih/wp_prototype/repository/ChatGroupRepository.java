package com.salih.wp_prototype.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.salih.wp_prototype.model.chatgroup;

public interface ChatGroupRepository extends JpaRepository<chatgroup, Long> {
}