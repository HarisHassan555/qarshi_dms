$filePath = "VIM-BE\src\main\java\com\bezkoder\spring\login\sa\dal\daoimpl\CfgTblCustomFormApplicationDAO.java"
$content = Get-Content $filePath -Raw

# Replace the sendBackEmailNotification method to use generateApprovalEmailHtml

$oldMethod = @'
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

$newMethod = @'
    /**
     * Send email notification when an application is sent back to previous department
     * Uses the same format as approval emails with approve/reject buttons
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
            
            // Build URLs
            String baseUrl = getBaseUrl();
            String approveUrl = baseUrl + "/approveApplicationFromEmail?applicationId=" + application.getSerApplicationId();
            String rejectUrl = baseUrl + "/rejectApplicationFromEmail?applicationId=" + application.getSerApplicationId();
            String sendBackUrl = null; // Don't allow sending back again from email
            
            // Send email to each department head using the standard approval email format
            for (Integer headId : headIds) {
                CfgTblUser deptHead = emailEntityManager.find(CfgTblUser.class, headId);
                if (deptHead == null || deptHead.getTxtAddress() == null || deptHead.getTxtAddress().trim().isEmpty()) {
                    log.warn("Department head not found or no email, headId={}", headId);
                    continue;
                }
                
                approveUrl = baseUrl + "/approveApplicationFromEmail?applicationId=" + application.getSerApplicationId() + 
                    "&userId=" + deptHead.getSerUserId();
                rejectUrl = baseUrl + "/rejectApplicationFromEmail?applicationId=" + application.getSerApplicationId() + 
                    "&userId=" + deptHead.getSerUserId();
                
                // Use the same email format as approval emails
                String subject = formName + " Sent Back - Requires Your Approval - " + 
                    (application.getTxtFormCode() != null ? application.getTxtFormCode() : "N/A");
                
                String html = generateApprovalEmailHtml(
                    deptHead.getTxtUserName() != null ? deptHead.getTxtUserName() : "User",
                    previousLevel, // level
                    application.getTxtFormCode(),
                    formName,
                    "IN_PROGRESS", // status - showing it's in progress after send back
                    application.getTxtRemarks(),
                    true, // showActionButtons
                    approveUrl,
                    rejectUrl,
                    sendBackUrl,
                    application.getTxtApprovalHistory(),
                    baseUrl
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
                        
                        // Use the same email format for submitter (without action buttons)
                        String submitterHtml = generateApprovalEmailHtml(
                            submitter.getTxtUserName() != null ? submitter.getTxtUserName() : "User",
                            0, // level
                            application.getTxtFormCode(),
                            formName,
                            "IN_PROGRESS",
                            application.getTxtRemarks(),
                            false, // showActionButtons - submitter can't approve
                            null, null, null,
                            application.getTxtApprovalHistory(),
                            baseUrl
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
Write-Host "Updated sendBackEmailNotification to use generateApprovalEmailHtml"

# Now remove the custom email generation methods since they're no longer needed
$oldGenerateMethods = @'

    /**
     * Generate HTML email content for send-back notification to department head
     */
    private String generateSendBackEmailHtml(String recipientName, String applicationCode, String formName,
            String departmentName, String remarks, String viewUrl) {
        StringBuilder html = new StringBuilder();
        html.append("<html><body style='font-family: Arial, sans-serif;'>");
        html.append("<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>");
        html.append("<div style='background-color: #fff3cd; border-radius: 8px; padding: 20px; margin-bottom: 20px;'>");
        html.append("<h2 style='color: #856404; margin: 0;'>Application Sent Back for Revision</h2>");
        html.append("</div>");
        html.append("<p>Dear <strong>").append(recipientName).append("</strong>,</p>");
        html.append("<p>The following application has been sent back to your department for revision:</p>");
        html.append("<table style='width: 100%; border-collapse: collapse; margin: 20px 0;'>");
        html.append("<tr><td style='padding: 8px; border: 1px solid #ddd;'><strong>Application Code:</strong></td>");
        html.append("<td style='padding: 8px; border: 1px solid #ddd;'>").append(applicationCode != null ? applicationCode : "N/A").append("</td></tr>");
        html.append("<tr><td style='padding: 8px; border: 1px solid #ddd;'><strong>Form Name:</strong></td>");
        html.append("<td style='padding: 8px; border: 1px solid #ddd;'>").append(formName != null ? formName : "N/A").append("</td></tr>");
        html.append("<tr><td style='padding: 8px; border: 1px solid #ddd;'><strong>Department:</strong></td>");
        html.append("<td style='padding: 8px; border: 1px solid #ddd;'>").append(departmentName != null ? departmentName : "N/A").append("</td></tr>");
        if (remarks != null && !remarks.trim().isEmpty()) {
            html.append("<tr><td style='padding: 8px; border: 1px solid #ddd;'><strong>Remarks:</strong></td>");
            html.append("<td style='padding: 8px; border: 1px solid #ddd;'>").append(remarks).append("</td></tr>");
        }
        html.append("</table>");
        html.append("<p>Please review the application and take necessary action.</p>");
        html.append("<a href='").append(viewUrl).append("' style='display: inline-block; background-color: #007bff; color: white; ");
        html.append("padding: 10px 20px; text-decoration: none; border-radius: 5px; margin-top: 10px;'>View Application</a>");
        html.append("<p style='margin-top: 20px; color: #666; font-size: 12px;'>This is an automated email. Please do not reply.</p>");
        html.append("</div></body></html>");
        return html.toString();
    }
    
    /**
     * Generate HTML email content for send-back notification to submitter
     */
    private String generateSendBackSubmitterEmailHtml(String recipientName, String applicationCode, String formName,
            String departmentName, String remarks, String viewUrl) {
        StringBuilder html = new StringBuilder();
        html.append("<html><body style='font-family: Arial, sans-serif;'>");
        html.append("<div style='max-width: 600px; margin: 0 auto; padding: 20px;'>");
        html.append("<div style='background-color: #fff3cd; border-radius: 8px; padding: 20px; margin-bottom: 20px;'>");
        html.append("<h2 style='color: #856404; margin: 0;'>Your Application Requires Revision</h2>");
        html.append("</div>");
        html.append("<p>Dear <strong>").append(recipientName).append("</strong>,</p>");
        html.append("<p>Your submitted application has been sent back for revision:</p>");
        html.append("<table style='width: 100%; border-collapse: collapse; margin: 20px 0;'>");
        html.append("<tr><td style='padding: 8px; border: 1px solid #ddd;'><strong>Application Code:</strong></td>");
        html.append("<td style='padding: 8px; border: 1px solid #ddd;'>").append(applicationCode != null ? applicationCode : "N/A").append("</td></tr>");
        html.append("<tr><td style='padding: 8px; border: 1px solid #ddd;'><strong>Form Name:</strong></td>");
        html.append("<td style='padding: 8px; border: 1px solid #ddd;'>").append(formName != null ? formName : "N/A").append("</td></tr>");
        if (remarks != null && !remarks.trim().isEmpty()) {
            html.append("<tr><td style='padding: 8px; border: 1px solid #ddd;'><strong>Reason for Return:</strong></td>");
            html.append("<td style='padding: 8px; border: 1px solid #ddd;'>").append(remarks).append("</td></tr>");
        }
        html.append("</table>");
        html.append("<p>Please review the remarks and make necessary changes to your application.</p>");
        html.append("<a href='").append(viewUrl).append("' style='display: inline-block; background-color: #007bff; color: white; ");
        html.append("padding: 10px 20px; text-decoration: none; border-radius: 5px; margin-top: 10px;'>View Application</a>");
        html.append("<p style='margin-top: 20px; color: #666; font-size: 12px;'>This is an automated email. Please do not reply.</p>");
        html.append("</div></body></html>");
        return html.toString();
    }

'@

$content = $content -replace [regex]::Escape($oldGenerateMethods), ""
Write-Host "Removed custom email generation methods"

Set-Content -Path $filePath -Value $content -NoNewline
Write-Host "All email format fixes applied"
