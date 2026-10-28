package com.bezkoder.spring.login.sa.bll.services;

import com.bezkoder.spring.login.sa.bll.dto.UserDelegationAttachmentDownload;
import com.bezkoder.spring.login.sa.bll.dto.UserDelegatableDocumentDto;
import com.bezkoder.spring.login.sa.bll.dto.UserDelegationDto;
import com.bezkoder.spring.login.sa.bll.dto.UserDelegationOnBehalfListItemDto;
import com.bezkoder.spring.login.sa.bll.dto.UserDelegationSaveRequest;
import com.bezkoder.spring.login.sa.dal.entities.TemplateDefinition;

import java.util.List;

public interface IUserDelegationService {

    List<UserDelegationDto> getAll(String status, Integer delegateUserId, Integer onBehalfUserId);

    UserDelegationDto getById(Integer id);

    UserDelegationDto create(UserDelegationSaveRequest request, int actorUserId);

    UserDelegationDto update(UserDelegationSaveRequest request, int actorUserId);

    UserDelegationAttachmentDownload loadAttachmentDownload(Integer id);

    List<UserDelegatableDocumentDto> getDelegatableDocumentsForOnBehalfUser(Integer onBehalfUserId);

    boolean canDelegateAccessTemplateDefinition(Integer delegateUserId, TemplateDefinition definition);

    List<UserDelegationOnBehalfListItemDto> getDelegationOnBehalfForTemplate(
            Integer delegateUserId, Integer formId);

    UserDelegationDto revoke(Integer id, int actorUserId);

    void delete(Integer id, int actorUserId);
}
