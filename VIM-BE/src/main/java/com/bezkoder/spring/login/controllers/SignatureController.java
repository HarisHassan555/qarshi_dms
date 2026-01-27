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
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@CrossOrigin(origins = "*")
@RestController
public class SignatureController {

    private Logger logger = LogManager.getLogger(SignatureController.class);

    @Autowired
    private IUserService userService;

    @Autowired
    private ICommonService commonService;

    private static final String SIGNATURE_UPLOAD_DIR = "signatures";

    @PostMapping(value = "/uploadSignature")
    public ResponseEntity<Map<String, Object>> uploadSignature(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "userId", required = false) Integer userId,
            HttpServletRequest request,
            HttpServletResponse response) {
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // Validate file
            if (file.isEmpty()) {
                result.put("status", "Failure");
                result.put("message", "Please select a file to upload");
                return ResponseEntity.badRequest().body(result);
            }

            // Validate file type (only images)
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                result.put("status", "Failure");
                result.put("message", "Only image files are allowed");
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
            CfgTblUser user = null;
            List<CfgTblUser> allUsers = userService.getAllUser();
            for (CfgTblUser u : allUsers) {
                if (u.getSerUserId() != null && u.getSerUserId().equals(userId)) {
                    user = u;
                    break;
                }
            }
            if (user == null) {
                result.put("status", "Failure");
                result.put("message", "User not found");
                return ResponseEntity.badRequest().body(result);
            }

            // Create upload directory if it doesn't exist
            // Use a more robust path handling
            // Use user home directory for guaranteed write access
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

            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String fileExtension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String uniqueFilename = "signature_" + userId + "_" + UUID.randomUUID().toString() + fileExtension;
            File serverFile = new File(uploadDir.getAbsolutePath() + File.separator + uniqueFilename);

            // Save file
            try (InputStream is = file.getInputStream();
                 BufferedOutputStream stream = new BufferedOutputStream(new FileOutputStream(serverFile))) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = is.read(buffer)) != -1) {
                    stream.write(buffer, 0, bytesRead);
                }
                stream.flush();
            }

            // Update user signature path in database
            String signaturePath = SIGNATURE_UPLOAD_DIR + "/" + uniqueFilename;
            user.setTxtSignaturePath(signaturePath);
            String updateResult = userService.updateUser(user);

            if (updateResult != null && updateResult.equals("Success")) {
                result.put("status", "Success");
                result.put("message", "Signature uploaded successfully");
                result.put("signaturePath", signaturePath);
                return ResponseEntity.ok(result);
            } else {
                // Delete file if database update failed
                if (serverFile.exists()) {
                    serverFile.delete();
                }
                result.put("status", "Failure");
                result.put("message", "Failed to update user signature");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
            }

        } catch (NullPointerException npe) {
            logger.error("NullPointerException uploading signature: " + npe.getMessage(), npe);
            result.put("status", "Failure");
            result.put("message", "Error uploading signature: Null pointer exception. Please check if user is authenticated and file is valid.");
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        } catch (IOException ioe) {
            logger.error("IOException uploading signature: " + ioe.getMessage(), ioe);
            result.put("status", "Failure");
            result.put("message", "Error uploading signature: File I/O error - " + ioe.getMessage());
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
            HttpServletRequest request,
            HttpServletResponse response) {
        
        try {
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
            CfgTblUser user = null;
            List<CfgTblUser> allUsers = userService.getAllUser();
            for (CfgTblUser u : allUsers) {
                if (u.getSerUserId() != null && u.getSerUserId().equals(userId)) {
                    user = u;
                    break;
                }
            }
            if (user == null || user.getTxtSignaturePath() == null || user.getTxtSignaturePath().isEmpty()) {
                return ResponseEntity.notFound().build();
            }

            // Read signature file
            String rootPath = System.getProperty("user.home") + File.separator + ".vim_dms_uploads";
            File signatureFile = new File(rootPath + File.separator + user.getTxtSignaturePath());

            if (!signatureFile.exists()) {
                return ResponseEntity.notFound().build();
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
            CfgTblUser user = null;
            List<CfgTblUser> allUsers = userService.getAllUser();
            for (CfgTblUser u : allUsers) {
                if (u.getSerUserId() != null && u.getSerUserId().equals(userId)) {
                    user = u;
                    break;
                }
            }
            if (user == null) {
                result.put("status", "Failure");
                result.put("message", "User not found");
                return ResponseEntity.badRequest().body(result);
            }

            result.put("status", "Success");
            result.put("signaturePath", user.getTxtSignaturePath() != null ? user.getTxtSignaturePath() : "");
            result.put("hasSignature", user.getTxtSignaturePath() != null && !user.getTxtSignaturePath().isEmpty());
            return ResponseEntity.ok(result);

        } catch (Exception ex) {
            logger.error("Error retrieving signature path: " + ex.getMessage(), ex);
            result.put("status", "Failure");
            result.put("message", "Error retrieving signature path: " + ex.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }
}

