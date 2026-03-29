package com.msgcustom;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class WordFilter {
    private final Set<String> filteredWords = new HashSet<>();
    private final List<String> filteredPhrases = new ArrayList<>();
    
    public void loadFilterFile(File file) {
        if (!file.exists()) {
            return;
        }
        
        filteredWords.clear();
        filteredPhrases.clear();
        
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                
                String normalized = normalizeAndSanitize(line);
                if (normalized.isEmpty()) {
                    continue;
                }
                
                if (normalized.contains(" ")) {
                    filteredPhrases.add(" " + normalized + " ");
                } else {
                    filteredWords.add(normalized);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public boolean containsFilteredWord(String message) {
        String normalized = Normalizer.normalize(message, Normalizer.Form.NFD);
        
        String lowerCaseMsg = normalized.toLowerCase();
        
        StringBuilder sanitized = new StringBuilder(lowerCaseMsg.length());
        boolean lastWasSpace = true;
        
        for (int i = 0; i < lowerCaseMsg.length(); i++) {
            char c = lowerCaseMsg.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) {
                sanitized.append(c);
                lastWasSpace = false;
            } else if (c == ' ' && !lastWasSpace) {
                sanitized.append(' ');
                lastWasSpace = true;
            }
        }
        
        String sanitizedMsg = sanitized.toString().trim();
        if (sanitizedMsg.isEmpty()) return false;

        String paddedMsg = " " + sanitizedMsg + " ";
        for (String phrase : filteredPhrases) {
            if (paddedMsg.contains(phrase)) {
                return true;
            }
        }

        String[] words = sanitizedMsg.split(" ");
        for (String word : words) {
            if (filteredWords.contains(word)) {
                return true;
            }
        }

        return false;
    }
    
    private String normalizeAndSanitize(String input) {
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD);
        String lowerCase = normalized.toLowerCase();
        
        StringBuilder sanitized = new StringBuilder(lowerCase.length());
        boolean lastWasSpace = true;
        
        for (int i = 0; i < lowerCase.length(); i++) {
            char c = lowerCase.charAt(i);
            if ((c >= 'a' && c <= 'z') || (c >= '0' && c <= '9')) {
                sanitized.append(c);
                lastWasSpace = false;
            } else if (c == ' ' && !lastWasSpace) {
                sanitized.append(' ');
                lastWasSpace = true;
            }
        }
        
        return sanitized.toString().trim();
    }
    
    public int getFilteredWordsCount() {
        return filteredWords.size();
    }
    
    public int getFilteredPhrasesCount() {
        return filteredPhrases.size();
    }
}