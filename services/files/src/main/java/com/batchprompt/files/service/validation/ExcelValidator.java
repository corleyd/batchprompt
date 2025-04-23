package com.batchprompt.files.service.validation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ExcelValidator {

    private final ObjectMapper objectMapper;

    public static class ValidationResult {
        private final boolean valid;
        private final JsonNode errors;
        private final List<Map<String, String>> records;

        public ValidationResult(boolean valid, JsonNode errors, List<Map<String, String>> records) {
            this.valid = valid;
            this.errors = errors;
            this.records = records;
        }

        public boolean isValid() {
            return valid;
        }

        public JsonNode getErrors() {
            return errors;
        }

        public List<Map<String, String>> getRecords() {
            return records;
        }
    }

    public ValidationResult validateExcelFile(InputStream inputStream) {
        List<String> validationErrors = new ArrayList<>();
        List<Map<String, String>> records = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(inputStream)) {
            // Validation rule 1: Must have exactly one worksheet
            if (workbook.getNumberOfSheets() != 1) {
                validationErrors.add("File must contain exactly one worksheet");
                return createValidationResult(validationErrors, records);
            }

            Sheet sheet = workbook.getSheetAt(0);
            
            // Get header row
            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                validationErrors.add("File must contain header row");
                return createValidationResult(validationErrors, records);
            }

            // Extract column names from header row
            List<String> columnNames = new ArrayList<>();
            for (Cell cell : headerRow) {
                String headerValue = getCellValueAsString(cell);
                if (headerValue != null && !headerValue.isEmpty()) {
                    columnNames.add(headerValue);
                }
            }
            
            if (columnNames.isEmpty()) {
                validationErrors.add("Header row must contain at least one column");
                return createValidationResult(validationErrors, records);
            }
            
            int headerColumnCount = columnNames.size();

            // Validate data rows
            int rowCount = 0;
            for (Row row : sheet) {
                // Skip header row
                if (row.getRowNum() == 0) continue;
                rowCount++;
                
                // Check if data row has more columns than header
                int lastCellNum = row.getLastCellNum();
                if (lastCellNum > headerColumnCount) {
                    validationErrors.add("Row " + row.getRowNum() + " contains more columns than the header row");
                    continue;
                }
                
                // Process valid row into a record
                Map<String, String> record = new HashMap<>();
                for (int i = 0; i < headerColumnCount; i++) {
                    Cell cell = row.getCell(i);
                    String value = (cell != null) ? getCellValueAsString(cell) : "";
                    record.put(columnNames.get(i), value);
                }
                
                records.add(record);
            }
            
            if (rowCount == 0) {
                validationErrors.add("File must contain at least one data row");
                return createValidationResult(validationErrors, records);
            }
            
        } catch (IOException e) {
            validationErrors.add("Invalid Excel file format: " + e.getMessage());
        } catch (Exception e) {
            validationErrors.add("Error processing file: " + e.getMessage());
        }
        
        return createValidationResult(validationErrors, records);
    }

    private ValidationResult createValidationResult(List<String> errors, List<Map<String, String>> records) {
        ObjectNode errorsNode = objectMapper.createObjectNode();
        ArrayNode errorsArray = errorsNode.putArray("errors");
        
        for (String error : errors) {
            errorsArray.add(error);
        }
        
        return new ValidationResult(errors.isEmpty(), errorsNode, records);
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) {
            return "";
        }
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                }
                // Convert to string but avoid scientific notation
                return String.valueOf(cell.getNumericCellValue());
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return "";
        }
    }
}