package com.example.demo.controller;

import com.example.demo.model.Contact;
import com.example.demo.model.EditContactRequest;
import com.example.demo.service.GoogleContactsService;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@Controller
public class ContactController {

    private final GoogleContactsService googleContactsService;

    public ContactController(GoogleContactsService googleContactsService) {
        this.googleContactsService = googleContactsService;
    }

    @GetMapping("/contacts")
    public String getContacts(Model model, @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient client) {
        List<Contact> contacts = googleContactsService.getContacts(client);
        model.addAttribute("contacts", contacts);
        return "contacts";  
    }

    @PatchMapping("/editContact")
    public ResponseEntity<String> editContact(@RequestBody EditContactRequest request, @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient client) {
        try {
            String result = googleContactsService.editContact(client, request.getContactId(), request.getNewName(), request.getNewNumber());
            if (result.startsWith("✅")) {
                return ResponseEntity.ok("{\"success\": true}");
            } else {
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"success\": false, \"message\": \"" + result + "\"}");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"success\": false, \"message\": \"Error updating contact: " + e.getMessage() + "\"}");
        }
    }

    @PostMapping("/deleteContact")
    public String deleteContact(String contactId, @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient client) {
        googleContactsService.deleteContact(client, contactId);
        return "redirect:/contacts";  
    }

    @PostMapping("/addContact")
    public String addContact(@RequestBody Contact newContact, @RegisteredOAuth2AuthorizedClient("google") OAuth2AuthorizedClient client) {
        googleContactsService.addContact(client, newContact);  
        return "redirect:/contacts";  
    }
}
