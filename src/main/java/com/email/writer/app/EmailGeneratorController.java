package com.email.writer.app;

import lombok.AllArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/emails")
@CrossOrigin(origins = "*") // Consider restricting origins in production
@AllArgsConstructor
public class EmailGeneratorController {

    private static final Logger logger = LoggerFactory.getLogger(EmailGeneratorController.class);
    private final EmailGeneratorService emailGeneratorService;

    @PostMapping("/generate")
    public ResponseEntity<String> generateEmail(@RequestBody EmailRequest emailRequest) {
        logger.info("Received request to generate email.");

        try {
            String response = emailGeneratorService.generateEmailReply(emailRequest);

            if (response.startsWith("Error:")) {
                logger.error("Failed to generate email: {}", response);
                return ResponseEntity.internalServerError().body(response);
            }

            logger.info("Email generation successful.");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            logger.error("Exception in generateEmail method: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body("Internal Server Error: " + e.getMessage());
        }
    }
}
