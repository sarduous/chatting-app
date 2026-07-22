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
import java.util.List;
import java.util.stream.Collectors;

import com.salih.wp_prototype.model.chatmodel;
import com.salih.wp_prototype.model.kullanici;
import com.salih.wp_prototype.repository.ChatRepository;
import com.salih.wp_prototype.repository.UserRepository;
import com.salih.wp_prototype.password.encryption;

@Controller
public class chatcontroller {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatRepository chatRepository;
    private final UserRepository userRepository;

    public chatcontroller(SimpMessagingTemplate messagingTemplate, ChatRepository chatRepository,
            UserRepository userRepository) {
        this.messagingTemplate = messagingTemplate;
        this.chatRepository = chatRepository;
        this.userRepository = userRepository;
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

            for (String oda : odalar) {
                // kendi ismimizi ve alt çizgiyi sil, geriye sadece karşı taraf kalsın
                String karsiTaraf = oda.replace(kullaniciAdi, "").replace("_", "");
                sohbetEdilenKisiler.add(karsiTaraf);
            }

            return ResponseEntity.ok(sohbetEdilenKisiler);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(null);
        }
    }
}