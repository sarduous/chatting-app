package com.salih.wp_prototype.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.salih.wp_prototype.model.kullanici;

public interface UserRepository extends JpaRepository<kullanici, String> {
}