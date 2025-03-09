package com.example.demo.service;

import com.example.demo.model.Contact;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.web.util.UriComponentsBuilder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpEntity;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

@Service
public class GoogleContactsService {

    private static final Logger logger = LoggerFactory.getLogger(GoogleContactsService.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();  // Jackson ObjectMapper for JSON parsing

    @Autowired
    public GoogleContactsService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public List<Contact> getContacts(OAuth2AuthorizedClient client) {
        String apiUrl = "https://people.googleapis.com/v1/people/me/connections";
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(apiUrl)
                .queryParam("personFields", "names,emailAddresses,phoneNumbers")  
                .queryParam("access_token", client.getAccessToken().getTokenValue());

        String response = restTemplate.getForObject(uriBuilder.toUriString(), String.class);
        logger.info("Google API Response: {}", response); // Log the response for debugging
        return parseContacts(response);
    }

    private List<Contact> parseContacts(String jsonResponse) {
        List<Contact> contacts = new ArrayList<>();
        try {
            JsonNode rootNode = objectMapper.readTree(jsonResponse);
            JsonNode connectionsNode = rootNode.get("connections");

            if (connectionsNode != null && connectionsNode.isArray()) {
                for (JsonNode contactNode : connectionsNode) {
                    String id = contactNode.path("resourceName").asText();
                    String name = contactNode.path("names").path(0).path("displayName").asText(null);
                    String email = contactNode.path("emailAddresses").path(0).path("value").asText(null);
                    String number = contactNode.path("phoneNumbers").path(0).path("value").asText(null);

                    contacts.add(new Contact(id, name != null ? name : "", email != null ? email : "", number != null ? number : ""));
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Error parsing contacts response", e);
        }
        return contacts;
    }

    public String addContact(OAuth2AuthorizedClient client, Contact contact) {
        // Define the API URL to add a new contact
        String apiUrl = "https://people.googleapis.com/v1/people:createContact";
    
        // Construct the request body using the contact object's fields
        String requestBody = "{ \"names\": [{ \"givenName\": \"" + contact.getName() + "\" }], "
                + "\"emailAddresses\": [{ \"value\": \"" + contact.getEmail() + "\" }], "
                + "\"phoneNumbers\": [{ \"value\": \"" + contact.getNumber() + "\" }] }";
    
        // Set up the headers, including the Bearer token for authorization
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(client.getAccessToken().getTokenValue());
        headers.setContentType(MediaType.APPLICATION_JSON);
    
        // Send the POST request to Google Contacts API
        HttpEntity<String> entity = new HttpEntity<>(requestBody, headers);
        return restTemplate.postForObject(apiUrl, entity, String.class);  // Return the response from the API (if needed for debugging)
    }    

    public String editContact(OAuth2AuthorizedClient client, String contactId, String newName, String newNumber) {
        try {
            // 1️⃣ Get the contact details (etag and resourceName) first
            String getContactUrl = "https://people.googleapis.com/v1/" + contactId + "?personFields=names,phoneNumbers";
            
            HttpHeaders getHeaders = new HttpHeaders();
            getHeaders.setBearerAuth(client.getAccessToken().getTokenValue());
            HttpEntity<String> getEntity = new HttpEntity<>(getHeaders);
            ResponseEntity<String> response = restTemplate.exchange(getContactUrl, HttpMethod.GET, getEntity, String.class);
            
            logger.info("GET Response: {}", response.getBody()); // Log the GET response
            
            // 2️⃣ Parse response to get etag and resourceName
            JsonNode contactNode = objectMapper.readTree(response.getBody());
            String etag = contactNode.path("etag").asText();
            String resourceName = contactNode.path("resourceName").asText();
    
            logger.info("Parsed etag: {}", etag); // Log the parsed etag
            logger.info("Parsed resourceName: {}", resourceName); // Log the parsed resourceName
    
            if (etag.isEmpty() || resourceName.isEmpty()) {
                return "❌ etag or resourceName is missing. Cannot update.";
            }
    
            // 3️⃣ Build the PATCH request URL, including personFields
            String updateContactUrl = "https://people.googleapis.com/v1/" + resourceName + ":updateContact?updatePersonFields=names,phoneNumbers";
    
            // 4️⃣ Format the request body properly for PATCH
            String requestBody = "{"
                    + "\"etag\": \"" + etag + "\","
                    + "\"names\": [{ \"givenName\": \"" + newName + "\" }],"
                    + "\"phoneNumbers\": [{ \"value\": \"" + newNumber + "\" }]"
                    + "}";
    
            logger.info("Request Body: {}", requestBody); // Log the request body
    
            // 5️⃣ Send PATCH request with etag
            HttpHeaders updateHeaders = new HttpHeaders();
            updateHeaders.setBearerAuth(client.getAccessToken().getTokenValue());
            updateHeaders.setContentType(MediaType.APPLICATION_JSON);
    
            HttpEntity<String> updateEntity = new HttpEntity<>(requestBody, updateHeaders);
    
            logger.info("Sending PATCH request to Google API: {}", updateContactUrl);
            
            ResponseEntity<String> updateResponse = restTemplate.exchange(updateContactUrl, HttpMethod.PATCH, updateEntity, String.class);
    
            // 6️⃣ Log and return response
            logger.info("Update Response: {}", updateResponse.getBody());
            return "✅ Contact updated successfully: " + updateResponse.getBody();
    
        } catch (Exception e) {
            logger.error("Error updating contact", e);
            return "❌ Error updating contact: " + e.getMessage();
        }
    }   

    public void deleteContact(OAuth2AuthorizedClient client, String contactId) {
        String apiUrl = "https://people.googleapis.com/v1/" + contactId + ":deleteContact";  

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(client.getAccessToken().getTokenValue());

        HttpEntity<String> entity = new HttpEntity<>(headers);
        restTemplate.exchange(apiUrl, HttpMethod.DELETE, entity, Void.class);
    }
}