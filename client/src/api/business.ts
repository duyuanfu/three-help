import { request } from './request';
import type { HelpOrder, ChatMessage, ContactItem, PostItem, CommentItem, OrderCategory, MessageType } from '../types';

/* ------------------------------------------------------------------ */
/* 订单                                                               */
/* ------------------------------------------------------------------ */

export interface CreateOrderPayload {
  category: OrderCategory;
  title: string;
  content: string;
  appointTime: string;
  address: string;
  bounty: number;
}

export const apiCreateOrder = (payload: CreateOrderPayload): Promise<HelpOrder> => {
  return request<HelpOrder>({ url: '/orders', method: 'POST', data: payload });
};

export const apiListOrders = (scope: 'mine' | 'pending' = 'mine'): Promise<HelpOrder[]> => {
  return request<HelpOrder[]>({ url: '/orders?scope=' + scope });
};

export const apiTakeOrder = (orderId: string): Promise<HelpOrder> => {
  return request<HelpOrder>({ url: `/orders/${orderId}/take`, method: 'POST' });
};

export const apiCompleteOrder = (orderId: string): Promise<HelpOrder> => {
  return request<HelpOrder>({ url: `/orders/${orderId}/complete`, method: 'POST' });
};

export const apiCancelOrder = (orderId: string): Promise<HelpOrder> => {
  return request<HelpOrder>({ url: `/orders/${orderId}/cancel`, method: 'POST' });
};

/* ------------------------------------------------------------------ */
/* 会话与消息                                                          */
/* ------------------------------------------------------------------ */

export interface SessionView {
  sessionId: string;
  lastMessage: string;
  lastTime: string;
  unreadCount: number;
  targetUser: { id: string; name: string; avatar: string; tag?: string; phone?: string } | null;
}

export const apiListSessions = (): Promise<SessionView[]> => {
  return request<SessionView[]>({ url: '/sessions' });
};

export const apiEnsureSession = (targetUserId: string): Promise<SessionView> => {
  return request<SessionView>({ url: '/sessions', method: 'POST', data: { targetUserId } });
};

export const apiMarkSessionRead = (sessionId: string): Promise<void> => {
  return request<void>({ url: `/sessions/${encodeURIComponent(sessionId)}/read`, method: 'POST', silent: true });
};

export const apiListMessages = (sessionId: string): Promise<ChatMessage[]> => {
  return request<ChatMessage[]>({ url: '/messages?sessionId=' + encodeURIComponent(sessionId) });
};

export interface SendMessagePayload {
  sessionId: string;
  type: MessageType;
  content: string;
  duration?: number;
  audioPath?: string;
}

export const apiSendMessage = (payload: SendMessagePayload): Promise<ChatMessage> => {
  return request<ChatMessage>({ url: '/messages', method: 'POST', data: payload });
};

/* ------------------------------------------------------------------ */
/* 联系人                                                              */
/* ------------------------------------------------------------------ */

export interface AddContactPayload {
  name?: string;
  phone?: string;
  tag?: string;
  avatar?: string;
  /** 已知对方账号ID时直接关联（从广场动态加好友） */
  targetUserId?: string;
}

export const apiListContacts = (): Promise<ContactItem[]> => {
  return request<ContactItem[]>({ url: '/contacts' });
};

export const apiAddContact = (payload: AddContactPayload): Promise<ContactItem> => {
  return request<ContactItem>({ url: '/contacts', method: 'POST', data: payload });
};

export const apiDeleteContact = (id: string): Promise<void> => {
  return request<void>({ url: `/contacts/${encodeURIComponent(id)}`, method: 'DELETE' });
};

/* ------------------------------------------------------------------ */
/* 广场                                                                */
/* ------------------------------------------------------------------ */

export const apiListPosts = (): Promise<PostItem[]> => {
  return request<PostItem[]>({ url: '/posts' });
};

export const apiCreatePost = (content: string, images: string[] = []): Promise<PostItem> => {
  return request<PostItem>({ url: '/posts', method: 'POST', data: { content, images } });
};

export const apiDeletePost = (id: string): Promise<void> => {
  return request<void>({ url: `/posts/${encodeURIComponent(id)}`, method: 'DELETE' });
};

export const apiToggleLike = (id: string): Promise<PostItem> => {
  return request<PostItem>({ url: `/posts/${encodeURIComponent(id)}/like`, method: 'POST' });
};

export const apiListComments = (postId: string): Promise<CommentItem[]> => {
  return request<CommentItem[]>({ url: `/posts/${encodeURIComponent(postId)}/comments` });
};

export const apiAddComment = (postId: string, content: string): Promise<CommentItem> => {
  return request<CommentItem>({ url: `/posts/${encodeURIComponent(postId)}/comments`, method: 'POST', data: { content } });
};
