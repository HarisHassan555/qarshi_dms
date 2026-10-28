import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { urls } from 'src/app/utils/urls';

export interface ChatUser {
  userId: number;
  displayName: string;
  loginName?: string;
  organizationName?: string;
  divisionName?: string;
  departmentName?: string;
  hasProfilePicture?: boolean;
}

export interface ChatConversation {
  conversationId: number;
  otherUserId: number;
  otherUserName: string;
  otherUserHasProfilePicture?: boolean;
  lastMessageBody?: string;
  lastMessageAt?: number;
  unreadCount: number;
}

export type ChatDeliveryStatus = 'pending' | 'sent' | 'delivered' | 'read';

export interface ChatMessage {
  messageId: number;
  conversationId: number;
  senderUserId: number;
  senderName?: string;
  body: string;
  sentAt?: number;
  deliveryStatus?: ChatDeliveryStatus;
}

@Injectable({ providedIn: 'root' })
export class ChatService {
  constructor(private http: HttpClient) {}

  getUsers() {
    return this.http.get<ChatUser[]>(urls.API_URL + 'chat/users');
  }

  getConversations() {
    return this.http.get<ChatConversation[]>(urls.API_URL + 'chat/conversations');
  }

  openDirect(targetUserId: number) {
    return this.http.post<ChatConversation>(urls.API_URL + 'chat/conversations/direct', { targetUserId });
  }

  getMessages(conversationId: number, limit = 100) {
    return this.http.get<ChatMessage[]>(
      urls.API_URL + `chat/conversations/${conversationId}/messages?limit=${limit}`
    );
  }

  sendMessage(conversationId: number, body: string) {
    return this.http.post<{ status: string; message?: ChatMessage }>(
      urls.API_URL + `chat/conversations/${conversationId}/messages`,
      { body }
    );
  }

  markRead(conversationId: number, lastReadMessageId: number) {
    return this.http.post(urls.API_URL + `chat/conversations/${conversationId}/read`, {
      lastReadMessageId,
    });
  }
}
