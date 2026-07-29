package com.bezkoder.spring.login.controllers;

import com.bezkoder.spring.login.admin.bll.services.ICommonService;
import com.bezkoder.spring.login.admin.bll.services.IUserService;
import com.bezkoder.spring.login.admin.dal.entities.CfgTblUser;
import org.apache.log4j.LogManager;
import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.*;
import java.nio.file.Files;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@CrossOrigin(origins = "*")
@RestController
public class SignatureController {

    private Logger logger = LogManager.getLogger(SignatureController.class);

    /** 1x1 transparent PNG returned when the signature file is missing on disk. */
    private static final byte[] TRANSPARENT_PNG = Base64.getDecoder().decode(
            "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==");

    @Autowired
    private IUserService userService;

    @Autowired
    private ICommonService commonService;

    private static final String SIGNATURE_UPLOAD_DIR = "signatures";

    @PostMapping(value = "/uploadSignature")
    public ResponseEntity<Map<String, Object>> uploadSignature(
            @RequestBody SignatureUploadRequest request,
            @RequestParam(value = "userId", required = false) Integer userId,
            HttpServletRequest httpRequest,
            HttpServletResponse response) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Get base64 data from request body
            String base64Data = request.getSignature();
            String fileType = request.getFileType();
            String department = request.getDepartment() != null ? request.getDepartment().trim() : "";
            String designation = request.getDesignation() != null ? request.getDesignation().trim() : "";
            boolean hasSignaturePayload = base64Data != null && !base64Data.trim().isEmpty();
            
            logger.debug("Received signature upload request");
            logger.debug("Base64Data is null: " + (base64Data == null));
            logger.debug("Base64Data length: " + (base64Data != null ? base64Data.length() : 0));
            logger.debug("FileType: " + fileType);
            
            // if (base64Data == null || base64Data.trim().isEmpty()) {
            //     result.put("status", "Failure");
            //     result.put("message", "Image data is missing or empty");
            //     return ResponseEntity.badRequest().body(result);
            // }

            if (!hasSignaturePayload && department.isEmpty() && designation.isEmpty()) {
                result.put("status", "Failure");
                result.put("message", "Nothing to update");
                return ResponseEntity.badRequest().body(result);
            }

            // Get current user if userId not provided
            if (userId == null) {
                try {
                    int loggedInUserId = commonService.getCurrentLoggedInUser();
                    if (loggedInUserId <= 0) {
                        result.put("status", "Failure");
                        result.put("message", "User not authenticated. Please provide userId or login first.");
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result);
                    }
                    CfgTblUser currentUser = commonService.getCurrentUser(loggedInUserId);
                    if (currentUser == null) {
                        result.put("status", "Failure");
                        result.put("message", "User not found");
                        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result);
                    }
                    userId = currentUser.getSerUserId();
                } catch (Exception e) {
                    logger.error("Error getting current user: " + e.getMessage(), e);
                    result.put("status", "Failure");
                    result.put("message", "User not authenticated. Please provide userId or login first.");
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result);
                }
            }

            // Get user to update
            CfgTblUser user = userService.getUserById(userId);
            if (user == null) {
                result.put("status", "Failure");
                result.put("message", "User not found");
                return ResponseEntity.badRequest().body(result);
            }

            String signaturePath = user.getTxtSignaturePath();
            File serverFile = null;
            if (hasSignaturePayload) {
                if (fileType == null || fileType.trim().isEmpty()) {
                    fileType = "image/png";
                }

                // Validate MIME type (only images)
                if (!fileType.startsWith("image/")) {
                    result.put("status", "Failure");
                    result.put("message", "Only image files are allowed");
                    return ResponseEntity.badRequest().body(result);
                }

                // Remove data URI prefix if present
                if (base64Data.contains(";base64,")) {
                    base64Data = base64Data.substring(base64Data.indexOf(";base64,") + 8);
                }

                // Decode base64 data
                byte[] decodedBytes;
                try {
                    decodedBytes = Base64.getDecoder().decode(base64Data);
                } catch (IllegalArgumentException e) {
                    logger.error("Invalid base64 data: " + e.getMessage());
                    result.put("status", "Failure");
                    result.put("message", "Invalid base64 data format");
                    return ResponseEntity.badRequest().body(result);
                }

                // Validate file size (max 5MB)
                if (decodedBytes.length > 5242880) { // 5MB in bytes
                    result.put("status", "Failure");
                    result.put("message", "File size exceeds 5MB limit");
                    return ResponseEntity.badRequest().body(result);
                }

                // Create upload directory if it doesn't exist
                String rootPath = System.getProperty("user.home") + File.separator + ".vim_dms_uploads";
                logger.info("Using upload root path: " + rootPath);

                File uploadDir = new File(rootPath + File.separator + SIGNATURE_UPLOAD_DIR);
                if (!uploadDir.exists()) {
                    boolean created = uploadDir.mkdirs();
                    if (!created) {
                        logger.error("Failed to create upload directory: " + uploadDir.getAbsolutePath());
                        result.put("status", "Failure");
                        result.put("message", "Failed to create upload directory");
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
                    }
                }

                // Determine file extension from MIME type
                String fileExtension = ".png"; // Default
                if (fileType.contains("jpeg")) {
                    fileExtension = ".jpg";
                } else if (fileType.contains("png")) {
                    fileExtension = ".png";
                } else if (fileType.contains("gif")) {
                    fileExtension = ".gif";
                } else if (fileType.contains("webp")) {
                    fileExtension = ".webp";
                }

                String uniqueFilename = "signature_" + userId + "_" + UUID.randomUUID().toString() + fileExtension;
                serverFile = new File(uploadDir.getAbsolutePath() + File.separator + uniqueFilename);

                // Save file with proper I/O handling
                try (BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(serverFile), 8192)) {
                    stream.write(decodedBytes);
                    stream.flush();
                    logger.info("File saved successfully: " + uniqueFilename);
                } catch (IOException ioe) {
                    logger.error("Failed to write file to disk: " + ioe.getMessage(), ioe);
                    // Cleanup on failure
                    if (serverFile.exists()) {
                        serverFile.delete();
                    }
                    result.put("status", "Failure");
                    result.put("message", "Failed to save file to disk: " + ioe.getMessage());
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
                }

                // Update user signature path in database
                signaturePath = SIGNATURE_UPLOAD_DIR + "/" + uniqueFilename;
                user.setTxtSignaturePath(signaturePath);
            }
            if (!department.isEmpty()) {
                user.setTxtDepartmentName(department);
            }
            if (!designation.isEmpty()) {
                user.setTxtDesignation(designation);
            }
            
            String updateResult = null;
            try {
                updateResult = userService.updateUser(user);
            } catch (Exception e) {
                logger.error("Database update error: " + e.getMessage(), e);
                // Delete file if database update failed
                if (serverFile != null && serverFile.exists()) {
                    serverFile.delete();
                }
                result.put("status", "Failure");
                result.put("message", "Failed to update user signature in database: " + e.getMessage());
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
            }

            if (updateResult != null && updateResult.equals("Success")) {
                result.put("status", "Success");
                result.put("message", hasSignaturePayload ? "Signature uploaded successfully" : "Details updated successfully");
                result.put("signaturePath", signaturePath);
                result.put("txtDepartmentName", user.getTxtDepartmentName() != null ? user.getTxtDepartmentName() : "");
                result.put("txtDesignation", user.getTxtDesignation() != null ? user.getTxtDesignation() : "");
                logger.info("Signature upload successful for userId: " + userId);
                return ResponseEntity.ok(result);
            } else {
                // Delete file if update was not successful
                if (serverFile != null && serverFile.exists()) {
                    serverFile.delete();
                }
                result.put("status", "Failure");
                result.put("message", "Failed to update user signature. Response: " + updateResult);
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
            }

        } catch (IllegalArgumentException iae) {
            logger.error("Invalid argument uploading signature: " + iae.getMessage(), iae);
            result.put("status", "Failure");
            result.put("message", "Invalid request format: " + iae.getMessage());
            return ResponseEntity.badRequest().body(result);
        } catch (NullPointerException npe) {
            logger.error("NullPointerException uploading signature: " + npe.getMessage(), npe);
            result.put("status", "Failure");
            result.put("message", "Error uploading signature: Null pointer exception. Please check if user is authenticated and data is valid.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        } catch (Exception ex) {
            logger.error("Error uploading signature: " + ex.getMessage(), ex);
            logger.error("Exception class: " + ex.getClass().getName());
            logger.error("Stack trace: ", ex);
            result.put("status", "Failure");
            result.put("message", "Error uploading signature: " + ex.getMessage());
            result.put("errorType", ex.getClass().getSimpleName());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }

    @RequestMapping(value = "/getSignature", method = RequestMethod.GET)
    public ResponseEntity<byte[]> getSignature(
            @RequestParam(value = "userId", required = false) Integer userId,
            @RequestParam(value = "signaturePath", required = false) String signaturePath,
            HttpServletRequest request,
            HttpServletResponse response) {
        
        try {
            String rootPath = System.getProperty("user.home") + File.separator + ".vim_dms_uploads";

            if (signaturePath != null && !signaturePath.trim().isEmpty()) {
                File signatureFile = new File(rootPath + File.separator + signaturePath.trim());
                if (signatureFile.exists()) {
                    byte[] fileBytes = Files.readAllBytes(signatureFile.toPath());
                    String contentType = Files.probeContentType(signatureFile.toPath());
                    if (contentType == null) {
                        contentType = "image/png";
                    }
                    return ResponseEntity.ok()
                            .contentType(MediaType.parseMediaType(contentType))
                            .body(fileBytes);
                }
            }

            // Get current user if userId not provided
            if (userId == null) {
                int loggedInUserId = commonService.getCurrentLoggedInUser();
                if (loggedInUserId <= 0) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
                }
                CfgTblUser currentUser = commonService.getCurrentUser(loggedInUserId);
                if (currentUser == null) {
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
                }
                userId = currentUser.getSerUserId();
            }

            // Get user
            CfgTblUser user = userService.getUserById(userId);
            if (user == null || user.getTxtSignaturePath() == null || user.getTxtSignaturePath().isEmpty()) {
                return ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_PNG)
                        .body(TRANSPARENT_PNG);
            }

            // Read signature file
            File signatureFile = new File(rootPath + File.separator + user.getTxtSignaturePath());

            if (!signatureFile.exists()) {
                logger.warn("Signature file missing for userId=" + userId + " path=" + signatureFile.getAbsolutePath());
                return ResponseEntity.ok()
                        .contentType(MediaType.IMAGE_PNG)
                        .body(TRANSPARENT_PNG);
            }

            // Read file bytes
            byte[] fileBytes = Files.readAllBytes(signatureFile.toPath());
            
            // Determine content type
            String contentType = Files.probeContentType(signatureFile.toPath());
            if (contentType == null) {
                contentType = "image/png"; // Default to PNG
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .body(fileBytes);

        } catch (Exception ex) {
            logger.error("Error retrieving signature: " + ex.getMessage(), ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @RequestMapping(value = "/getUserSignaturePath", method = RequestMethod.GET)
    public ResponseEntity<Map<String, Object>> getUserSignaturePath(
            @RequestParam(value = "userId", required = false) Integer userId,
            HttpServletRequest request,
            HttpServletResponse response) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Get current user if userId not provided
            if (userId == null) {
                int loggedInUserId = commonService.getCurrentLoggedInUser();
                if (loggedInUserId <= 0) {
                    result.put("status", "Failure");
                    result.put("message", "User not authenticated. Please provide userId or login first.");
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result);
                }
                CfgTblUser currentUser = commonService.getCurrentUser(loggedInUserId);
                if (currentUser == null) {
                    result.put("status", "Failure");
                    result.put("message", "User not found");
                    return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result);
                }
                userId = currentUser.getSerUserId();
            }

            // Get user
            CfgTblUser user = userService.getUserById(userId);
            if (user == null) {
                result.put("status", "Failure");
                result.put("message", "User not found");
                return ResponseEntity.badRequest().body(result);
            }

            result.put("status", "Success");
            result.put("signaturePath", user.getTxtSignaturePath() != null ? user.getTxtSignaturePath() : "");
            result.put("hasSignature", user.getTxtSignaturePath() != null && !user.getTxtSignaturePath().isEmpty());
            result.put("txtDepartmentName", user.getTxtDepartmentName() != null ? user.getTxtDepartmentName() : "");
            result.put("txtDesignation", user.getTxtDesignation() != null ? user.getTxtDesignation() : "");
            return ResponseEntity.ok(result);

        } catch (Exception ex) {
            logger.error("Error retrieving signature path: " + ex.getMessage(), ex);
            result.put("status", "Failure");
            result.put("message", "Error retrieving signature path: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }
}
