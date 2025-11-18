/**
 * Session REST controller.
 */

package com.opty.socket.controller;


/**
 * IMPORTS
 */
import com.opty.socket.dto.SessionDTO;
import com.opty.socket.service.SessionManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;


/**
 * CODE
 */

/**
 * REST API for session management.
 */
@Slf4j
@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class SessionController {

    // --- ATTRIBUTES ---
    private final SessionManager sessionManager;


    /**
     * Gets all sessions that are waiting for a supervisor (unpaired).
     *
     * @return list of unpaired sessions
     */
    @GetMapping("/available")
    public ResponseEntity<List<SessionDTO>> getAvailableSessions() {

        // Fetch unpaired sessions and convert to DTO
        List<SessionDTO> sessions = sessionManager.getUnpairedSessions()
            .stream()
            .map(SessionDTO::fromSession)
            .collect(Collectors.toList());

        // Log and return the list
        log.info("Fetched {} available sessions", sessions.size());
        return ResponseEntity.ok(sessions);
    }
}
