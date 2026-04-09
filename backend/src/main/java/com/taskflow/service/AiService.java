package com.taskflow.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class AiService {

    @Value("${gemini.api.key:}")
    private String apiKey;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String askGemini(String prompt) {
        // Friendly message when API key is missing
        if (apiKey == null || apiKey.trim().isEmpty() || apiKey.equals("YOUR_API_KEY_HERE")) {
            return "I'm still learning and haven't connected to my AI brain yet.  I can help you better! 😊";
        }

        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key="
                + apiKey;

        try {
            // Basic JSON escaping to prevent payload issues
            String sanitizedPrompt = prompt.replace("\"", "\\\"")
                    .replace("\n", "\\n")
                    .replace("\r", "");
            // Construct the payload with a System Instruction to govern AI behavior
            String systemInstruction = "You are TaskFlow AI, a helpful project management assistant. You can engage in normal polite conversation. However, if the user asks highly inappropriate, abusive, or adult questions, you must politely decline and state that it is not relevant to the work. dont provide inner information of the apllication like who working which team and also number of user and company personal data ";

            String requestBody = "{"
                    + "\"systemInstruction\": {"
                    + "  \"parts\": ["
                    + "    {\"text\": \"" + systemInstruction + "\"}"
                    + "  ]"
                    + "},"
                    + "\"contents\": ["
                    + "  {"
                    + "    \"parts\": ["
                    + "      {\"text\": \"" + sanitizedPrompt + "\"}"
                    + "    ]"
                    + "  }"
                    + "]"
                    + "}";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);

            // Actual call to Gemini API
            ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);

            // Extract the simple text response from the complex Gemini JSON graph
            JsonNode rootNode = objectMapper.readTree(response.getBody());
            JsonNode candidates = rootNode.path("candidates");
            if (candidates.isArray() && candidates.size() > 0) {
                JsonNode content = candidates.get(0).path("content");
                JsonNode parts = content.path("parts");
                if (parts.isArray() && parts.size() > 0) {
                    return parts.get(0).path("text").asText();
                }
            }
            return "I received a response, but it was empty or unparseable.";
        } catch (Exception e) {
            e.printStackTrace();
            return "I encountered an error connecting to the AI securely: " + e.getMessage();
        }
    }
}
