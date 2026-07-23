let seciliMesajId = null; // Menü için gerekli ID tanımı eklendi

const myGlobalName = sessionStorage.getItem('chatKullaniciAdi');
const myRoomName = sessionStorage.getItem('chatOdasi');
const karsiTaraf = sessionStorage.getItem('konusulanKisi');

if (!myGlobalName || !myRoomName) {
    window.location.href = '/login.html';
}

document.querySelector('.header h2').innerText = karsiTaraf || 'Sohbet Odası';

const stompClient = new window.StompJs.Client({
    brokerURL: 'ws://' + window.location.host + '/ws-chat'
});

const aktifYazanlar = new Set();

stompClient.onConnect = (frame) => {
    stompClient.subscribe('/topic/chatroom/' + myRoomName, (mesajPaketi) => {
        const gelenMesaj = JSON.parse(mesajPaketi.body);
        if (gelenMesaj.tip === "SİL") {
            const silinecekBalon = document.querySelector(`.message-box[data-id="${gelenMesaj.silinecekId}"]`);
            if (silinecekBalon) {
                silinecekBalon.remove();
            }
        } else {
            mesajgoster(gelenMesaj);
        }
    });

    stompClient.subscribe('/topic/typing/' + myRoomName, (paket) => {
        const data = JSON.parse(paket.body);

        if (data.user_sender !== myGlobalName) {
            let degisiklikOlduMu = false;

            if (data.isTyping === "true" || data.isTyping === true) {
                if (!aktifYazanlar.has(data.user_sender)) {
                    aktifYazanlar.add(data.user_sender);
                    degisiklikOlduMu = true;
                }
            } else {
                if (aktifYazanlar.has(data.user_sender)) {
                    aktifYazanlar.delete(data.user_sender);
                    degisiklikOlduMu = true;
                }
            }

            if (degisiklikOlduMu) {
                yaziyorArayuzunuGuncelle();
            }
        }
    });

    // kendi online durumumuz
    stompClient.publish({
        destination: '/app/durum',
        body: JSON.stringify({ kullaniciAdi: myGlobalName, durum: 'ONLINE' })
    });

    // başkalarının durumu
    stompClient.subscribe('/topic/durum', (paket) => {
        const data = JSON.parse(paket.body);

        if (data.kullaniciAdi === karsiTaraf && !myRoomName.startsWith("GROUP_")) {
            arayuzDurumGuncelle(data.durum === 'ONLINE');
        }

        // grup modalının içinde kontrol
        const grupNoktasi = document.getElementById('grup-durum-' + data.kullaniciAdi);

        if (grupNoktasi) { // eğer o an liste açıksa ve nokta oradaysa
            if (data.durum === 'ONLINE') {
                grupNoktasi.style.color = '#4cd137';
                grupNoktasi.title = 'Çevrimiçi';
            } else {
                grupNoktasi.style.color = '#ddd';
                grupNoktasi.title = 'Çevrimdışı';
            }
        }
    });
};

stompClient.onWebSocketError = (error) => {
    console.error('Bağlantı Hatası', error);
};

function sendMessage() {
    const mesajIcerik = document.getElementById('message-input').value.trim();
    const mesajZaman = new Date().toLocaleString('tr-TR', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit'
    }).replace(' ', ' - ');

    if (mesajIcerik) {
        const gonderilecekObje = {
            user_sender: myGlobalName,
            text: mesajIcerik,
            chatRoomId: myRoomName,
            timestamp: mesajZaman
        };

        stompClient.publish({
            destination: '/app/chat',
            body: JSON.stringify(gonderilecekObje)
        });

        document.getElementById('message-input').value = '';
    }
}

function handleKeyPress(e) {
    if (e.key === 'Enter') {
        sendMessage();
    }
}

function mesajgoster(mesaj) {
    const mesajListesi = document.getElementById('message-list');
    const yeniMesaj = document.createElement('div');
    yeniMesaj.className = 'message-box';

    // dbden gelen mesaj idsini html'e gömme
    if (mesaj.id) {
        yeniMesaj.setAttribute('data-id', mesaj.id);
    }

    if (mesaj.user_sender === myGlobalName) {
        yeniMesaj.classList.add('me');
    }

    yeniMesaj.innerHTML = `<strong>${mesaj.user_sender}</strong> ${mesaj.text}<span style="display: block; text-align: right; font-size: 0.7rem; margin-top: 5px; opacity: 0.7;">${mesaj.timestamp}</span>`;

    mesajListesi.appendChild(yeniMesaj);

    const indicator = document.getElementById('typing');
    if (indicator) {
        mesajListesi.appendChild(indicator);
    }

    mesajListesi.scrollTop = mesajListesi.scrollHeight;
}

stompClient.activate();

function tummesajlar() {
    fetch('/api/mesajlar/' + myRoomName)
        .then(cevap => cevap.json())
        .then(mesajlarListesi => {
            mesajlarListesi.forEach(eskiMesaj => {
                mesajgoster(eskiMesaj);
            });
        })
        .catch(hata => console.log("Eski mesajlar çekilirken hata oluştu: ", hata));
}

window.onload = tummesajlar;
let typingTimer;
const typingInterval = 3000;
let suAnYaziyorum = false;

function klavyetiklama() {
    if (!suAnYaziyorum) {
        stompClient.publish({
            destination: '/app/typing',
            body: JSON.stringify({ user_sender: myGlobalName, chatRoomId: myRoomName, isTyping: true })
        });
        suAnYaziyorum = true;
    }

    //eski sayacı iptal et ve 2 saniyeden geriye saymaya başlar
    clearTimeout(typingTimer);
    typingTimer = setTimeout(() => {
        // 2 saniye boyunca harfe basılmazsa yazmayı bıraktı sinyali
        stompClient.publish({
            destination: '/app/typing',
            body: JSON.stringify({ user_sender: myGlobalName, chatRoomId: myRoomName, isTyping: false })
        });
        suAnYaziyorum = false; // 2 saniye bitti, kilidi aç 
    }, typingInterval);
}

const baslikMetni = document.getElementById('grup-adi');

baslikMetni.innerText = karsiTaraf || 'Sohbet Odası';
if (myRoomName && myRoomName.startsWith("GROUP_")) {

    baslikMetni.innerHTML = karsiTaraf + ' <i class="fa-solid fa-users" style="font-size: 1.1rem; margin-left: 8px; opacity: 0.9;"></i>';
    baslikMetni.style.cursor = 'pointer';// fareyle üzerine gelince el işareti
    baslikMetni.title = 'Grup üyelerini görmek için tıklayın';// imleç üzerinde durunca çıkacak yazı
    baslikMetni.onmouseover = function () { this.style.opacity = '0.7'; };
    baslikMetni.onmouseout = function () { this.style.opacity = '1'; };
    baslikMetni.onclick = kullanicilariGoster;

    document.getElementById('user-status-text').style.display = 'none';
} else {
    document.getElementById('user-status-text').style.display = 'flex';

    // sayfa açıldığında karşı taraf o an içeride mi diye backend'e sor
    fetch('/api/durum/' + karsiTaraf)
        .then(res => res.json())
        .then(isOnline => {
            arayuzDurumGuncelle(isOnline);
        })
        .catch(err => console.log("Durum çekilemedi:", err));
}

function uyelerModalKapat() {
    document.getElementById('uyelerModal').style.display = 'none';
}

function kullanicilariGoster() {
    const grupId = myRoomName.replace("GROUP_", "");
    const modal = document.getElementById('uyelerModal');
    const listeContainer = document.getElementById('modal-uyeler-listesi');

    document.getElementById('modal-grup-adi').innerText = karsiTaraf;
    listeContainer.innerHTML = '<div style="text-align:center; color:#888; padding: 20px;">Yükleniyor...</div>';
    modal.style.display = 'flex';

    fetch('/api/gruplar/' + grupId + '/kullanicilar')
        .then(cevap => cevap.json())
        .then(kullanicilar => {

            let kullanicilarHTML = '';

            if (kullanicilar.length === 0) {
                kullanicilarHTML = '<div style="text-align:center; color:#888; padding: 20px;">Grupta üye bulunamadı.</div>';
            } else {
                kullanicilar.forEach(uye => {
                    const basHarf = uye.charAt(0).toUpperCase();
                    const benMiyim = (uye === myGlobalName);
                    const carpiIkonu = benMiyim ? '' : `<i class="fa-solid fa-circle-xmark" style="color: #ff5858; cursor: pointer; font-size: 1.3rem; transition: transform 0.2s;" onmouseover="this.style.transform='scale(1.1)'" onmouseout="this.style.transform='scale(1)'" onclick="kullaniciCikar('${grupId}', '${uye}')" title="Kullanıcıyı Gruptan Çıkar"></i>`;
                    kullanicilarHTML += `
<div style="padding: 10px; border-bottom: 1px solid #eee; display: flex; align-items: center; justify-content: space-between; background: white; border-radius: 8px; margin-bottom: 8px;">
<div style="display: flex; align-items: center; gap: 15px;">
    <div style="width: 35px; height: 35px; border-radius: 50%; background: linear-gradient(135deg, #8e2de2, #f857a6); color: white; display: flex; justify-content: center; align-items: center; font-weight: bold; font-size: 1rem; box-shadow: 0 2px 5px rgba(142, 45, 226, 0.3);">
        ${basHarf}
    </div> 
    <span style="font-weight: 500; font-size: 1.05rem; color: #333; display: flex; align-items: center;">
        ${uye} 
        <span style="font-size: 0.8rem; color: #888; font-weight: normal; margin-left: 5px;">${benMiyim ? '(Sen)' : ''}</span>
        <i id="grup-durum-${uye}" class="fa-solid fa-circle" style="font-size: 0.55rem; color: #ddd; margin-left: 8px; transition: color 0.3s;" title="Çevrimdışı"></i>
    </span>
</div>
<div>
    ${carpiIkonu}
</div>
</div>`;
                });
            }
            listeContainer.innerHTML = kullanicilarHTML;

            kullanicilar.forEach(uye => {
                fetch('/api/durum/' + uye)
                    .then(res => res.json())
                    .then(isOnline => {
                        const durumNoktasi = document.getElementById('grup-durum-' + uye);
                        if (durumNoktasi && isOnline) {
                            durumNoktasi.style.color = '#4cd137';
                            durumNoktasi.title = 'Çevrimiçi';
                        }
                    })
                    .catch(err => console.log("Durum çekilemedi:", err));
            });
        })
        .catch(hata => {
            console.error("Hata oluştu:", hata);
            listeContainer.innerHTML = '<div style="text-align:center; color:#e84118; padding: 20px;">Üyeler çekilirken bir sorun oluştu.</div>';
        });
}

function kullaniciCikar(grupId, kullaniciAdi) {
    Swal.fire({
        title: 'Gruptan çıkarıyorsun!',
        text: `${kullaniciAdi} adlı kullanıcıyı gruptan çıkarmak istediğine emin misin?`,
        icon: 'warning',
        showCancelButton: true,
        confirmButtonColor: '#e84118',
        cancelButtonColor: '#888',
        confirmButtonText: 'Evet, Çıkar',
        cancelButtonText: 'İptal'
    }).then((result) => {
        if (result.isConfirmed) {

            fetch('/api/gruplar/' + grupId + '/kullanicilar/' + kullaniciAdi, {
                method: 'DELETE'
            })
                .then(cevap => {
                    if (cevap.ok) {
                        Swal.fire({
                            title: 'Çıkarıldı!',
                            text: 'Kullanıcı gruptan başarıyla silindi.',
                            icon: 'success',
                            customClass: { confirmButton: 'btn-kapat' },
                            buttonsStyling: false
                        });
                        kullanicilariGoster();
                    } else {
                        throw new Error('Silme işlemi başarısız');
                    }
                })
                .catch(hata => {
                    Swal.fire({
                        title: 'Hata!',
                        text: 'Kullanıcı çıkarılamadı.',
                        icon: 'error',
                        customClass: { confirmButton: 'btn-kapat' },
                        buttonsStyling: false
                    });
                    console.error("Çıkarma hatası:", hata);
                });
        }
    });
}

function kullaniciEkleModalKapat() {
    document.getElementById('kullaniciEkleModal').style.display = 'none';
}

function kullaniciEkleme() {
    document.getElementById('uyelerModal').style.display = 'none';
    const modal = document.getElementById('kullaniciEkleModal');
    const listeContainer = document.getElementById('eklenecek-kullanicilar-listesi');
    const grupId = myRoomName.replace("GROUP_", "");

    listeContainer.innerHTML = '<div style="text-align:center; color:#888;">Yükleniyor...</div>';
    modal.style.display = 'flex';

    const fetchTumKullanicilar = fetch('/api/kullanicilar').then(res => res.json());
    const fetchGrupUyeleri = fetch('/api/gruplar/' + grupId + '/kullanicilar').then(res => res.json());

    Promise.all([fetchTumKullanicilar, fetchGrupUyeleri])
        .then(([tumKullanicilar, grupUyeleri]) => {
            let html = '';

            tumKullanicilar.forEach(kisi => {
                if (!grupUyeleri.includes(kisi) && kisi !== myGlobalName) {
                    html += `
            <label style="display: flex; align-items: center; padding: 6px 0; cursor: pointer; font-size: 0.95rem; color: #333;">
                <input type="checkbox" class="eklenecek-kisi-cb" value="${kisi}" style="margin-right: 10px; width: 16px; height: 16px;">
                ${kisi}
            </label>`;
                }
            });

            if (html === '') {
                html = '<div style="text-align:center; color:#888;">Grup zaten tam kadro! Eklenecek yeni kişi bulunamadı.</div>';
            }

            listeContainer.innerHTML = html;
        })
        .catch(hata => {
            console.error("Kullanıcılar çekilemedi:", hata);
            listeContainer.innerHTML = '<div style="text-align:center; color:#e84118;">Liste çekilemedi.</div>';
        });
}

function secilenleriGrubaEkle() {
    const grupId = myRoomName.replace("GROUP_", "");
    const seciliKutular = document.querySelectorAll('.eklenecek-kisi-cb:checked');

    if (seciliKutular.length === 0) {
        Swal.fire({ title: 'Uyarı', text: 'Lütfen en az bir kişi seçin!', icon: 'warning' });
        return;
    }

    let basariliSayisi = 0;
    let bitenIstekSayisi = 0;

    seciliKutular.forEach(kutu => {
        const eklenecekKullanici = kutu.value;

        fetch('/api/gruplar/' + grupId + '/kullanicilar/' + eklenecekKullanici, {
            method: 'POST'
        })
            .then(cevap => {
                if (cevap.ok) basariliSayisi++;
            })
            .catch(hata => console.error("Ekleme hatası:", hata))
            .finally(() => {
                bitenIstekSayisi++;

                if (bitenIstekSayisi === seciliKutular.length) {
                    kullaniciEkleModalKapat();

                    Swal.fire({
                        title: 'İşlem Tamamlandı',
                        text: `${basariliSayisi} kişi başarıyla eklendi.`,
                        icon: 'success',
                        customClass: { confirmButton: 'btn-ekle' },
                        buttonsStyling: false
                    }).then(() => {
                        kullanicilariGoster();
                    });
                }
            });
    });
}

function yaziyorArayuzunuGuncelle() {
    const indicator = document.getElementById('typing');
    const avatarsBox = document.getElementById('typing-avatars-box');
    const mesajListesi = document.getElementById('message-list');

    if (aktifYazanlar.size === 0) {
        indicator.style.display = 'none';
        return;
    }

    indicator.style.display = 'flex';
    avatarsBox.innerHTML = '';

    const yazanListesi = Array.from(aktifYazanlar).slice(0, 3);

    yazanListesi.forEach(kisi => {
        const basHarf = kisi.charAt(0).toUpperCase();

        const avatar = document.createElement('div');
        avatar.className = 'typing-avatar';
        avatar.textContent = basHarf;
        avatar.title = kisi + " yazıyor...";

        avatarsBox.appendChild(avatar);
    });

    mesajListesi.appendChild(indicator);
    mesajListesi.scrollTop = mesajListesi.scrollHeight;
}

function arayuzDurumGuncelle(isOnline) {
    const statusLabel = document.getElementById('status-label');
    const statusIcon = document.getElementById('status-icon');

    if (isOnline) {
        statusLabel.innerText = 'Çevrimiçi';
        statusLabel.style.color = '#4cd137';
        statusIcon.style.color = '#4cd137';
    } else {
        statusLabel.innerText = 'Çevrimdışı';
        statusLabel.style.color = '#ddd';
        statusIcon.style.color = '#ddd';
    }
}

// sayfadan çıkarken offline sinyali gönderir
window.addEventListener('beforeunload', () => {//before unload sayfa kapanma durumunu
    if (stompClient && stompClient.connected) {
        stompClient.publish({
            destination: '/app/durum',
            body: JSON.stringify({ kullaniciAdi: myGlobalName, durum: 'OFFLINE' })
        });
    }
});

document.addEventListener('contextmenu', function (e) {
    const tiklananMesaj = e.target.closest('.message-box.me');

    if (tiklananMesaj && tiklananMesaj.hasAttribute('data-id')) {
        e.preventDefault();

        seciliMesajId = tiklananMesaj.getAttribute('data-id');
        const menu = document.getElementById('custom-context-menu');
        menu.style.display = 'block';
        const menuGenislik = menu.offsetWidth;
        const menuYukseklik = menu.offsetHeight;
        const ekranGenislik = window.innerWidth;
        const ekranYukseklik = window.innerHeight;

        let baslangicX = e.pageX;
        let baslangicY = e.pageY;

        if (baslangicX + menuGenislik > ekranGenislik) {
            baslangicX = baslangicX - menuGenislik;
        }

        if (baslangicY + menuYukseklik > ekranYukseklik) {
            baslangicY = baslangicY - menuYukseklik;
        }

        menu.style.left = baslangicX + 'px';
        menu.style.top = baslangicY + 'px';

    } else {
        // başka bir yere tıklandığında 'mesajı sil' kısmını kapat
        document.getElementById('custom-context-menu').style.display = 'none';
    }
});
// başka bir yere tıklandığında 'mesajı sil' kısmını kapat
document.addEventListener('click', function (e) {
    const menu = document.getElementById('custom-context-menu');
    if (menu) {
        menu.style.display = 'none';
    }
});

// scroll yapılırsa kapat
const mesajListesiScroll = document.getElementById('message-list');
if (mesajListesiScroll) {
    mesajListesiScroll.addEventListener('scroll', function () {
        document.getElementById('custom-context-menu').style.display = 'none';
    });
}

document.getElementById('delete-message-btn').addEventListener('click', function (e) {
    e.stopPropagation();
    document.getElementById('custom-context-menu').style.display = 'none';
    // javaya sil diyoruz
    if (seciliMesajId) {
        stompClient.publish({
            destination: '/app/chat.delete',
            body: JSON.stringify({
                mesajId: seciliMesajId,
                chatRoomId: myRoomName
            })
        });
    }
});