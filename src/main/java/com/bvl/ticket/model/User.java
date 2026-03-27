package com.bvl.ticket.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Repräsentiert einen Benutzer des Systems.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
/**
 * @author Omar
 */
public class User {
    private String id = UUID.randomUUID().toString();
    private String email;
    private String name;
    private String password; // Wird als Hash gespeichert
    private UserRole role;
    private LocalDateTime createdAt = LocalDateTime.now();
}



