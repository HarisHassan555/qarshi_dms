$filePath = "VIM-BE\src\main\java\com\bezkoder\spring\login\sa\dal\daoimpl\CfgTblCustomFormApplicationDAO.java"
$content = Get-Content $filePath -Raw

# Replace the sendBackApplication method to:
# 1. Fix the email notification to use proper method like sendApprovalEmails
# 2. Remove signatures from approval history when sending back

$oldMethod = @'
    /**
     * Send email notification when an application is sent back to previous department
     */
    private void sendBackEmailNotification(CfgTblCustomFormApplication application, Integer previousLevel, 
            List<java.util.Map<String, Object>> pipelines) {
        if (application == null || previousLevel == null || previousLevel < 0) {
            return;
        }
        
        EntityManager emailEntityManager = getEntityManager();
        try {
            // Get the form
            CfgTblCustomForm form = emailEntityManager.find(CfgTblCustomForm.class, application.getSerFormId());
            String formName = form != null ? form.getTxtFormName() : "Application";
            
            // Find the previous department (the one at previousLevel - after send back, app goes to this level)
            // If previousLevel is now 0, there's no previous department to notify (back to submitter)
            if (previousLevel <= 0 || pipelines == null || pipelines.isEmpty()) {
                log.info("No previous department to notify for send-back, appId={}", application.getSerApplicationId());
                return;
            }
            
            int prevLevelIndex = previousLevel - 1;
            if (prevLevelIndex < 0 || prevLevelIndex >= pipelines.size()) {
                log.info("Previous level index out of bounds for send-back, appId={}, level={}", 
                    application.getSerApplicationId(), previousLevel);
                return;
            }
            
            java.util.Map<String, Object> prevPipeline = pipelines.get(prevLevelIndex);
            if (prevPipeline == null) {
                return;
            }
            
            Integer prevDeptId = null;
            Object deptIdObj = prevPipeline.get("serDepartmentId");
            if (deptIdObj != null) {
                prevDeptId = deptIdObj instanceof Integer ? (Integer) deptIdObj : Integer.parseInt(deptIdObj.toString());
            }
            
            if (prevDeptId == null) {
                // Try alternative key names
                Object altDeptIdObj = prevPipeline.get("departmentId");
                if (altDeptIdObj != null) {
                    prevDeptId = altDeptIdObj instanceof Integer ? (Integer) altDeptIdObj : Integer.parseInt(altDeptIdObj.toString());
                }
            }
            
            if (prevDeptId == null) {
                log.warn("Could not find previous department ID for send-back email, appId={}", application.getSerApplicationId());
                return;
            }
            
            String prevDeptName = resolveDepartmentName(emailEntityManager, prevDeptId, prevPipeline);
            
            // Get department head for the previous department
            emailEntityManager.getTransaction().begin();
            java.util.List<Integer> headIds = findDepartmentHeadUserIds(emailEntityManager, prevDeptId);
            Integer fallbackHeadId = findDepartmentHeadUserId(emailEntityManager, prevDeptId);
            if (fallbackHeadId != null && !headIds.contains(fallbackHeadId)) {
                headIds.add(fallbackHeadId);
            }
            
            if (headIds.isEmpty()) {
                emailEntityManager.getTransaction().rollback();
                log.warn("No department head found for previous department {} in send-back email", prevDeptId);
                return;
            }
            
            // Build view URL once
            String baseUrl = getBaseUrl();
            String viewUrl = baseUrl + "/application-details/" + application.getSerApplicationId() + "?from=pending";
            
            // Send email to each department head
            for (Integer headId : headIds) {
                CfgTblUser deptHead = emailEntityManager.find(CfgTblUser.class, headId);
                if (deptHead == null || deptHead.getTxtAddress() == null || deptHead.getTxtAddress().trim().isEmpty()) {
                    continue;
                }
                
                String subject = formName + " Sent Back for Revision - " + 
                    (application.getTxtFormCode() != null ? application.getTxtFormCode() : "N/A");
                
                String html = generateSendBackEmailHtml(
                    deptHead.getTxtUserName() != null ? deptHead.getTxtUserName() : "User",
                    application.getTxtFormCode(),
                    formName,
                    prevDeptName,
                    application.getTxtRemarks(),
                    viewUrl
                );
                
                emailService.sendHtmlEmail(
                    java.util.Arrays.asList(deptHead.getTxtAddress()),
                    subject,
                    html
                );
                
                log.info("Send-back notification email sent to previous department head: " + deptHead.getTxtAddress());
            }
            
            emailEntityManager.getTransaction().commit();
            
            // Also notify the submitter
            if (application.getSerSubmittedBy() != null) {
                try {
                    emailEntityManager.getTransaction().begin();
                    CfgTblUser submitter = emailEntityManager.find(CfgTblUser.class, application.getSerSubmittedBy());
                    if (submitter != null && submitter.getTxtAddress() != null && !submitter.getTxtAddress().trim().isEmpty()) {
                        String submitterSubject = formName + " Requires Revision - " + 
                            (application.getTxtFormCode() != null ? application.getTxtFormCode() : "N/A");
                        String submitterHtml = generateSendBackSubmitterEmailHtml(
                            submitter.getTxtUserName() != null ? submitter.getTxtUserName() : "User",
                            application.getTxtFormCode(),
                            formName,
                            prevDeptName,
                            application.getTxtRemarks(),
                            viewUrl
                        );
                        emailService.sendHtmlEmail(
                            java.util.Arrays.asList(submitter.getTxtAddress()),
                            submitterSubject,
                            submitterHtml
                        );
                        log.info("Send-back notification email sent to submitter: " + submitter.getTxtAddress());
                    }
                    emailEntityManager.getTransaction().commit();
                } catch (Exception e) {
                    if (emailEntityManager.getTransaction().isActive()) {
                        emailEntityManager.getTransaction().rollback();
                    }
                    log.warn("Failed to send send-back email to submitter: " + e.getMessage());
                }
            }
            
        } catch (Exception e) {
            if (emailEntityManager.getTransaction().isActive()) {
                emailEntityManager.getTransaction().rollback();
            }
            log.error("Error sending send-back email notification: " + e.getMessage(), e);
        } finally {
            if (emailEntityManager.isOpen()) {
                emailEntityManager.close();
            }
        }
    }
'@

$newMethod = @'
    /**
     * Send email notification when an application is sent back to previous department
     * Uses the same pattern as sendApprovalEmails to get department head emails
     */
    private void sendBackEmailNotification(CfgTblCustomFormApplication application, Integer previousLevel, 
            List<java.util.Map<String, Object>> pipelines) {
        if (application == null || previousLevel == null || previousLevel <= 0) {
            log.info("Skipping send-back email: appId={}, previousLevel={}", 
                application != null ? application.getSerApplicationId() : "null", previousLevel);
            return;
        }
        
        if (pipelines == null || pipelines.isEmpty()) {
            log.info("No pipelines to notify for send-back, appId={}", application.getSerApplicationId());
            return;
        }
        
        EntityManager emailEntityManager = getEntityManager();
        try {
            // Get the form
            CfgTblCustomForm form = emailEntityManager.find(CfgTblCustomForm.class, application.getSerFormId());
            String formName = form != null ? form.getTxtFormName() : "Application";
            
            // Find the previous department (the one at previousLevel - after send back, app goes to this level)
            int prevLevelIndex = previousLevel - 1;
            if (prevLevelIndex < 0 || prevLevelIndex >= pipelines.size()) {
                log.info("Previous level index out of bounds for send-back, appId={}, level={}, pipelineSize={}", 
                    application.getSerApplicationId(), previousLevel, pipelines.size());
                return;
            }
            
            java.util.Map<String, Object> prevPipeline = pipelines.get(prevLevelIndex);
            if (prevPipeline == null) {
                log.warn("Previous pipeline is null for send-back, appId={}, level={}", 
                    application.getSerApplicationId(), previousLevel);
                return;
            }
            
            // Get department ID - try multiple key names
            Integer prevDeptId = safeInt(prevPipeline.get("serDepartmentId"), 
                safeInt(prevPipeline.get("departmentId"), null));
            
            if (prevDeptId == null) {
                log.warn("Could not find previous department ID for send-back email, appId={}", application.getSerApplicationId());
                return;
            }
            
            log.info("Sending send-back email for appId={}, prevDeptId={}, previousLevel={}", 
                application.getSerApplicationId(), prevDeptId, previousLevel);
            
            String prevDeptName = resolveDepartmentName(emailEntityManager, prevDeptId, prevPipeline);
            
            // Get department head from HrTblDepartment table (like sendApprovalEmails does)
            emailEntityManager.getTransaction().begin();
            com.bezkoder.spring.login.sa.dal.entities.HrTblDepartment prevDept = 
                emailEntityManager.find(com.bezkoder.spring.login.sa.dal.entities.HrTblDepartment.class, prevDeptId);
            
            java.util.List<Integer> headIds = new java.util.ArrayList<>();
            if (prevDept != null && prevDept.getSerDepartmentHeadId() != null) {
                String headIdsStr = prevDept.getSerDepartmentHeadId();
                for (String id : headIdsStr.split(",")) {
                    try {
                        headIds.add(Integer.parseInt(id.trim()));
                    } catch (Exception e) {
                    }
                }
            }
            
            // Fallback if no head IDs found
            if (headIds.isEmpty()) {
                Integer fallbackHeadId = findDepartmentHeadUserId(emailEntityManager, prevDeptId);
                if (fallbackHeadId != null) {
                    headIds.add(fallbackHeadId);
                }
            }
            
            if (headIds.isEmpty()) {
                emailEntityManager.getTransaction().rollback();
                log.warn("No department head found for previous department {} in send-back email", prevDeptId);
                return;
            }
            
            // Build view URL
            String baseUrl = getBaseUrl();
            String viewUrl = baseUrl + "/application-details/" + application.getSerApplicationId() + "?from=pending";
            
            // Send email to each department head
            for (Integer headId : headIds) {
                CfgTblUser deptHead = emailEntityManager.find(CfgTblUser.class, headId);
                if (deptHead == null || deptHead.getTxtAddress() == null || deptHead.getTxtAddress().trim().isEmpty()) {
                    log.warn("Department head not found or no email, headId={}", headId);
                    continue;
                }
                
                String subject = formName + " Sent Back for Revision - " + 
                    (application.getTxtFormCode() != null ? application.getTxtFormCode() : "N/A");
                
                String html = generateSendBackEmailHtml(
                    deptHead.getTxtUserName() != null ? deptHead.getTxtUserName() : "User",
                    application.getTxtFormCode(),
                    formName,
                    prevDeptName,
                    application.getTxtRemarks(),
                    viewUrl
                );
                
                emailService.sendHtmlEmail(
                    java.util.Arrays.asList(deptHead.getTxtAddress()),
                    subject,
                    html
                );
                
                log.info("Send-back notification email sent to previous department head: " + deptHead.getTxtAddress());
            }
            
            emailEntityManager.getTransaction().commit();
            
            // Also notify the submitter
            if (application.getSerSubmittedBy() != null) {
                try {
                    emailEntityManager.getTransaction().begin();
                    CfgTblUser submitter = emailEntityManager.find(CfgTblUser.class, application.getSerSubmittedBy());
                    if (submitter != null && submitter.getTxtAddress() != null && !submitter.getTxtAddress().trim().isEmpty()) {
                        String submitterSubject = formName + " Requires Revision - " + 
                            (application.getTxtFormCode() != null ? application.getTxtFormCode() : "N/A");
                        String submitterHtml = generateSendBackSubmitterEmailHtml(
                            submitter.getTxtUserName() != null ? submitter.getTxtUserName() : "User",
                            application.getTxtFormCode(),
                            formName,
                            prevDeptName,
                            application.getTxtRemarks(),
                            viewUrl
                        );
                        emailService.sendHtmlEmail(
                            java.util.Arrays.asList(submitter.getTxtAddress()),
                            submitterSubject,
                            submitterHtml
                        );
                        log.info("Send-back notification email sent to submitter: " + submitter.getTxtAddress());
                    }
                    emailEntityManager.getTransaction().commit();
                } catch (Exception e) {
                    if (emailEntityManager.getTransaction().isActive()) {
                        emailEntityManager.getTransaction().rollback();
                    }
                    log.warn("Failed to send send-back email to submitter: " + e.getMessage());
                }
            }
            
        } catch (Exception e) {
            log.error("Error sending send-back email notification: " + e.getMessage(), e);
        } finally {
            if (emailEntityManager.isOpen()) {
                emailEntityManager.close();
            }
        }
    }
'@

$content = $content -replace [regex]::Escape($oldMethod), $newMethod
Write-Host "Updated sendBackEmailNotification method"

# Now add the signature removal logic in sendBackApplication method
# This should be added after the approval level is decremented

$oldSendBackLogic = @'
            // Decrement approval level (send back to previous department)
            if (currentLevel > 0) {
                currentLevel--;
            }

            // Update status based on new level
            if (currentLevel <= 0) {
                // Back to initial state
                application.setTxtStatus("PENDING");
                application.setIntCurrentApprovalLevel(0);
            } else {
                // Back to a previous department
                application.setTxtStatus("IN_PROGRESS");
                application.setIntCurrentApprovalLevel(currentLevel);
            }

            application.setSerCurrentApprover(null); // Clear current approver
            application.setTxtRemarks(remarks);
            application.setDteModifiedDate(commonService.getCurrentTimeStamp_new());
            application.setSerModifiedUser(commonService.getCurrentLoggedInUser());

            entityManager.merge(application);
            entityManager.getTransaction().commit();
'@

$newSendBackLogic = @'
            // Get the original current level before decrementing (for signature removal)
            Integer originalLevel = currentLevel;
            
            // Decrement approval level (send back to previous department)
            if (currentLevel > 0) {
                currentLevel--;
            }

            // Remove signatures from approval history for the current level and above
            // When sending back, the current level and all levels above need to re-approve
            if (originalLevel != null && originalLevel > 0) {
                try {
                    // Parse approval history
                    List<java.util.Map<String, Object>> updatedHistory = new java.util.ArrayList<>();
                    if (approvalHistory != null) {
                        for (java.util.Map<String, Object> entry : approvalHistory) {
                            Integer entryLevel = safeInt(entry.get("level"), 
                                safeInt(entry.get("intApprovalOrder"), null));
                            // Keep entries that are below the current level (already approved levels)
                            if (entryLevel == null || entryLevel < originalLevel) {
                                updatedHistory.add(entry);
                            } else {
                                log.info("Removing signature from approval history for level {} when sending back appId={}", 
                                    entryLevel, application.getSerApplicationId());
                            }
                        }
                    }
                    
                    // Update approval history
                    com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                    String updatedHistoryJson = mapper.writeValueAsString(updatedHistory);
                    application.setTxtApprovalHistory(updatedHistoryJson);
                    
                    // Clear the PDF data so it gets regenerated without the removed signatures
                    application.setBlbPdfData(null);
                    application.setTxtPdfName(null);
                    application.setTxtPdfMime(null);
                    
                    log.info("Cleared signatures and PDF for appId={} when sending back from level {} to {}", 
                        application.getSerApplicationId(), originalLevel, currentLevel);
                    
                } catch (Exception e) {
                    log.warn("Error removing signatures from approval history: " + e.getMessage(), e);
                }
            }

            // Update status based on new level
            if (currentLevel <= 0) {
                // Back to initial state
                application.setTxtStatus("PENDING");
                application.setIntCurrentApprovalLevel(0);
            } else {
                // Back to a previous department
                application.setTxtStatus("IN_PROGRESS");
                application.setIntCurrentApprovalLevel(currentLevel);
            }

            application.setSerCurrentApprover(null); // Clear current approver
            application.setTxtRemarks(remarks);
            application.setDteModifiedDate(commonService.getCurrentTimeStamp_new());
            application.setSerModifiedUser(commonService.getCurrentLoggedInUser());

            entityManager.merge(application);
            entityManager.getTransaction().commit();
'@

$content = $content -replace [regex]::Escape($oldSendBackLogic), $newSendBackLogic
Write-Host "Added signature removal logic"

Set-Content -Path $filePath -Value $content -NoNewline
Write-Host "All fixes applied"
