package com.batchprompt.jobs.output.worker;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.batchprompt.files.client.FileClient;
import com.batchprompt.files.model.dto.FileDto;
import com.batchprompt.files.model.dto.FileRecordDto;
import com.batchprompt.jobs.core.model.Job;
import com.batchprompt.jobs.core.model.JobTask;
import com.batchprompt.jobs.core.repository.JobRepository;
import com.batchprompt.jobs.core.repository.JobTaskRepository;
import com.batchprompt.jobs.model.JobStatus;
import com.batchprompt.jobs.model.TaskStatus;
import com.batchprompt.jobs.model.dto.JobOutputMessage;
import com.batchprompt.prompts.client.PromptClient;
import com.batchprompt.prompts.model.PromptOutputMethod;
import com.batchprompt.prompts.model.dto.PromptDto;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class JobOutputWorker {

    private final JobRepository jobRepository;
    private final JobTaskRepository jobTaskRepository;
    private final FileClient fileClient;
    private final PromptClient promptClient;
    private final ObjectMapper objectMapper;
    
    @RabbitListener(queues = "${rabbitmq.queue.job-output.name}")
    @Transactional
    public void processJobOutput(JobOutputMessage message) {
        UUID jobUuid = message.getJobUuid();
        boolean hasErrors = message.isHasErrors();
        
        log.info("Processing job output for job {}", jobUuid);
        File tempFile = null;
        
        try {
            // Get the job
            Job job = jobRepository.findById(jobUuid).orElse(null);
            if (job == null) {
                log.error("Job not found: {}", jobUuid);
                return;
            }

            PromptDto prompt = promptClient.getPrompt(job.getPromptUuid(), null);
            if (prompt == null) {
                log.error("Prompt not found for job: {}", jobUuid);
                failJob(job);
                return;
            }

            FileDto inputFile = fileClient.getFile(job.getFileUuid(), null);
            if (inputFile == null) {
                log.error("File not found for job: {}", jobUuid);
                failJob(job);
                return;
            }
            
            // Update job status to GENERATING_OUTPUT
            job.setStatus(JobStatus.GENERATING_OUTPUT);
            job.setUpdatedAt(LocalDateTime.now());
            jobRepository.save(job);
            
            // Get all tasks for the job
            List<JobTask> tasks = jobTaskRepository.findByJobUuid(jobUuid);
            if (tasks.isEmpty()) {
                log.error("No tasks found for job: {}", jobUuid);
                failJob(job);
                return;
            }
            
            // Generate Excel file
            tempFile = generateExcelFile(job, prompt, tasks);
            if (tempFile == null) {
                log.error("Failed to generate Excel file for job: {}", jobUuid);
                failJob(job);
                return;
            }
            
            // Create the result file name by adding the prompt name and the date/time in YYYYMMDDHHMMSS to the original file name
            String fileName = createOutputFileName(inputFile, prompt); 

            // Use a try-with-resources to ensure the FileInputStream is properly closed
            try (FileInputStream fileInputStream = new FileInputStream(tempFile)) {
                FileDto resultFileDto = fileClient.uploadFile(
                    fileInputStream,
                    fileName,
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    tempFile.length(),
                    "RESULT",
                    null,
                    job.getUserId()
                );
                
                if (resultFileDto == null) {
                    log.error("Failed to upload Excel file for job: {}", jobUuid);
                    failJob(job);
                    return;
                }
                
                // Validate the uploaded file
                boolean validationSuccess = fileClient.validateFile(resultFileDto.getFileUuid(), null);
                if (!validationSuccess) {
                    log.error("File validation failed for job: {}", jobUuid);
                    failJob(job);
                    return;
                }
                
                // Update job status to COMPLETED or COMPLETED_WITH_ERRORS
                JobStatus finalStatus = hasErrors ? JobStatus.COMPLETED_WITH_ERRORS : JobStatus.COMPLETED;
                job.setResultFileUuid(resultFileDto.getFileUuid());
                job.setStatus(finalStatus);
                job.setUpdatedAt(LocalDateTime.now());
                jobRepository.save(job);
                
                log.info("Job output processing completed for job {}. Status: {}", jobUuid, finalStatus);
            }
        } catch (Exception e) {
            log.error("Error processing job output for job: {}", jobUuid, e);
            // Try to update the job status to FAILED
            try {
                Job job = jobRepository.findById(jobUuid).orElse(null);
                if (job != null) {
                    failJob(job);
                }
            } catch (Exception ex) {
                log.error("Failed to update job status to FAILED for job: {}", jobUuid, ex);
            }
        } finally {
            // Clean up the temporary file
            if (tempFile != null && tempFile.exists()) {
                if (!tempFile.delete()) {
                    tempFile.deleteOnExit();
                }
            }
        }
    }

    /**
     * Create a unique output file name based on the original file name, prompt name, and current date/time
     * 
     * @param inputFile The input file
     * @param prompt The prompt
     * @return The generated output file name
     */

    private String createOutputFileName(FileDto inputFile, PromptDto prompt) {
        String originalFileName = inputFile.getFileName();
        String promptName = prompt.getName().replaceAll("[^a-zA-Z0-9]", "_");
        String dateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));

        // Remove the file extension from the original file name
        int lastDotIndex = originalFileName.lastIndexOf('.');
        if (lastDotIndex != -1) {
            originalFileName = originalFileName.substring(0, lastDotIndex);
        }
        return String.format("%s_%s_%s.xlsx", originalFileName, promptName, dateTime);
    }
    
    /**
     * Generate Excel file with job results
     * 
     * @param job The job
     * @param prompt The prompt
     * @param tasks The job tasks
     * @return The temporary file containing the Excel data
     */
    private File generateExcelFile(Job job, PromptDto prompt, List<JobTask> tasks) {
        File tempFile = null;
        try {
            // Create a temporary file
            tempFile = File.createTempFile("job_output_" + job.getJobUuid(), ".xlsx");
            
            try (Workbook workbook = new XSSFWorkbook();
                 FileOutputStream fileOut = new FileOutputStream(tempFile)) {
                
                Sheet sheet = workbook.createSheet("Results");
                
                // Create header row
                Row headerRow = sheet.createRow(0);
                List<String> headers = getHeaders(job, prompt, tasks);
                int colIndex = 0;
                
                for (String header : headers) {
                    Cell cell = headerRow.createCell(colIndex++);
                    cell.setCellValue(header);
                }
                
                // Create data rows
                int rowIndex = 1;
                for (JobTask task : tasks) {
                    try {
                        Row row = sheet.createRow(rowIndex++);
                        populateRow(row, prompt, task, headers);
                    } catch (Exception e) {
                        log.error("Error processing task {} for job {}: {}", 
                                  task.getJobTaskUuid(), job.getJobUuid(), e.getMessage());
                        // Continue with the next task even if this one fails
                    }
                    
                    // Flush workbook to disk periodically to keep memory usage low
                    if (rowIndex % 100 == 0) {
                        workbook.write(fileOut);
                        fileOut.flush();
                    }
                }
                
                // Auto-size columns
                for (int i = 0; i < headers.size(); i++) {
                    sheet.autoSizeColumn(i);
                }
                
                // Write the workbook to the file
                workbook.write(fileOut);
            }
            
            // Return the temporary file reference instead of reading it into memory
            return tempFile;
            
        } catch (Exception e) {
            log.error("Error generating Excel file for job {}: {}", job.getJobUuid(), e.getMessage());
            // Clean up the temp file if there was an error
            if (tempFile != null && tempFile.exists()) {
                tempFile.delete();
            }
            return null;
        }
    }
    
    /**
     * Get the headers for the Excel file
     * 
     * @param job The job
     * @param tasks The job tasks
     * @return List of header names
     */
    private List<String> getHeaders(Job job, PromptDto prompt, List<JobTask> tasks) {
        List<String> headers = new ArrayList<>();
        headers.add("record_number");
        
        // Check if any task has a response text with JSON schema
        for (JobTask task : tasks) {
            if (task.getResponseText() != null && task.getStatus() == TaskStatus.COMPLETED) {
                try {
                    String cleanResponseText = cleanResponseText(task.getResponseText());
                    JsonNode jsonNode = objectMapper.readTree(cleanResponseText);
                    
                    // Add all properties from the first valid JSON response
                    jsonNode.fieldNames().forEachRemaining(headers::add);
                    break;
                } catch (Exception e) {
                    // This task doesn't have a valid JSON response, continue to next
                }
            }
        }
        
        // Add error and full response columns
        if (prompt.getOutputMethod() != PromptOutputMethod.STRUCTURED) {
            String responseTextHeader = prompt.getResponseTextColumnName();
            if (responseTextHeader == null) {
                responseTextHeader = "response_text";
            }
            headers.add(responseTextHeader);
        }

        headers.add("error_message");
        
        return headers;
    }
    
    /**
     * Clean response text by removing markdown code block formatting if present
     * 
     * @param responseText The raw response text
     * @return Cleaned response text
     */
    private String cleanResponseText(String responseText) {
        if (responseText == null) {
            return "";
        }
        
        // Remove markdown code block formatting if present
        String cleaned = responseText.trim();
        if (cleaned.startsWith("```json") || cleaned.startsWith("```")) {
            cleaned = cleaned.replaceAll("^```json\\s*|^```\\s*", "").replaceAll("\\s*```$", "");
        }
        
        return cleaned;
    }
    
    /**
     * Populate a row in the Excel file
     * 
     * @param row The Excel row
     * @param task The job task
     * @param headers The Excel headers
     * @throws JsonProcessingException If there's an error parsing JSON
     */
    private void populateRow(Row row, PromptDto prompt, JobTask task, List<String> headers) throws JsonProcessingException {
        // Fetch the file record to get the record number
        FileRecordDto recordDto = null;
        try {
            // Use null for authToken - the FileClient will automatically use service-to-service authentication
            recordDto = fileClient.getFileRecord(task.getFileRecordUuid(), null);
        } catch (Exception e) {
            log.warn("Could not fetch record data for task {}: {}", task.getJobTaskUuid(), e.getMessage());
        }

        int colIndex = 0;
        
        // First column is record number
        Cell recordNumberCell = row.createCell(colIndex++);
        if (recordDto != null && recordDto.getRecord() != null && recordDto.getRecord().has("record_number")) {
            recordNumberCell.setCellValue(recordDto.getRecord().get("record_number").asText(""));
        } else {
            recordNumberCell.setCellValue(row.getRowNum());
        }
        
        // Initialize a JSON object to null
        JsonNode jsonResponse = null;
        String errorParsingJson = null;
        
        // Try to parse response as JSON if task completed successfully
        if (task.getStatus() == TaskStatus.COMPLETED && task.getResponseText() != null) {
            try {
                String cleanedResponse = cleanResponseText(task.getResponseText());
                jsonResponse = objectMapper.readTree(cleanedResponse);
            } catch (Exception e) {
                errorParsingJson = "error parsing model results";
            }
        }
        
        // Handle JSON schema properties
        for (int i = 1; i < headers.size() - 2; i++) {  // Skip record_number, error_message, response_text
            Cell cell = row.createCell(colIndex++);
            String header = headers.get(i);
            
            // If we have valid JSON and it contains this property, use it
            if (jsonResponse != null && jsonResponse.has(header)) {
                JsonNode value = jsonResponse.get(header);
                if (value.isTextual()) {
                    cell.setCellValue(value.asText());
                } else if (value.isNumber()) {
                    cell.setCellValue(value.asDouble());
                } else if (value.isBoolean()) {
                    cell.setCellValue(value.asBoolean());
                } else {
                    cell.setCellValue(value.toString());
                }
            }
        }
                
        // Add response text column
        if (prompt.getOutputMethod() != PromptOutputMethod.STRUCTURED) {
            Cell responseTextCell = row.createCell(colIndex++);
            if (task.getResponseText() != null) {
                responseTextCell.setCellValue(task.getResponseText());
            }
        }

        // Add error message column
        Cell errorCell = row.createCell(colIndex++);
        if (errorParsingJson != null) {
            // If we failed to parse JSON
            errorCell.setCellValue(errorParsingJson);
        } else if (task.getStatus() == TaskStatus.FAILED && task.getErrorMessage() != null) {
            // If task failed with error
            errorCell.setCellValue(task.getErrorMessage());
        }
    }
    
    /**
     * Update job status to FAILED
     * 
     * @param job The job
     */
    private void failJob(Job job) {
        job.setStatus(JobStatus.FAILED);
        job.setUpdatedAt(LocalDateTime.now());
        jobRepository.save(job);
        log.error("Job {} failed", job.getJobUuid());
    }
}