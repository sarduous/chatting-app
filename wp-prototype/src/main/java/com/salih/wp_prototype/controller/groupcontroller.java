package com.salih.wp_prototype.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;

import com.salih.wp_prototype.model.chatgroup;
import com.salih.wp_prototype.model.groupkullanici;
import com.salih.wp_prototype.repository.ChatGroupRepository;
import com.salih.wp_prototype.repository.GroupUserRepository;

@Controller
public class groupcontroller {

    private final ChatGroupRepository chatGroupRepository;
    private final GroupUserRepository groupUserRepository;

    public groupcontroller(ChatGroupRepository chatGroupRepository, GroupUserRepository groupUserRepository) {
        this.chatGroupRepository = chatGroupRepository;
        this.groupUserRepository = groupUserRepository;
    }

    @PostMapping("/api/gruplar/olustur")
    @ResponseBody
    public chatgroup grupOlustur(@RequestParam String grupAdi, @RequestParam List<String> uyeler) {

        chatgroup yeniGrup = new chatgroup();
        yeniGrup.setGroupName(grupAdi);
        chatGroupRepository.save(yeniGrup);

        for (String uye : uyeler) {
            groupkullanici gk = new groupkullanici();
            gk.setGroupId(yeniGrup.getId());
            gk.setUsername(uye);
            groupUserRepository.save(gk);
        }

        return yeniGrup;
    }

    @PostMapping("/api/gruplar/{grupId}/kullanicilar/{kullaniciAdi}")
    public ResponseEntity<String> grubaKullaniciEkle(@PathVariable Long grupId, @PathVariable String kullaniciAdi) {
        try {
            // 1. Yeni bir köprü nesnesi (masada yeni bir satır) oluşturuyoruz
            groupkullanici yeniKayit = new groupkullanici();

            // 2. Bilgileri içine dolduruyoruz
            yeniKayit.setGroupId(grupId);
            yeniKayit.setUsername(kullaniciAdi);

            // 3. Veritabanına kaydetmesini söylüyoruz
            groupUserRepository.save(yeniKayit);

            return ResponseEntity.ok("Kullanıcı başarıyla eklendi.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Ekleme başarısız: " + e.getMessage());
        }
    }

    // kayıtlı kullanıcının grupları
    @GetMapping("/api/gruplar/benim/{kullaniciAdi}")
    @ResponseBody
    public List<chatgroup> benimGruplarim(@PathVariable String kullaniciAdi) {

        List<groupkullanici> uyeOldugumKayitlar = groupUserRepository.findByUsername(kullaniciAdi);

        List<chatgroup> gruplarim = new ArrayList<>();
        for (groupkullanici kayit : uyeOldugumKayitlar) {
            chatGroupRepository.findById(kayit.getGroupId()).ifPresent(gruplarim::add);
        }

        return gruplarim;
    }

    @GetMapping("/api/gruplar/{grupId}/kullanicilar")
    public ResponseEntity<List<String>> getGrupKullanicilari(@PathVariable Long grupId) {

        List<groupkullanici> kullaniciKayitlari = groupUserRepository.findByGroupId(grupId);

        List<String> kullaniciIsimleri = kullaniciKayitlari.stream()
                .map(groupkullanici::getUsername)
                .collect(Collectors.toList());

        return ResponseEntity.ok(kullaniciIsimleri);
    }

    @DeleteMapping("/api/gruplar/{grupId}/kullanicilar/{kullaniciAdi}")
    public ResponseEntity<String> gruptanKullaniciCikar(@PathVariable Long grupId, @PathVariable String kullaniciAdi) {
        try {
            groupUserRepository.deleteByGroupIdAndUsername(grupId, kullaniciAdi);
            return ResponseEntity.ok("Kullanıcı gruptan başarıyla çıkarıldı.");
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body("Silme işlemi başarısız: " + e.getMessage());
        }
    }

}