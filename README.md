# Course Generator

## Overview  
A Java-based application that automatically generates complete language learning course content across all CEFR levels (A1–C2) using OpenAI's GPT models.

---

## Key Features

### Multi-Level Content Generation
- Supports all CEFR levels: A1, A2, B1, B2, C1, C2
- Adapts content complexity to each proficiency level

### Flexible Course Structure
- Organizes material into modules and topics
- Allows custom hierarchies to match any curriculum

### AI-Powered Content Creation
- Uses OpenAI API to generate:
  - Learning objectives  
  - Pre-reading content  
  - Vocabulary lists with definitions  
  - Grammar explanations with examples  
  - Interactive exercises and activities  
  - Assessments  
- Built on customizable JSON schemas

### Export Formats
- Generates:
  - Individual JSON files (per topic)  
  - A complete JSON file (entire course)  
  - Word documents (for distribution or editing)

---

## How It Works

### Workflow
1. Define Course Structure  
   - Select CEFR levels  
   - Create modules and topics

2. Create Prompt  
   - Describe the desired content sections  
   - Add specific teaching goals or requirements

3. Generate Content  
   - Application calls OpenAI API  
   - Produces structured content  
   - Exports content as JSON and Word documents

---

## Output Files

| File Type             | Path Example                                        | Description                              |
|-----------------------|-----------------------------------------------------|------------------------------------------|
| Individual JSON       | `generated/A2/Emails/SubjectLines.json`             | Topic-specific content                   |
| Complete Course JSON  | `complete_course_data.json`                         | Full course in one file                  |
| Word Documents        | `word_output/Emails_A2.docx`                        | Nicely formatted editable files          |

---

## Example Prompt
Create a comprehensive business writing course with these sections:

Pre-Read Material

Importance of the format

Context and usage

Writing Objectives

Clear communication goals

Audience and outcome focus

Anchor Vocabulary

Key definitions

Formal vs. informal variants

Grammar & Structure Focus

Sentence/paragraph rules

Connectors and transitions

Exercises

Practice activities

Peer review and self-assessment

Tailor each section to the appropriate CEFR level.

## Customization Options

| Component         | How to Customize                          | File                         |
|-------------------|-------------------------------------------|------------------------------|
| CEFR Levels       | Modify `DIFFICULTY_LEVELS`                | `CourseGenerator.java`       |
| JSON Schema       | Edit `generateJsonSchema()` method        | `CourseGenerator.java`       |
| Content Prompts   | Edit `generateTopicContent()` method      | `CourseGenerator.java`       |
| Word Export Style | Update `exportToWord()` method            | `CourseGenerator.java`       |


## License
MIT License. See the `LICENSE` file for full terms.


## Acknowledgements
- [OpenAI](https://openai.com/) – GPT APIs  
- [Apache POI](https://poi.apache.org/) – Word generation  
- [Jackson](https://github.com/FasterXML/jackson) – JSON processing 

