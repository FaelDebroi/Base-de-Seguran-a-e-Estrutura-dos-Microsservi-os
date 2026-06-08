package com.ms.userservice.services;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CodigoCacheService {

    private record Entry(String code, Instant expiry) {}

    private final Map<String, Entry> cache = new ConcurrentHashMap<>();

    public void salvar(String email, String codigo) {
        cache.put(email, new Entry(codigo, Instant.now().plusSeconds(300)));
    }

    public boolean validar(String email, String codigo) {
        Entry entry = cache.get(email);
        if (entry == null || Instant.now().isAfter(entry.expiry())) {
            cache.remove(email);
            return false;
        }
        return entry.code().equals(codigo);
    }

    @Scheduled(fixedRate = 60_000)
    public void limparExpirados() {
        Instant now = Instant.now();
        cache.entrySet().removeIf(e -> now.isAfter(e.getValue().expiry()));
    }
}
