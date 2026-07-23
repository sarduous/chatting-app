package com.salih.wp_prototype.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.salih.wp_prototype.model.chatmodel;
import com.salih.wp_prototype.model.deletingchatrooms;
import com.salih.wp_prototype.model.kullanici;
import com.salih.wp_prototype.repository.ChatGroupRepository;
import com.salih.wp_prototype.repository.ChatRepository;
import com.salih.wp_prototype.repository.UserRepository;
import com.salih.wp_prototype.password.encryption;
import com.salih.wp_prototype.repository.DeletingChatRoomsRepository;
import com.salih.wp_prototype.repository.GroupUserRepository;

@Controller
public class chatcontroller {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatRepository chatRepository;
    private final UserRepository userRepository;
    private final DeletingChatRoomsRepository deletingChatRoomsRepository;
    private final GroupUserRepository groupUserRepository;
    private final ChatGroupRepository chatGroupRepository;
    private final ConcurrentHashMap<String, Boolean> onlineUsers = new ConcurrentHashMap<>();

    public chatcontroller(SimpMessagingTemplate messagingTemplate, ChatRepository chatRepository,
            UserRepository userRepository, DeletingChatRoomsRepository deletingChatRoomsRepository,
            GroupUserRepository groupUserRepository, ChatGroupRepository chatGroupRepository) {
        this.messagingTemplate = messagingTemplate;
        this.chatRepository = chatRepository;
        this.userRepository = userRepository;
        this.deletingChatRoomsRepository = deletingChatRoomsRepository;
        this.groupUserRepository = groupUserRepository;
        this.chatGroupRepository = chatGroupRepository;
    }

    @MessageMapping("/chat")
    public void broadcastMessage(@Payload chatmodel message) {
        chatRepository.save(message);
        messagingTemplate.convertAndSend("/topic/chatroom/" + message.getChatRoomId(), message);
    }

    @GetMapping("/api/mesajlar/{chatRoomId}")
    @ResponseBody
    public List<chatmodel> getMesajGecmisi(@PathVariable String chatRoomId) {
        return chatRepository.findByChatRoomId(chatRoomId);
    }

    // tüm kullanıcıları getirme
    @GetMapping("/api/kullanicilar")
    @ResponseBody
    public List<String> getKullanicilar() {
        return userRepository.findAll().stream()
                .map(kullanici::getUsername)
                .collect(Collectors.toList());
    }

    @PostMapping("/api/login/{username}")
    @ResponseBody
    public String sistemeKaydet(@PathVariable String username, @RequestParam String password) {

        if (!userRepository.existsById(username)) {
            kullanici yeniKullanici = new kullanici(username);

            String sifreliParola = encryption.encrypt(password);
            yeniKullanici.setPassword(sifreliParola);

            userRepository.save(yeniKullanici);
            return "Başarılı";

        } else {

            kullanici mevcutKullanici = userRepository.findById(username).get();

            if (mevcutKullanici.getPassword() == null) {
                return "Hata: Eski kullanıcı kaydında parola bulunmuyor, lütfen yeni bir hesap açın veya veritabanını güncelleyin.";
            }

            String cozulmusSifre = encryption.decrypt(mevcutKullanici.getPassword());
            if (cozulmusSifre.equals(password)) {
                return "Başarılı";
            } else {
                return "Parolanız hatalı!";
            }
        }
    }

    @MessageMapping("/typing")
    public void typing(@Payload java.util.Map<String, String> payload) {
        String roomId = payload.get("chatRoomId");
        messagingTemplate.convertAndSend("/topic/typing/" + roomId, payload);
    }

    @GetMapping("/api/kullanicilar/{kullaniciAdi}/ikili-sohbetler")
    public ResponseEntity<List<String>> getIkiliSohbetler(@PathVariable String kullaniciAdi) {
        try {
            // odayı bul x_x
            List<String> odalar = chatRepository.findIkiliSohbetOdalarim(kullaniciAdi);
            List<String> sohbetEdilenKisiler = new ArrayList<>();
            List<String> gizlenenOdalar = deletingChatRoomsRepository.findGizlenenOdalarByKullanici(kullaniciAdi);

            for (String oda : odalar) {

                if (gizlenenOdalar.contains(oda)) {
                    continue;
                }
                // kendi ismimizi ve alt çizgiyi sil, geriye sadece karşı taraf kalsın
                String karsiTaraf = oda.replace(kullaniciAdi, "").replace("_", "");
                sohbetEdilenKisiler.add(karsiTaraf);
            }

            return ResponseEntity.ok(sohbetEdilenKisiler);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(null);
        }
    }

    @PostMapping("/api/sohbet/sil")
    public ResponseEntity<String> sohbetiSil(@RequestParam String kullaniciAdi, @RequestParam String chatRoomId) {
        try {
            // soft delete
            if (!deletingChatRoomsRepository.existsByUsernameAndChatRoomId(kullaniciAdi, chatRoomId)) {
                deletingchatrooms yeniKayit = new deletingchatrooms(kullaniciAdi, chatRoomId);
                deletingChatRoomsRepository.save(yeniKayit);
            }

            // karşılıklı sohbet için hard delete kontrolü
            if (!chatRoomId.startsWith("GROUP_")) {
                long silenKisiSayisi = deletingChatRoomsRepository.countByChatRoomId(chatRoomId);
                if (silenKisiSayisi == 2) {
                    chatRepository.deleteByChatRoomId(chatRoomId);
                    deletingChatRoomsRepository.deleteByChatRoomId(chatRoomId);

                    System.out.println(
                            chatRoomId + " odası ve içindeki tüm veriler veritabanından KALICI OLARAK silindi!");
                }
            } else {

                Long groupId = Long.parseLong(chatRoomId.replace("GROUP_", ""));

                // grupta kaç kişi olduğunu GroupUserRepositoryden sayısını çekiyoruz
                long toplamUyeSayisi = groupUserRepository.findByGroupId(groupId).size();

                // bu grubu kendineden silenlerin sayısını buluyoruz
                long silenKisiSayisi = deletingChatRoomsRepository.countByChatRoomId(chatRoomId);

                // gruptaki herkes (toplam üye sayısı == silen kişi sayısı) sildiyse
                if (toplamUyeSayisi > 0 && silenKisiSayisi == toplamUyeSayisi) {

                    // gruba ait tüm mesajlar ve logları sil
                    chatRepository.deleteByChatRoomId(chatRoomId);
                    // silinen sohbetler tablosundan silme
                    deletingChatRoomsRepository.deleteByChatRoomId(chatRoomId);
                    groupUserRepository.deleteByGroupId(groupId);
                    chatGroupRepository.deleteById(groupId);

                    System.out
                            .println(chatRoomId + " grubunun mesajları tüm üyeler sildiği için kalıcı olarak silindi!");
                }

            }

            return ResponseEntity.ok("Başarılı");
        } catch (

        Exception e) {
            return ResponseEntity.internalServerError().body("Hata");
        }
    }

    // websocket üzerinden gelen online/offline durumunu alır
    @MessageMapping("/durum")
    public void durumGuncelle(@Payload Map<String, String> payload) {
        String kullanici = payload.get("kullaniciAdi");
        String durum = payload.get("durum");

        if ("ONLINE".equals(durum)) {
            onlineUsers.put(kullanici, true);
        } else {
            onlineUsers.remove(kullanici);
        }

        messagingTemplate.convertAndSend("/topic/durum", payload);
    }

    // biri ilk girdiğinde karşı tarafın o anlık durumunu öğrenmesi için
    @GetMapping("/api/durum/{kullaniciAdi}")
    @ResponseBody
    public boolean durumSorgula(@PathVariable String kullaniciAdi) {
        return onlineUsers.containsKey(kullaniciAdi);
    }

    // websocketten gelen mesajı sil'i dinler
    @MessageMapping("/chat.delete")
    public void mesajSilSinyali(@Payload Map<String, String> payload) {
        Long mesajId = Long.parseLong(payload.get("mesajId"));
        String chatRoomId = payload.get("chatRoomId");
        // hard delete
        chatRepository.deleteById(mesajId);

        // odadaki herkesten mesajı silme
        Map<String, Object> bildirim = new HashMap<>();
        bildirim.put("tip", "SİL");
        bildirim.put("silinecekId", mesajId);

        messagingTemplate.convertAndSend("/topic/chatroom/" + chatRoomId, (Object) bildirim);
    }
}