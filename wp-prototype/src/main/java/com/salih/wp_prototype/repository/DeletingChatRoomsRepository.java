package com.salih.wp_prototype.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.transaction.annotation.Transactional;

import com.salih.wp_prototype.model.deletingchatrooms;
import java.util.List;

public interface DeletingChatRoomsRepository extends JpaRepository<deletingchatrooms, Long> {

    // acaba bu odayı sildi mi diye filtrelemek için
    @Query("SELECT d.chatRoomId FROM deletingchatrooms d WHERE d.username = :kullaniciAdi")
    List<String> findGizlenenOdalarByKullanici(@Param("kullaniciAdi") String kullaniciAdi);// silinen odalar hiç
                                                                                           // gösterilmeyecek

    // bir odada kaç kişi sohbeti silmiş, herkes sildiyse yok et
    long countByChatRoomId(String chatRoomId);

    // aynı odayı iki defa silmeye çalışırsa diye kontrol
    boolean existsByUsernameAndChatRoomId(String username, String chatRoomId);

    @Modifying
    @Transactional
    void deleteByChatRoomId(String chatRoomId);
}