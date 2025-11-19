package edu.ucsal.fiadopay.service;

import edu.ucsal.fiadopay.domain.Merchant;
import edu.ucsal.fiadopay.repo.MerchantRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AutService {
    private final MerchantRepository merchants;

    public AutService(MerchantRepository merchants) {
        this.merchants = merchants;
    }

    Merchant merchantFromAuth(String auth){
        if (auth == null || !auth.startsWith("Bearer FAKE-")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        var raw = auth.substring("Bearer FAKE-".length());
        long id;
        try {
            id = Long.parseLong(raw);
        } catch (NumberFormatException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        var merchant = merchants.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED));
        if (merchant.getStatus() != Merchant.Status.ACTIVE) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return merchant;
    }
}
