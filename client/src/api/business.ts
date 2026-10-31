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

/**
 * 会话消息。
 *
 * @param since 只取比这个时间戳更新的消息（轮询用）；不传则返回最近 limit 条
 */
export const apiListMessages = (sessionId: string, since?: number): Promise<ChatMessage[]> => {
  let url = '/messages?sessionId=' + encodeURIComponent(sessionId);
  if (since !== undefined && since !== null) {
    url += '&since=' + since;
  }
  return request<ChatMessage[]>({ url });
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

/** 联系人「更多」页的档案：账号信息（性别/年龄/住址/职业）+ 通讯录关系 */
export interface ContactProfile {
  contactId: string | null;
  userId: string;
  name: string;
  avatar: string;
  phone: string;
  tag?: string;
  gender?: string;
  age?: number;
  address?: string;
  occupation?: string;
  organization?: string;
  isRealName?: boolean;
  isFriend?: boolean;
}

/** 按对方账号ID取档案（聊天页右上角「更多」传的就是账号ID） */
export const apiContactProfile = (userId: string): Promise<ContactProfile> => {
  return request<ContactProfile>({ url: `/contacts/by-user/${encodeURIComponent(userId)}` });
};

/* ------------------------------------------------------------------ */
/* 广场                                                                */
/* ------------------------------------------------------------------ */

/** 分页结果：nextCursor 为 null 表示已经到底 */
export interface PostPage {
  items: PostItem[];
  nextCursor: number | null;
  hasMore: boolean;
}

export interface FeedQuery {
  /** 上一页返回的 nextCursor；首页不传 */
  cursor?: number | null;
  /** 按作者姓名搜索（服务端 LIKE，前端本地过滤在分页下搜不全） */
  keyword?: string;
  limit?: number;
}

const toQuery = (params: FeedQuery): Record<string, any> => {
  const data: Record<string, any> = {};
  if (params.cursor !== undefined && params.cursor !== null) data.cursor = params.cursor;
  if (params.keyword) data.keyword = params.keyword;
  if (params.limit) data.limit = params.limit;
  return data;
};

/** 广场全部动态（游标分页） */
export const apiPostFeed = (params: FeedQuery = {}): Promise<PostPage> => {
  return request<PostPage>({ url: '/posts/feed', data: toQuery(params) });
};

/** 我发布的动态（游标分页） */
export const apiMyPosts = (params: FeedQuery = {}): Promise<PostPage> => {
  return request<PostPage>({ url: '/posts/mine', data: toQuery(params) });
};

/** 某个用户发布的动态（联系人「更多」页） */
export const apiUserPosts = (userId: string, params: FeedQuery = {}): Promise<PostPage> => {
  return request<PostPage>({
    url: `/posts/user/${encodeURIComponent(userId)}`,
    data: toQuery(params)
  });
};

/** 单条动态详情（评论页按 id 直接打开时用） */
export const apiGetPost = (id: string): Promise<PostItem> => {
  return request<PostItem>({ url: `/posts/${encodeURIComponent(id)}` });
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
