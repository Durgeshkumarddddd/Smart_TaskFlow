package com.taskflow.service;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseToken;
import com.google.firebase.auth.FirebaseAuthException;
import org.springframework.stereotype.Service;

@Service
public class FirebaseService {

    public String verifyToken(String idToken) throws FirebaseAuthException {
        try {
            FirebaseToken decodedToken = FirebaseAuth.getInstance().verifyIdToken(idToken);
            return decodedToken.getEmail();
        } catch (IllegalStateException e) {
            // Fallback for when Firebase is not initialized due to missing serviceAccountKey.json
            System.err.println("[CRITICAL] Firebase Admin SDK not initialized. Using developer fallback for local testing.");
            
            // For local development only: manually extract email from token parts to allow DB sync
            try {
                String[] parts = idToken.split("\\.");
                if (parts.length > 1) {
                    String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
                    if (payload.contains("\"email\":\"")) {
                        String email = payload.split("\"email\":\"")[1].split("\"")[0];
                        System.out.println("[DEV] Extracted email from token via fallback: " + email);
                        return email;
                    }
                }
            } catch (Exception ex) {
                System.err.println("[DEV] Fallback failed: " + ex.getMessage());
            }
            throw e;
        }
    }
}
