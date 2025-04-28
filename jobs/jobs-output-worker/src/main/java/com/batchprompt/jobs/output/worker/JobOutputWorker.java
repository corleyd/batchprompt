package com.batchprompt.jobs.output.worker;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.time.LocalDateTime;
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
    private final ObjectMapper objectMapper;
    
    @RabbitListener(queues = "${rabbitmq.queue.output.name}")
    @Transactional
    public void processJobOutput(JobOutputMessage message) {
        UUID jobUuid = message.getJobUuid();
        boolean hasErrors = message.isHasErrors();
        
        log.info("Processing job output for job {}", jobUuid);
        
        try {
            // Get the job
            Job job = jobRepository.findById(jobUuid).orElse(null);
            if (job == null) {
                log.error("Job not found: {}", jobUuid);
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
            byte[] excelBytes = generateExcelFile(job, tasks);
            if (excelBytes == null) {
                log.error("Failed to generate Excel file for job: {}", jobUuid);
                failJob(job);
                return;
            }
            
            // Upload Excel file to file service
            String fileName = "results_" + job.getJobUuid() + ".xlsx";
            FileDto resultFileDto = uploadExcelFile(excelBytes, fileName, job.getUserId(), "RESULT", null, job.getUserId());
            
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
        }
    }
    
    /**
     * Generate Excel file with job results
     * 
     * @param job The job
     * @param tasks The job tasks
     * @return The Excel file content as a byte array
     */
    private byte[] generateExcelFile(Job job, List<JobTask> tasks) {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Results");
            
            // Create header row
            Row headerRow = sheet.createRow(0);
            List<String> headers = getHeaders(job, tasks);
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
                    populateRow(row, task, headers);
                } catch (Exception e) {
                    log.error("Error processing task {} for job {}: {}", 
                              task.getJobTaskUuid(), job.getJobUuid(), e.getMessage());
                    // Continue with the next task even if this one fails
                }
            }
            
            // Auto-size columns
            for (int i = 0; i < headers.size(); i++) {
                sheet.autoSizeColumn(i);
            }
            
            // Write the Excel file to a byte array
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            return outputStream.toByteArray();
        } catch (Exception e) {
            log.error("Error generating Excel file for job {}: {}", job.getJobUuid(), e.getMessage());
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
    private List<String> getHeaders(Job job, List<JobTask> tasks) {
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
        headers.add("error_message");
        headers.add("response_text");
        
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
    private void populateRow(Row row, JobTask task, List<String> headers) throws JsonProcessingException {
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
        
        // Add error message column
        Cell errorCell = row.createCell(colIndex++);
        if (errorParsingJson != null) {
            // If we failed to parse JSON
            errorCell.setCellValue(errorParsingJson);
        } else if (task.getStatus() == TaskStatus.FAILED && task.getErrorMessage() != null) {
            // If task failed with error
            errorCell.setCellValue(task.getErrorMessage());
        }
        
        // Add response text column
        Cell responseCell = row.createCell(colIndex);
        if (task.getResponseText() != null) {
            responseCell.setCellValue(task.getResponseText());
        }
    }
    
    /**
     * Upload the Excel file to the file service
     * 
     * @param fileContent The file content
     * @param fileName The file name
     * @param userId The user ID
     * @param fileType The file type (upload or result)
     * @return True if upload was successful
     */
    private FileDto uploadExcelFile(byte[] fileContent, String fileName, String userId, String fileType, String authToken, String requestedUserId) {
        try {
            // Convert byte array to input stream
            ByteArrayInputStream inputStream = new ByteArrayInputStream(fileContent);
            
            // Use the authToken if provided, otherwise FileClient will use service-to-service auth
            return fileClient.uploadFile(
                    inputStream,
                    fileName,
                    "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                    fileContent.length,
                    fileType,
                    authToken,
                    requestedUserId
            );
            
        } catch (Exception e) {
            log.error("Error uploading Excel file: {}", e.getMessage());
            return null;
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