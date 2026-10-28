package com.bezkoder.spring.login.chat;

import com.bezkoder.spring.login.chat.bll.dto.ChatConversationDto;
import com.bezkoder.spring.login.chat.bll.dto.ChatUserDto;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class ChatDtoJsonSerializationTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void chatUserDto_serializesHasProfilePictureKey() throws Exception {
        ChatUserDto dto = new ChatUserDto();
        dto.setUserId(1);
        dto.setHasProfilePicture(true);
        String json = objectMapper.writeValueAsString(dto);
        assertTrue(
                json.contains("\"hasProfilePicture\":true"),
                "Expected hasProfilePicture in JSON but got: " + json);
    }

    @Test
    void chatConversationDto_serializesOtherUserHasProfilePictureKey() throws Exception {
        ChatConversationDto dto = new ChatConversationDto();
        dto.setConversationId(1);
        dto.setOtherUserHasProfilePicture(true);
        String json = objectMapper.writeValueAsString(dto);
        assertTrue(
                json.contains("\"otherUserHasProfilePicture\":true"),
                "Expected otherUserHasProfilePicture in JSON but got: " + json);
    }
}
