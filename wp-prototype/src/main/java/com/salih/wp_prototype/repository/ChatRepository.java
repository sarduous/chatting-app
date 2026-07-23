package com.salih.wp_prototype.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import com.salih.wp_prototype.model.chatmodel;
import java.util.List;

public interface ChatRepository extends JpaRepository<chatmodel, Long> {
    List<chatmodel> findByChatRoomId(String chatRoomId);

    @Query("SELECT DISTINCT c.user_sender FROM chatmodel c")
    List<String> findTumKullanicilar();

    @Query("SELECT DISTINCT c.chatRoomId FROM chatmodel c WHERE c.chatRoomId LIKE CONCAT('%', :kullaniciAdi, '%') AND c.chatRoomId NOT LIKE 'GROUP_%'")
    List<String> findIkiliSohbetOdalarim(@Param("kullaniciAdi") String kullaniciAdi);

    @Modifying
    @Transactional
    void deleteByChatRoomId(String chatRoomId);
}
