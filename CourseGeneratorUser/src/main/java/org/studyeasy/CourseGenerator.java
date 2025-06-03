package org.studyeasy;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.JsonNode;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatMessage;
import com.theokanning.openai.service.OpenAiService;

import org.apache.poi.xwpf.usermodel.*;
import org.apache.poi.xwpf.usermodel.ParagraphAlignment;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.awt.Desktop;
import java.time.Duration;
import okhttp3.OkHttpClient;
import retrofit2.Retrofit;

public class CourseGenerator {
    private static final String OPENAI_API_KEY = "ENTER YOUR API KEY";
    private static final List<String> DIFFICULTY_LEVELS = Arrays.asList("A1", "A2", "B1", "B2", "C1", "C2");
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

    public static void main(String[] args) {
        try {
            System.out.println("Welcome to Course Generator!");
            System.out.println("This tool will help you generate content for language learning courses across difficulty levels.");
            
            // Create the output directory for Word documents if it doesn't exist
            File outputDir = new File("word_output");
            if (!outputDir.exists()) {
                outputDir.mkdirs();
            }
            
            // STEP 1: Collect course structure first
            System.out.println("\nLet's start by defining your course structure.");
            Map<String, Map<String, List<String>>> courseStructure = collectCourseStructure();
            
            // STEP 2: Collect general prompt
            System.out.println("\nNow, enter your general prompt for content generation:");
            System.out.println("(Describe the type of language learning content you want to generate)");
            System.out.print("> ");
            String generalPrompt = reader.readLine().trim();
            
            // STEP 3: Generate schema based on the prompt
            System.out.println("\nGenerating JSON schema based on your prompt...");
            String schema = generateJsonSchema(generalPrompt);
            
            // STEP 4: Generate content for all topics
            System.out.println("\nGenerating content for all topics...");
            ObjectNode allCoursesData = generateContent(courseStructure, generalPrompt, schema);
            
            // STEP 5: Save complete data
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(new File("complete_course_data.json"), allCoursesData);
            System.out.println("\nComplete course data saved to complete_course_data.json");
            
            // STEP 6: Export to Word documents
            System.out.println("\nExporting content to Word documents...");
            exportToWord(courseStructure, "word_output");
            System.out.println("\nWord documents exported to the 'word_output' directory");
            
            System.out.println("\nProcess completed successfully!");
            
        } catch (IOException e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static Map<String, Map<String, List<String>>> collectCourseStructure() throws IOException {
        Map<String, Map<String, List<String>>> courseStructure = new LinkedHashMap<>();

        System.out.println("\nPlease enter module and topic names for each difficulty level.");
        System.out.println("Example: For modules, enter: 'Business Communication, Email Writing'");
        System.out.println("Example: For topics, enter: 'Formal Emails, Meeting Minutes'");

        // Collect all module and topic inputs
        for (String level : DIFFICULTY_LEVELS) {
            System.out.println("\nFor difficulty level " + level + ":");

            // Get modules
            System.out.println("Enter modules for level " + level + " (comma-separated):");
            System.out.print("> ");
            String moduleInput = reader.readLine().trim();

            // Skip this level if no modules provided
            if (moduleInput.isEmpty()) {
                System.out.println("No modules provided for level " + level + ". Skipping this level.");
                continue;
            }

            // Get topics for each module
            Map<String, List<String>> moduleTopics = new LinkedHashMap<>();
            List<String> modules = parseCommaSeparatedList(moduleInput);

            for (String module : modules) {
                System.out.println("Enter topics for module '" + module + "' (comma-separated):");
                System.out.print("> ");
                String topicInput = reader.readLine().trim();

                // Use default topic if none provided
                if (topicInput.isEmpty()) {
                    System.out.println("No topics provided. Using 'Default Topic' for module '" + module + "'");
                    topicInput = "Default Topic";
                }

                List<String> topics = parseCommaSeparatedList(topicInput);
                moduleTopics.put(module, topics);
            }

            courseStructure.put(level, moduleTopics);
        }

        // Show summary of collected structure
        System.out.println("\nCourse Structure Summary:");
        for (String level : courseStructure.keySet()) {
            System.out.println("Level " + level + ":");
            Map<String, List<String>> modules = courseStructure.get(level);
            for (String module : modules.keySet()) {
                System.out.println("  Module: " + module);
                List<String> topics = modules.get(module);
                for (String topic : topics) {
                    System.out.println("    Topic: " + topic);
                }
            }
        }

        return courseStructure;
    }

    private static List<String> parseCommaSeparatedList(String input) {
        List<String> items = new ArrayList<>();
        if (input == null || input.trim().isEmpty()) {
            return items;
        }

        String[] parts = input.split(",");
        for (String part : parts) {
            String trimmed = part.trim();
            if (!trimmed.isEmpty()) {
                items.add(trimmed);
            }
        }

        return items;
    }

    private static String generateJsonSchema(String generalPrompt) throws IOException {
        System.out.println("\nGenerating JSON schema based on your prompt...");
        OpenAiService service = createOpenAiService();
        
        String schemaPrompt = "You are tasked with creating a comprehensive JSON schema for a language learning course. " +
                "Your schema MUST EXACTLY match the structure specified in the prompt below. " +
                "IMPORTANT: Extract ALL headings, subheadings, sections, and structural elements from the prompt. " +
                "Each heading (like 'Pre-Read Material', 'Writing Objectives', 'Anchor Vocabulary', etc.) and subheading " +
                "in the prompt MUST become a property or nested object in your schema. " +
                "If a section contains tables or lists, structure them appropriately as arrays of objects. " +
                "Do not add generic properties that aren't mentioned in the prompt. " +
                "Do not omit any sections mentioned in the prompt. " +
                "\n\nHere is the prompt: \n\n" + generalPrompt;

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage("system",
                "You are an expert JSON schema designer with perfect attention to detail. " +
                "Your task is to create JSON schemas that EXACTLY match the structure specified in prompts. " +
                "You carefully extract ALL headings, subheadings, and structural elements from prompts and " +
                "convert them into corresponding JSON schema properties. " +
                "You are especially skilled at identifying structured content like tables and lists and " +
                "representing them appropriately in the schema. " +
                "You never add generic properties that aren't mentioned in the prompt. " +
                "You never omit any sections mentioned in the prompt. " +
                "You return only valid JSON schema without any explanations."));
        messages.add(new ChatMessage("user", schemaPrompt));

        ChatCompletionRequest completionRequest = ChatCompletionRequest.builder()
                .messages(messages)
                .model("gpt-4o") // Using GPT-4o for better schema generation
                .temperature(0.1) // Very low temperature for more deterministic output
                .maxTokens(3000) // Reduced token limit to improve response time
                .build();

        // Implement retry logic
        String response = null;
        int maxRetries = 3;
        int retryCount = 0;
        boolean success = false;
        
        while (!success && retryCount < maxRetries) {
            try {
                System.out.println("Attempting to generate schema (attempt " + (retryCount + 1) + " of " + maxRetries + ")...");
                response = service.createChatCompletion(completionRequest).getChoices().get(0).getMessage().getContent();
                success = true;
            } catch (Exception e) {
                retryCount++;
                if (retryCount >= maxRetries) {
                    System.err.println("Failed after " + maxRetries + " attempts: " + e.getMessage());
                    throw new IOException("Failed to generate schema after multiple attempts", e);
                }
                System.out.println("API call failed, retrying in " + (retryCount * 2) + " seconds...");
                try {
                    Thread.sleep(retryCount * 2000); // Exponential backoff
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        // Extract JSON from response if needed
        if (response != null) {
            if (response.contains("```json")) {
                response = response.substring(response.indexOf("```json") + 7, response.lastIndexOf("```")).trim();
            } else if (response.contains("```")) {
                response = response.substring(response.indexOf("```") + 3, response.lastIndexOf("```")).trim();
            }
        } else {
            throw new IOException("Failed to get a valid response from the API");
        }

        try {
            // Validate the schema
            ObjectNode schemaNode = (ObjectNode) objectMapper.readTree(response);
            
            // Ensure the schema has the required structure
            ensureSchemaStructure(schemaNode);
            
            // Save the schema
            File schemaFile = new File("course_schema.json");
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(schemaFile, schemaNode);
            
            System.out.println("✓ Custom JSON schema generated and saved to course_schema.json");
            
            return response;
        } catch (Exception e) {
            System.err.println("Error processing schema: " + e.getMessage());
            System.err.println("Raw response: " + response);
            throw new IOException("Failed to process schema", e);
        }
    }

    private static void ensureSchemaStructure(ObjectNode schemaNode) {
        // Add basic schema structure if missing
        if (!schemaNode.has("$schema")) {
            schemaNode.put("$schema", "http://json-schema.org/draft-07/schema#");
        }
        
        if (!schemaNode.has("title")) {
            schemaNode.put("title", "Business Writing Lesson Schema");
        }
        
        if (!schemaNode.has("type")) {
            schemaNode.put("type", "object");
        }
        
        if (!schemaNode.has("properties")) {
            schemaNode.set("properties", objectMapper.createObjectNode());
        }
        
        // Ensure basic metadata fields are defined
        ObjectNode properties = (ObjectNode) schemaNode.get("properties");
        
        if (!properties.has("lessonTitle")) {
            ObjectNode lessonTitle = objectMapper.createObjectNode();
            lessonTitle.put("type", "string");
            lessonTitle.put("description", "The title of the lesson");
            properties.set("lessonTitle", lessonTitle);
        }
        
        if (!properties.has("difficultyLevel")) {
            ObjectNode difficultyLevel = objectMapper.createObjectNode();
            difficultyLevel.put("type", "string");
            difficultyLevel.put("description", "The CEFR difficulty level of the lesson");
            properties.set("difficultyLevel", difficultyLevel);
        }
        
        if (!properties.has("moduleTitle")) {
            ObjectNode moduleTitle = objectMapper.createObjectNode();
            moduleTitle.put("type", "string");
            moduleTitle.put("description", "The title of the module");
            properties.set("moduleTitle", moduleTitle);
        }
        
        // Ensure required fields are defined
        if (!schemaNode.has("required")) {
            ArrayNode required = objectMapper.createArrayNode();
            required.add("lessonTitle");
            required.add("difficultyLevel");
            required.add("moduleTitle");
            schemaNode.set("required", required);
        }
    }

    private static ObjectNode generateContent(
            Map<String, Map<String, List<String>>> courseStructure,
            String generalPrompt,
            String schema) throws IOException {

        ObjectNode allCoursesData = objectMapper.createObjectNode();

        for (String level : courseStructure.keySet()) {
            System.out.println("\nProcessing difficulty level: " + level);
            ObjectNode levelNode = objectMapper.createObjectNode();

            Map<String, List<String>> modules = courseStructure.get(level);
            for (String module : modules.keySet()) {
                System.out.println("  Processing module: " + module);
                ArrayNode moduleTopics = objectMapper.createArrayNode();

                List<String> topics = modules.get(module);
                for (String topic : topics) {
                    System.out.println("    Processing topic: " + topic);

                    // Create topic prompt with emphasis on educational content
                    String topicPrompt = generalPrompt + 
                            "\n\nCreate COMPREHENSIVE educational content for " + level + " level learners." +
                            "\nEnsure the content is immediately usable by students and contains detailed explanations, examples, and exercises.";

                    // Generate content
                    try {
                        ObjectNode topicContent = generateTopicContent(level, module, topic, topicPrompt);

                        // Ensure required fields are present
                        ensureRequiredFields(topicContent, level, module, topic);
                        
                        // NEW: Validate content quality
                        ensureContentQuality(topicContent);

                        moduleTopics.add(topicContent);

                        // Save intermediate result
                        saveIntermediateResult(level, module, topic, topicContent);

                        System.out.println("    ✓ Generated comprehensive content for: " + topic);
                    } catch (Exception e) {
                        System.err.println("    ✗ Error generating content for " + topic + ": " + e.getMessage());
                        e.printStackTrace();
                    }
                }

                levelNode.set(module, moduleTopics);
            }

            allCoursesData.set(level, levelNode);
        }

        return allCoursesData;
    }

    private static void ensureRequiredFields(ObjectNode content, String level, String module, String topic) {
        // Ensure basic fields are present
        if (!content.has("lessonTitle") || content.get("lessonTitle").isNull() || content.get("lessonTitle").asText().isEmpty()) {
            content.put("lessonTitle", topic);
        }

        if (!content.has("difficultyLevel") || content.get("difficultyLevel").isNull() || content.get("difficultyLevel").asText().isEmpty()) {
            content.put("difficultyLevel", level);
        }

        if (!content.has("moduleTitle") || content.get("moduleTitle").isNull() || content.get("moduleTitle").asText().isEmpty()) {
            content.put("moduleTitle", module);
        }
        
        // Check for schema-specific fields based on your sample
        if (!content.has("CEFR_level") || content.get("CEFR_level").isNull() || content.get("CEFR_level").asText().isEmpty()) {
            content.put("CEFR_level", level);
        }
        
        if (!content.has("lesson_title") || content.get("lesson_title").isNull() || content.get("lesson_title").asText().isEmpty()) {
            content.put("lesson_title", topic);
        }
        
        // Initialize empty arrays for required array fields if they don't exist
        if (!content.has("learning_objectives") || content.get("learning_objectives").isNull()) {
            content.set("learning_objectives", objectMapper.createArrayNode());
        }
        
        if (!content.has("format_options") || content.get("format_options").isNull()) {
            content.set("format_options", objectMapper.createArrayNode());
        }
    }

    private static ObjectNode generateTopicContent(String level, String module, String topic, String prompt) throws IOException {
        System.out.println("    Generating content with AI...");
        OpenAiService service = createOpenAiService();

        // Load the schema to include in the prompt
        String schema;
        try {
            schema = objectMapper.readTree(new File("course_schema.json")).toString();
        } catch (Exception e) {
            System.err.println("    Error loading schema: " + e.getMessage());
            schema = "{}"; // Fallback to empty schema
        }

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(new ChatMessage("system",
                "You are an expert content creator for language learning courses. " +
                "You must generate content that EXACTLY follows the provided JSON schema structure. " +
                "Every property in the schema must be included in your response with appropriate values. " +
                "Pay special attention to sections like 'Pre-Read Material', 'Writing Objectives', 'Anchor Vocabulary', " +
                "'Grammar & Structure Focus', etc., and ensure you populate them with comprehensive educational content. " +
                "For table-structured data (like vocabulary lists), format them as arrays of objects with all required properties. " +
                "Do not add properties that aren't in the schema. " +
                "Do not omit any properties from the schema. " +
                "Return only valid JSON that matches the schema structure."));
        
        // Enhanced prompt with stronger instructions
        String enhancedPrompt = "Create detailed educational content for a language learning course with the following details:\n\n" + 
                prompt + // This is the original user prompt with all their requirements
                "\n\nDifficulty Level: " + level +
                "\nModule: " + module +
                "\nTopic: " + topic +
                "\n\nYou MUST follow this exact JSON schema structure: " + schema + 
                "\n\nIMPORTANT REQUIREMENTS:" +
                "\n1. Include ALL properties from the schema" +
                "\n2. Populate ALL sections with comprehensive educational content" +
                "\n3. For sections like 'Pre-Read Material', include all subsections (whyFormat, contextualIntroduction, etc.)" +
                "\n4. For 'Anchor Vocabulary', create a complete list with terms, definitions, and usage tips" +
                "\n5. For 'Grammar & Structure Focus', include rules, examples, and usage guidance" +
                "\n6. Include practical exercises with clear instructions and examples" +
                "\n7. Ensure your response is valid JSON" +
                "\n8. Include the metadata fields 'lessonTitle', 'difficultyLevel', and 'moduleTitle'";
        
        messages.add(new ChatMessage("user", enhancedPrompt));

        ChatCompletionRequest completionRequest = ChatCompletionRequest.builder()
                .messages(messages)
                .model("gpt-4o")
                .temperature(0.4) // Slightly higher for more creative content
                .maxTokens(4000) // Maximum tokens for comprehensive content
                .build();

        // Implement retry logic
        String response = null;
        int maxRetries = 3;
        int retryCount = 0;
        boolean success = false;
        
        while (!success && retryCount < maxRetries) {
            try {
                System.out.println("    Attempting to generate content (attempt " + (retryCount + 1) + " of " + maxRetries + ")...");
                response = service.createChatCompletion(completionRequest).getChoices().get(0).getMessage().getContent();
                success = true;
            } catch (Exception e) {
                retryCount++;
                if (retryCount >= maxRetries) {
                    System.err.println("    Failed after " + maxRetries + " attempts: " + e.getMessage());
                    throw new IOException("Failed to generate content after " + maxRetries + " attempts", e);
                }
                System.out.println("    API call failed, retrying in " + (retryCount * 2) + " seconds...");
                try {
                    Thread.sleep(retryCount * 2000); // Exponential backoff
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }

        // Extract JSON from response if needed
        if (response.contains("```json")) {
            response = response.substring(response.indexOf("```json") + 7, response.lastIndexOf("```")).trim();
        } else if (response.contains("```")) {
            response = response.substring(response.indexOf("```") + 3, response.lastIndexOf("```")).trim();
        }

        try {
            ObjectNode contentNode = (ObjectNode) objectMapper.readTree(response);
            
            // Ensure required metadata fields are present
            ensureRequiredFields(contentNode, level, module, topic);
            
            return contentNode;
        } catch (Exception e) {
            System.err.println("    Error parsing AI response as JSON: " + e.getMessage());
            System.err.println("    Raw response: " + response);
            throw new IOException("Failed to parse AI response as JSON");
        }
    }

    private static String callOpenAIWithRetry(OpenAiService service, ChatCompletionRequest request, int maxRetries) {
        int retries = 0;
        while (retries < maxRetries) {
            try {
                return service.createChatCompletion(request).getChoices().get(0).getMessage().getContent();
            } catch (Exception e) {
                retries++;
                if (retries >= maxRetries) {
                    throw new RuntimeException("Failed after " + maxRetries + " retries: " + e.getMessage(), e);
                }
                System.out.println("API call failed, retrying (" + retries + "/" + maxRetries + ")...");
                try {
                    Thread.sleep(2000 * retries); // Exponential backoff
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        throw new RuntimeException("Failed after " + maxRetries + " retries");
    }

    private static void saveIntermediateResult(String level, String module, String topic, ObjectNode content) {
        try {
            // Create directories if they don't exist
            File dir = new File("generated/" + level + "/" + sanitizeFileName(module));
            dir.mkdirs();

            // Save the content
            String filename = sanitizeFileName(topic) + ".json";
            File outputFile = new File(dir, filename);
            
            // Write with pretty printer
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(outputFile, content);

            System.out.println("    ✓ Saved to generated/" + level + "/" + sanitizeFileName(module) + "/" + filename);
        } catch (IOException e) {
            System.err.println("    Error saving intermediate result: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static String sanitizeFileName(String input) {
        return input.replaceAll("[^a-zA-Z0-9]", "_");
    }

    private static void exportToWord(Map<String, Map<String, List<String>>> courseStructure, 
                                    String outputPath) throws IOException {
        System.out.println("\nExporting content to Word documents...");
        
        // Create output directory if it doesn't exist
        File outputDir = new File(outputPath);
        if (!outputDir.exists()) {
            outputDir.mkdirs();
        }
        
        for (String level : courseStructure.keySet()) {
            System.out.println("Processing level: " + level);
            Map<String, List<String>> modules = courseStructure.get(level);
            
            for (String module : modules.keySet()) {
                System.out.println("  Processing module: " + module);
                try {
                    // Create a new document for each module
                    XWPFDocument document = new XWPFDocument();
                    
                    // Add title
                    XWPFParagraph titlePara = document.createParagraph();
                    titlePara.setAlignment(ParagraphAlignment.CENTER);
                    
                    XWPFRun titleRun = titlePara.createRun();
                    titleRun.setText(module + " - Level: " + level);
                    titleRun.setBold(true);
                    titleRun.setFontSize(16);
                    titleRun.addBreak();
                    
                    // Add date
                    XWPFRun dateRun = titlePara.createRun();
                    dateRun.setText("Generated on: " + new java.util.Date().toString());
                    dateRun.setFontSize(10);
                    dateRun.addBreak();
                    dateRun.addBreak();
                    
                    List<String> topics = modules.get(module);
                    for (String topic : topics) {
                        System.out.println("    Processing topic: " + topic);
                        // Try to load the generated content
                        try {
                            String jsonPath = "generated/" + level + "/" + 
                                    sanitizeFileName(module) + "/" + 
                                    sanitizeFileName(topic) + ".json";
                            File jsonFile = new File(jsonPath);
                            
                            if (jsonFile.exists()) {
                                System.out.println("    Found JSON file: " + jsonPath);
                                ObjectNode content = (ObjectNode) objectMapper.readTree(jsonFile);
                                
                                // Add topic heading
                                XWPFParagraph topicPara = document.createParagraph();
                                XWPFRun topicRun = topicPara.createRun();
                                topicRun.setText(topic);
                                topicRun.setBold(true);
                                topicRun.setFontSize(14);
                                topicRun.addBreak();
                                
                                // Process content fields
                                addJsonContentToDocument(document, content);
                                
                                System.out.println("    ✓ Added topic content to document");
                            } else {
                                System.out.println("    ⚠ No content file found at: " + jsonPath);
                            }
                        } catch (Exception e) {
                            System.err.println("    Error adding topic " + topic + " to Word document: " + e.getMessage());
                        }
                    }
                    
                    // Save the document
                    String filename = sanitizeFileName(module) + "_" + level + ".docx";
                    String filePath = outputPath + "/" + filename;
                    try (FileOutputStream out = new FileOutputStream(filePath)) {
                        document.write(out);
                        System.out.println("  ✓ Saved module to " + filePath);
                    }
                } catch (Exception e) {
                    System.err.println("  Error creating Word document for module " + module + ": " + e.getMessage());
                    e.printStackTrace();
                }
            }
        }
        
        System.out.println("Word document export complete.");
    }

    private static void addJsonContentToDocument(XWPFDocument document, ObjectNode content) {
        // Add each field from the JSON to the document
        Iterator<Map.Entry<String, JsonNode>> fields = content.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String fieldName = field.getKey();
            JsonNode value = field.getValue();
            
            // Skip schema and other metadata fields
            if (fieldName.equals("$schema") || fieldName.equals("type") || 
                fieldName.equals("properties") || fieldName.equals("required")) {
                continue;
            }
            
            // Add field name as a subheading
            XWPFParagraph fieldPara = document.createParagraph();
            XWPFRun fieldRun = fieldPara.createRun();
            fieldRun.setText(formatFieldName(fieldName) + ":");
            fieldRun.setBold(true);
            fieldRun.setFontSize(12);
            
            // Add field value
            if (value.isTextual()) {
                XWPFParagraph valuePara = document.createParagraph();
                XWPFRun valueRun = valuePara.createRun();
                valueRun.setText(value.asText());
                valueRun.addBreak();
            } else if (value.isArray()) {
                for (JsonNode item : value) {
                    XWPFParagraph valuePara = document.createParagraph();
                    valuePara.setIndentationLeft(720); // 0.5 inch indent
                    XWPFRun valueRun = valuePara.createRun();
                    valueRun.setText("• " + (item.isTextual() ? item.asText() : item.toString()));
                    valueRun.addBreak();
                }
            } else if (value.isObject()) {
                // Recursively add nested objects
                Iterator<Map.Entry<String, JsonNode>> nestedFields = value.fields();
                while (nestedFields.hasNext()) {
                    Map.Entry<String, JsonNode> nestedField = nestedFields.next();
                    String nestedFieldName = nestedField.getKey();
                    JsonNode nestedValue = nestedField.getValue();
                    
                    XWPFParagraph nestedPara = document.createParagraph();
                    nestedPara.setIndentationLeft(720); // 0.5 inch indent
                    XWPFRun nestedRun = nestedPara.createRun();
                    nestedRun.setText(formatFieldName(nestedFieldName) + ":");
                    nestedRun.setBold(true);
                    
                    if (nestedValue.isTextual()) {
                        XWPFRun valueRun = nestedPara.createRun();
                        valueRun.setBold(false);
                        valueRun.setText(" " + nestedValue.asText());
                        valueRun.addBreak();
                    } else if (nestedValue.isArray() || nestedValue.isObject()) {
                        nestedRun.addBreak();
                        // Create a temporary ObjectNode to hold this nested field
                        ObjectNode tempNode = objectMapper.createObjectNode();
                        tempNode.set(nestedFieldName, nestedValue);
                        // Process it with indentation
                        addJsonContentToDocument(document, tempNode);
                    }
                }
            }
        }
    }

    private static void createSimpleTable(XWPFDocument document, JsonNode arrayNode) {
        if (arrayNode.size() == 0) {
            return;
        }
        
        // Get field names from the first object
        JsonNode firstItem = arrayNode.get(0);
        List<String> fieldNames = new ArrayList<>();
        Iterator<String> fieldNamesIter = firstItem.fieldNames();
        while (fieldNamesIter.hasNext()) {
            fieldNames.add(fieldNamesIter.next());
        }
        
        // Create table
        XWPFTable table = document.createTable(arrayNode.size() + 1, fieldNames.size());
        
        // Add header row
        XWPFTableRow headerRow = table.getRow(0);
        for (int i = 0; i < fieldNames.size(); i++) {
            XWPFTableCell cell = headerRow.getCell(i);
            
            XWPFParagraph para = cell.getParagraphs().get(0);
            
            XWPFRun run = para.createRun();
            run.setText(formatFieldName(fieldNames.get(i)));
            run.setBold(true);
        }
        
        // Add data rows
        for (int i = 0; i < arrayNode.size(); i++) {
            JsonNode item = arrayNode.get(i);
            XWPFTableRow row = table.getRow(i + 1);
            
            for (int j = 0; j < fieldNames.size(); j++) {
                String fieldName = fieldNames.get(j);
                JsonNode value = item.get(fieldName);
                
                XWPFTableCell cell = row.getCell(j);
                XWPFParagraph para = cell.getParagraphs().get(0);
                
                XWPFRun run = para.createRun();
                if (value != null) {
                    if (value.isTextual()) {
                        run.setText(value.asText());
                    } else {
                        run.setText(value.toString());
                    }
                } else {
                    run.setText("");
                }
            }
        }
        
        // Add space after table
        document.createParagraph();
    }

    private static String formatFieldName(String fieldName) {
        // Convert camelCase or snake_case to Title Case With Spaces
        String result = fieldName.replaceAll("([a-z])([A-Z])", "$1 $2")
                                .replaceAll("_", " ");
        
        // Capitalize first letter of each word
        String[] words = result.split(" ");
        StringBuilder formatted = new StringBuilder();
        
        for (String word : words) {
            if (word.length() > 0) {
                formatted.append(word.substring(0, 1).toUpperCase())
                        .append(word.substring(1).toLowerCase())
                        .append(" ");
            }
        }
        
        return formatted.toString().trim();
    }

    private static void validateContentAgainstSchema(ObjectNode content, String schemaJson) {
        try {
            JsonNode schema = objectMapper.readTree(schemaJson);
            if (schema.has("properties")) {
                JsonNode properties = schema.get("properties");
                Iterator<String> fieldNames = properties.fieldNames();
                while (fieldNames.hasNext()) {
                    String field = fieldNames.next();
                    if (!content.has(field)) {
                        System.out.println("    Warning: Generated content is missing field '" + field + "' from schema");
                    }
                }
            }
            
            if (schema.has("required") && schema.get("required").isArray()) {
                for (JsonNode requiredField : schema.get("required")) {
                    String field = requiredField.asText();
                    if (!content.has(field)) {
                        System.out.println("    Warning: Generated content is missing required field '" + field + "'");
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("    Error validating content against schema: " + e.getMessage());
        }
    }

    private static OpenAiService createOpenAiService() {
        // Create a service with increased timeout
        return new OpenAiService(OPENAI_API_KEY, Duration.ofSeconds(120)); // 120 second timeout
    }

    private static void ensureContentQuality(ObjectNode content) {
        boolean needsRegeneration = false;
        StringBuilder issues = new StringBuilder();
        
        // Check learning objectives
        if (!content.has("learning_objectives") || 
            !content.get("learning_objectives").isArray() || 
            content.get("learning_objectives").size() < 3) {
            issues.append("- Missing or insufficient learning objectives\n");
            needsRegeneration = true;
        }
        
        // Check for content sections
        if (!content.has("content") && !content.has("contentSections") && !content.has("lessonContent")) {
            issues.append("- No main content sections found\n");
            needsRegeneration = true;
        }
        
        // Check for exercises
        if (!content.has("exercises") && !content.has("activities") && !content.has("practice")) {
            issues.append("- No exercises or activities found\n");
            needsRegeneration = true;
        }
        
        // If serious issues found, alert and provide option to regenerate
        if (needsRegeneration) {
            System.err.println("\n⚠️ CONTENT QUALITY ISSUES DETECTED:");
            System.err.println(issues.toString());
            System.err.println("The generated content may not be useful for learners.");
            
            // Add code here to offer regeneration option
        }
    }
}
